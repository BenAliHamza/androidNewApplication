package tn.esprit.presentation.profile;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import java.math.BigDecimal;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import tn.esprit.R;
import tn.esprit.data.auth.AuthLocalDataSource;
import tn.esprit.data.remote.ApiClient;
import tn.esprit.data.remote.user.UserImageApiService;
import tn.esprit.domain.auth.AuthTokens;
import tn.esprit.domain.doctor.DoctorProfile;
import tn.esprit.domain.patient.PatientProfile;
import tn.esprit.domain.user.User;

/**
 * Role-aware profile screen.
 *
 * - If the current user is a DOCTOR:
 *      * Shows the doctor profile card (specialty, clinic, fees, availability).
 *      * Uses DoctorProfile data from backend (/me + /api/doctors/me).
 *      * "Edit profile" opens the doctor edit screen (ProfileEditFragment via Navigation
 *        when available, or ProfileActivity as fallback).
 * - If the current user is a PATIENT:
 *      * Shows a patient profile summary card (blood type, height/weight, lifestyle, notes).
 *      * Uses PatientProfile data from backend (/me + /api/patients/me).
 *      * "Edit profile" opens EditPatientProfileFragment via Navigation.
 *
 * For BOTH roles:
 *  - Top section allows viewing (and later changing) the profile image.
 *    It uses the base User (/me) data, without any hard-coding.
 */
public class ProfileFragment extends Fragment {

    private ProfileViewModel viewModel;

    private View contentContainer;
    private ProgressBar progressBar;

    // Avatar section (both roles)
    private ImageView imageAvatar;
    private View buttonChangeAvatar;

    // Sections
    private View sectionDoctor;
    private View sectionPatient;

    // Doctor views
    private TextView textName;
    private TextView textSpecialty;
    private TextView textLocation;
    private TextView textFee;
    private TextView textBio;
    private TextView textAcceptsNew;
    private TextView textTeleconsultation;

    // Patient views
    private TextView textPatientBloodType;
    private TextView textPatientHeightWeight;
    private TextView textPatientFlags;
    private TextView textPatientNotes;

    private Button buttonEdit;

    // Cached role from /me
    @Nullable
    private String currentRole;

    // For avatar upload
    private UserImageApiService userImageApiService;
    private AuthLocalDataSource authLocalDataSource;

    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_profile, container, false);

        contentContainer = root.findViewById(R.id.profile_content_container);
        progressBar = root.findViewById(R.id.profile_progress);

        // Avatar section
        imageAvatar = root.findViewById(R.id.image_profile_avatar);
        buttonChangeAvatar = root.findViewById(R.id.button_change_avatar);

        sectionDoctor = root.findViewById(R.id.section_doctor);
        sectionPatient = root.findViewById(R.id.section_patient);

        // Doctor section views
        textName = root.findViewById(R.id.text_profile_name);
        textSpecialty = root.findViewById(R.id.text_profile_specialty);
        textLocation = root.findViewById(R.id.text_profile_location);
        textFee = root.findViewById(R.id.text_profile_fee);
        textBio = root.findViewById(R.id.text_profile_bio);
        textAcceptsNew = root.findViewById(R.id.text_profile_accepts_new);
        textTeleconsultation = root.findViewById(R.id.text_profile_teleconsultation);

        // Patient section views
        textPatientBloodType = root.findViewById(R.id.text_patient_blood_type);
        textPatientHeightWeight = root.findViewById(R.id.text_patient_height_weight);
        textPatientFlags = root.findViewById(R.id.text_patient_flags);
        textPatientNotes = root.findViewById(R.id.text_patient_notes);

        buttonEdit = root.findViewById(R.id.button_edit_profile);

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);

        userImageApiService = ApiClient.createService(UserImageApiService.class);
        authLocalDataSource = new AuthLocalDataSource(requireContext().getApplicationContext());

        setupImagePicker();

        // Loading state
        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            boolean show = loading != null && loading;
            if (progressBar != null) {
                progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            }
            if (contentContainer != null) {
                contentContainer.setVisibility(show ? View.INVISIBLE : View.VISIBLE);
            }
        });

        // Base user (/me) => determines role + avatar
        viewModel.getUser().observe(getViewLifecycleOwner(), this::onUserLoaded);

        // Doctor profile (/api/doctors/me if role == DOCTOR)
        viewModel.getDoctorProfile().observe(getViewLifecycleOwner(), this::bindDoctorProfile);

        // Patient profile (/api/patients/me if role == PATIENT)
        viewModel.getPatientProfile().observe(getViewLifecycleOwner(), this::bindPatientProfile);

        // Optional: error messages
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), message -> {
            // You can show a Snackbar/Toast here if you want.
        });

        buttonEdit.setOnClickListener(v -> handleEditClick());

        if (buttonChangeAvatar != null) {
            buttonChangeAvatar.setOnClickListener(v -> openImagePicker());
        }

        // Trigger backend load (real data)
        viewModel.loadProfile();
    }

    // -------------------------------------------------------------------------
    // Role handling & base user
    // -------------------------------------------------------------------------

    private void onUserLoaded(@Nullable User user) {
        currentRole = user != null ? user.getRole() : null;
        applyRoleUi();
        bindAvatar(user);
    }

    private void applyRoleUi() {
        String roleUpper = currentRole != null ? currentRole.toUpperCase() : "";

        if ("DOCTOR".equals(roleUpper)) {
            // Doctor sees the doctor card; patient card hidden
            if (sectionDoctor != null) sectionDoctor.setVisibility(View.VISIBLE);
            if (sectionPatient != null) sectionPatient.setVisibility(View.GONE);

            if (buttonEdit != null) {
                buttonEdit.setVisibility(View.VISIBLE);
                buttonEdit.setText(getString(R.string.profile_edit_title)); // "Edit profile"
            }
        } else if ("PATIENT".equals(roleUpper)) {
            // Patient sees the patient summary card; doctor card hidden
            if (sectionDoctor != null) sectionDoctor.setVisibility(View.GONE);
            if (sectionPatient != null) sectionPatient.setVisibility(View.VISIBLE);

            if (buttonEdit != null) {
                buttonEdit.setVisibility(View.VISIBLE);
                buttonEdit.setText(getString(R.string.profile_patient_edit_title)); // "Edit medical profile"
            }
        } else {
            // Unknown role: hide both sections and the edit button
            if (sectionDoctor != null) sectionDoctor.setVisibility(View.GONE);
            if (sectionPatient != null) sectionPatient.setVisibility(View.GONE);
            if (buttonEdit != null) {
                buttonEdit.setVisibility(View.GONE);
            }
        }
    }

    private void handleEditClick() {
        String roleUpper = currentRole != null ? currentRole.toUpperCase() : "";

        if ("DOCTOR".equals(roleUpper)) {
            // Prefer Navigation when available (MainActivity + nav_main),
            // but fallback to ProfileActivity so we do not break existing behavior.
            try {
                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_profileFragment_to_profileEditFragment);
            } catch (Exception e) {
                if (getActivity() instanceof ProfileActivity) {
                    ((ProfileActivity) getActivity()).openEditProfile();
                } else {
                    Intent intent = new Intent(requireContext(), ProfileActivity.class);
                    startActivity(intent);
                }
            }
        } else if ("PATIENT".equals(roleUpper)) {
            // PATIENT: navigate to EditPatientProfileFragment via Navigation component.
            try {
                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_profileFragment_to_editPatientProfileFragment);
            } catch (IllegalArgumentException ignored) {
                // If nav graph does not declare editPatientProfileFragment yet,
                // we silently ignore; at least we don't crash.
            }
        }
    }

    // -------------------------------------------------------------------------
    // Avatar: display + upload
    // -------------------------------------------------------------------------

    private void bindAvatar(@Nullable User user) {
        if (imageAvatar == null) return;

        // Fallback avatar drawable (what you already use now)
        int fallbackRes = R.drawable.ic_launcher_foreground; // or your logo/avatar

        if (user == null || user.getProfileImage() == null || user.getProfileImage().isEmpty()) {
            imageAvatar.setImageResource(fallbackRes);
            return;
        }

        // If you have Glide/Picasso in the project, you can use it here.
        // For now, we keep it simple: just try to load via Uri.
        try {
            Uri uri = Uri.parse(user.getProfileImage());
            imageAvatar.setImageURI(uri);
        } catch (Exception e) {
            imageAvatar.setImageResource(fallbackRes);
        }
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != android.app.Activity.RESULT_OK ||
                            result.getData() == null ||
                            result.getData().getData() == null) {
                        return;
                    }

                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        uploadAvatar(imageUri);
                    }
                }
        );
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void uploadAvatar(@NonNull Uri imageUri) {
        AuthTokens tokens = authLocalDataSource.getTokens();
        if (tokens == null || tokens.getAccessToken() == null || tokens.getAccessToken().isEmpty()) {
            Toast.makeText(requireContext(),
                    getString(R.string.profile_error_not_authenticated),
                    Toast.LENGTH_LONG).show();
            return;
        }

        String type = tokens.getTokenType() != null ? tokens.getTokenType() : "Bearer";
        String authHeader = type + " " + tokens.getAccessToken();

        try {
            // Read the image bytes from the content resolver
            byte[] bytes = requireContext().getContentResolver()
                    .openInputStream(imageUri)
                    .readAllBytes();

            RequestBody requestBody = RequestBody.create(
                    bytes,
                    MediaType.parse("image/*")
            );

            MultipartBody.Part part = MultipartBody.Part.createFormData(
                    "image",
                    "avatar.jpg",
                    requestBody
            );

            userImageApiService.uploadMyProfileImage(authHeader, part)
                    .enqueue(new Callback<User>() {
                        @Override
                        public void onResponse(Call<User> call, Response<User> response) {
                            if (!isAdded()) return;

                            if (!response.isSuccessful() || response.body() == null) {
                                Toast.makeText(requireContext(),
                                        getString(R.string.profile_error_generic),
                                        Toast.LENGTH_LONG).show();
                                return;
                            }

                            User updated = response.body();
                            // Update ViewModel so header + profile react
                            viewModel.setUser(updated);
                            bindAvatar(updated);

                            Toast.makeText(requireContext(),
                                    "Profile image updated",
                                    Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailure(Call<User> call, Throwable t) {
                            if (!isAdded()) return;
                            Toast.makeText(requireContext(),
                                    getString(R.string.profile_error_network),
                                    Toast.LENGTH_LONG).show();
                        }
                    });

        } catch (Exception e) {
            Toast.makeText(requireContext(),
                    getString(R.string.profile_error_generic),
                    Toast.LENGTH_LONG).show();
        }
    }

    // -------------------------------------------------------------------------
    // Doctor binding
    // -------------------------------------------------------------------------

    private void bindDoctorProfile(@Nullable DoctorProfile profile) {
        // If we don't have a doctor profile (e.g. PATIENT account),
        // clear and exit. The whole doctor section is hidden by applyRoleUi().
        if (profile == null) {
            if (textName != null) textName.setText("");
            if (textSpecialty != null) textSpecialty.setText("");
            if (textLocation != null) textLocation.setText("");
            if (textFee != null) textFee.setText("—");
            if (textBio != null) textBio.setText("");
            if (textAcceptsNew != null) textAcceptsNew.setText("");
            if (textTeleconsultation != null) textTeleconsultation.setText("");
            return;
        }

        // Name
        String displayName;
        if (profile.getFirstname() != null && profile.getLastname() != null) {
            displayName = profile.getFirstname() + " " + profile.getLastname();
        } else if (profile.getFirstname() != null) {
            displayName = profile.getFirstname();
        } else {
            displayName = getString(R.string.profile_role_doctor);
        }
        if (textName != null) {
            textName.setText(displayName);
        }

        // Specialty
        if (textSpecialty != null) {
            if (profile.getSpecialty() != null && profile.getSpecialty().getName() != null) {
                textSpecialty.setText(profile.getSpecialty().getName());
            } else {
                textSpecialty.setText("");
            }
        }

        // Location (clinic, city, country)
        if (textLocation != null) {
            StringBuilder locationBuilder = new StringBuilder();
            if (profile.getClinicAddress() != null) {
                locationBuilder.append(profile.getClinicAddress());
            }
            if (profile.getCity() != null) {
                if (locationBuilder.length() > 0) locationBuilder.append(", ");
                locationBuilder.append(profile.getCity());
            }
            if (profile.getCountry() != null) {
                if (locationBuilder.length() > 0) locationBuilder.append(", ");
                locationBuilder.append(profile.getCountry());
            }
            textLocation.setText(locationBuilder.length() == 0
                    ? ""
                    : locationBuilder.toString());
        }

        // Fee
        if (textFee != null) {
            BigDecimal fee = profile.getConsultationFee();
            if (fee != null) {
                textFee.setText(fee.toPlainString() + " TND");
            } else {
                textFee.setText("—");
            }
        }

        // Bio
        if (textBio != null) {
            if (profile.getBio() != null && !profile.getBio().isEmpty()) {
                textBio.setText(profile.getBio());
            } else {
                textBio.setText("");
            }
        }

        // Accepting new patients
        if (textAcceptsNew != null) {
            Boolean acceptsNew = profile.getAcceptsNewPatients();
            if (acceptsNew != null && acceptsNew) {
                textAcceptsNew.setText(getString(R.string.profile_flag_accepts_new));
            } else {
                textAcceptsNew.setText("");
            }
        }

        // Teleconsultation
        if (textTeleconsultation != null) {
            Boolean tele = profile.getTeleconsultationEnabled();
            if (tele != null && tele) {
                textTeleconsultation.setText(getString(R.string.profile_flag_teleconsultation));
            } else {
                textTeleconsultation.setText("");
            }
        }
    }

    // -------------------------------------------------------------------------
    // Patient binding
    // -------------------------------------------------------------------------

    private void bindPatientProfile(@Nullable PatientProfile profile) {
        if (textPatientBloodType == null
                || textPatientHeightWeight == null
                || textPatientFlags == null
                || textPatientNotes == null) {
            return;
        }

        if (profile == null) {
            textPatientBloodType.setText("");
            textPatientHeightWeight.setText("");
            textPatientFlags.setText(getString(R.string.profile_flags_none));
            textPatientNotes.setText("");
            return;
        }

        // Blood type
        String blood = profile.getBloodType();
        if (blood != null && !blood.isEmpty()) {
            String label = getString(R.string.profile_patient_blood_type_prefix, blood);
            textPatientBloodType.setText(label);
        } else {
            textPatientBloodType.setText("");
        }

        // Height / weight
        Integer height = profile.getHeightCm();
        Integer weight = profile.getWeightKg();
        if (height != null && weight != null) {
            textPatientHeightWeight.setText(
                    getString(R.string.profile_patient_height_weight, height, weight)
            );
        } else if (height != null) {
            textPatientHeightWeight.setText(
                    getString(R.string.profile_patient_height_only_format, height)
            );
        } else if (weight != null) {
            textPatientHeightWeight.setText(
                    getString(R.string.profile_patient_weight_only_format, weight)
            );
        } else {
            textPatientHeightWeight.setText("");
        }

        // Lifestyle flags (smoker / alcohol)
        StringBuilder flags = new StringBuilder();
        Boolean smoker = profile.getSmoker();
        Boolean alcohol = profile.getAlcoholUse();

        if (smoker != null && smoker) {
            flags.append(getString(R.string.profile_flag_smoker));
        }
        if (alcohol != null && alcohol) {
            if (flags.length() > 0) flags.append(" • ");
            flags.append(getString(R.string.profile_flag_alcohol));
        }
        if (flags.length() == 0) {
            flags.append(getString(R.string.profile_flags_none));
        }
        textPatientFlags.setText(flags.toString());

        // Notes
        if (profile.getNotes() != null && !profile.getNotes().isEmpty()) {
            textPatientNotes.setText(profile.getNotes());
        } else {
            textPatientNotes.setText("");
        }
    }
}
