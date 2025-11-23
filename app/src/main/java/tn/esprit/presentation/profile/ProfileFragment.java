package tn.esprit.presentation.profile;

import android.app.Activity;
import android.content.ContentResolver;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

import tn.esprit.MainActivity;
import tn.esprit.R;
import tn.esprit.data.profile.ProfileRepository;
import tn.esprit.domain.doctor.DoctorProfile;
import tn.esprit.domain.patient.PatientProfile;
import tn.esprit.domain.user.User;
import tn.esprit.presentation.home.HomeUiHelper;

public class ProfileFragment extends Fragment {

    private ProgressBar progressBar;

    private ImageView imageProfileAvatar;
    private MaterialCardView sectionDoctor;
    private MaterialCardView sectionPatient;

    // Header inside profile card
    private android.widget.TextView textHeaderName;
    private android.widget.TextView textHeaderRole;

    // Doctor section
    private android.widget.TextView textDoctorName;
    private android.widget.TextView textDoctorSpecialty;
    private android.widget.TextView textDoctorLocation;
    private android.widget.TextView textDoctorFee;
    private android.widget.TextView textDoctorBio;
    private android.widget.TextView textDoctorAcceptsNew;
    private android.widget.TextView textDoctorTeleconsult;

    // Patient section
    private android.widget.TextView textPatientBloodType;
    private android.widget.TextView textPatientHeightWeight;
    private android.widget.TextView textPatientFlags;
    private android.widget.TextView textPatientNotes;

    private MaterialButton buttonChangeAvatar;
    private MaterialButton buttonEditProfile;

    private ProfileRepository profileRepository;
    private User currentUser;
    private DoctorProfile currentDoctorProfile;
    private PatientProfile currentPatientProfile;

    // Image picker launcher
    private ActivityResultLauncher<String> imagePickerLauncher;

    public ProfileFragment() {
        // Required empty constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        profileRepository = new ProfileRepository(requireContext());

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        uploadProfileImage(uri);
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        progressBar = view.findViewById(R.id.profile_progress);
        imageProfileAvatar = view.findViewById(R.id.image_profile_avatar);
        sectionDoctor = view.findViewById(R.id.section_doctor);
        sectionPatient = view.findViewById(R.id.section_patient);

        textHeaderName = view.findViewById(R.id.text_profile_header_name);
        textHeaderRole = view.findViewById(R.id.text_profile_header_role);

        textDoctorName = view.findViewById(R.id.text_profile_name);
        textDoctorSpecialty = view.findViewById(R.id.text_profile_specialty);
        textDoctorLocation = view.findViewById(R.id.text_profile_location);
        textDoctorFee = view.findViewById(R.id.text_profile_fee);
        textDoctorBio = view.findViewById(R.id.text_profile_bio);
        textDoctorAcceptsNew = view.findViewById(R.id.text_profile_accepts_new);
        textDoctorTeleconsult = view.findViewById(R.id.text_profile_teleconsultation);

        textPatientBloodType = view.findViewById(R.id.text_patient_blood_type);
        textPatientHeightWeight = view.findViewById(R.id.text_patient_height_weight);
        textPatientFlags = view.findViewById(R.id.text_patient_flags);
        textPatientNotes = view.findViewById(R.id.text_patient_notes);

        buttonChangeAvatar = view.findViewById(R.id.button_change_avatar);
        buttonEditProfile = view.findViewById(R.id.button_edit_profile);

        if (buttonEditProfile != null) {
            buttonEditProfile.setOnClickListener(v -> onEditProfileClicked());
        }

        if (buttonChangeAvatar != null) {
            buttonChangeAvatar.setOnClickListener(v -> {
                if (imagePickerLauncher != null) {
                    imagePickerLauncher.launch("image/*");
                }
            });
        }

        loadProfile();
    }

    private void loadProfile() {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        if (sectionDoctor != null) sectionDoctor.setVisibility(View.GONE);
        if (sectionPatient != null) sectionPatient.setVisibility(View.GONE);

        profileRepository.loadProfile(new ProfileRepository.ProfileCallback() {
            @Override
            public void onSuccess(User user,
                                  DoctorProfile doctorProfile,
                                  PatientProfile patientProfile) {
                if (!isAdded()) return;

                currentUser = user;
                currentDoctorProfile = doctorProfile;
                currentPatientProfile = patientProfile;

                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }

                bindUser(user);

                String role = user != null ? user.getRole() : null;
                if (role != null && "DOCTOR".equalsIgnoreCase(role)) {
                    bindDoctorSection(doctorProfile);
                } else if (role != null && "PATIENT".equalsIgnoreCase(role)) {
                    bindPatientSection(patientProfile);
                } else {
                    if (sectionDoctor != null) sectionDoctor.setVisibility(View.GONE);
                    if (sectionPatient != null) sectionPatient.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(Throwable throwable, Integer httpCode, String errorBody) {
                if (!isAdded()) return;
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                }
                Toast.makeText(requireContext(),
                        R.string.profile_error_loading,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindUser(@Nullable User user) {
        String displayName = null;
        String role = null;
        String imageUrl = null;

        if (user != null) {
            String first = user.getFirstname() != null ? user.getFirstname() : "";
            String last = user.getLastname() != null ? user.getLastname() : "";
            String combined = (first + " " + last).trim();
            if (!combined.isEmpty()) {
                displayName = combined;
            } else if (!TextUtils.isEmpty(user.getEmail())) {
                displayName = user.getEmail();
            }

            role = user.getRole();

            try {
                String candidate = user.getProfileImage();
                if (!TextUtils.isEmpty(candidate)) {
                    imageUrl = candidate;
                }
            } catch (Exception ignored) {
            }
        }

        if (TextUtils.isEmpty(displayName)) {
            // Placeholder string is now empty in strings.xml, so this is safe.
            displayName = getString(R.string.home_drawer_user_name_placeholder);
        }

        if (textHeaderName != null) {
            textHeaderName.setText(displayName);
        }

        // Role label
        int roleLabelResId = HomeUiHelper.resolveRoleLabelResId(role);
        String roleLabel = getString(roleLabelResId);
        if (textHeaderRole != null) {
            textHeaderRole.setText(roleLabel);
        }

        // Avatar
        if (imageProfileAvatar != null) {
            if (!TextUtils.isEmpty(imageUrl)) {
                Glide.with(this)
                        .load(imageUrl)
                        .placeholder(R.drawable.logo)
                        .error(R.drawable.logo)
                        .circleCrop()
                        .into(imageProfileAvatar);
            } else {
                imageProfileAvatar.setImageResource(R.drawable.logo);
            }
        }
    }

    private void bindDoctorSection(@Nullable DoctorProfile doctorProfile) {
        if (sectionDoctor != null) {
            sectionDoctor.setVisibility(View.VISIBLE);
        }
        if (sectionPatient != null) {
            sectionPatient.setVisibility(View.GONE);
        }

        String displayName = textHeaderName != null ? textHeaderName.getText().toString() : "";

        if (textDoctorName != null) {
            if (!TextUtils.isEmpty(displayName)) {
                textDoctorName.setText(getString(R.string.profile_doctor_name_format, displayName));
            } else {
                textDoctorName.setText(getString(R.string.profile_title));
            }
        }

        if (doctorProfile == null) {
            return;
        }

        if (textDoctorSpecialty != null) {
            try {
                DoctorProfile.Specialty specialty = doctorProfile.getSpecialty();
                if (specialty != null && !TextUtils.isEmpty(specialty.getName())) {
                    textDoctorSpecialty.setText(specialty.getName());
                } else {
                    textDoctorSpecialty.setText("");
                }
            } catch (Exception ignored) {
                textDoctorSpecialty.setText("");
            }
        }

        if (textDoctorLocation != null) {
            StringBuilder location = new StringBuilder();
            try {
                String clinic = doctorProfile.getClinicAddress();
                String city = doctorProfile.getCity();
                String country = doctorProfile.getCountry();
                if (!TextUtils.isEmpty(clinic)) {
                    location.append(clinic);
                }
                if (!TextUtils.isEmpty(city)) {
                    if (location.length() > 0) location.append(", ");
                    location.append(city);
                }
                if (!TextUtils.isEmpty(country)) {
                    if (location.length() > 0) location.append(", ");
                    location.append(country);
                }
            } catch (Exception ignored) {
            }
            textDoctorLocation.setText(location.toString());
        }

        if (textDoctorFee != null) {
            try {
                BigDecimal fee = doctorProfile.getConsultationFee();
                if (fee != null) {
                    String feeText = getString(R.string.profile_doctor_fee_format, fee.toPlainString());
                    textDoctorFee.setText(feeText);
                } else {
                    textDoctorFee.setText("");
                }
            } catch (Exception ignored) {
                textDoctorFee.setText("");
            }
        }

        if (textDoctorBio != null) {
            try {
                String bio = doctorProfile.getBio();
                textDoctorBio.setText(bio != null ? bio : "");
            } catch (Exception ignored) {
                textDoctorBio.setText("");
            }
        }

        if (textDoctorAcceptsNew != null) {
            try {
                Boolean accepts = doctorProfile.getAcceptsNewPatients();
                if (accepts != null && accepts) {
                    textDoctorAcceptsNew.setText(getString(R.string.profile_flag_accepts_new));
                } else {
                    textDoctorAcceptsNew.setText(getString(R.string.profile_flag_not_accepts_new));
                }
            } catch (Exception ignored) {
                textDoctorAcceptsNew.setText("");
            }
        }

        if (textDoctorTeleconsult != null) {
            try {
                Boolean tele = doctorProfile.getTeleconsultationEnabled();
                if (tele != null && tele) {
                    textDoctorTeleconsult.setText(getString(R.string.profile_flag_teleconsultation));
                } else {
                    textDoctorTeleconsult.setText(getString(R.string.profile_flag_no_teleconsultation));
                }
            } catch (Exception ignored) {
                textDoctorTeleconsult.setText("");
            }
        }
    }

    private void bindPatientSection(@Nullable PatientProfile patientProfile) {
        if (sectionPatient != null) {
            sectionPatient.setVisibility(View.VISIBLE);
        }
        if (sectionDoctor != null) {
            sectionDoctor.setVisibility(View.GONE);
        }

        if (patientProfile == null) {
            return;
        }

        if (textPatientBloodType != null) {
            try {
                String blood = patientProfile.getBloodType();
                if (!TextUtils.isEmpty(blood)) {
                    textPatientBloodType.setText(
                            getString(R.string.profile_patient_blood_type_format, blood)
                    );
                } else {
                    textPatientBloodType.setText("");
                }
            } catch (Exception ignored) {
                textPatientBloodType.setText("");
            }
        }

        if (textPatientHeightWeight != null) {
            try {
                Integer h = patientProfile.getHeightCm();
                Integer w = patientProfile.getWeightKg();
                if (h != null && w != null) {
                    String hw = getString(R.string.profile_patient_height_weight_format, h, w);
                    textPatientHeightWeight.setText(hw);
                } else {
                    textPatientHeightWeight.setText("");
                }
            } catch (Exception ignored) {
                textPatientHeightWeight.setText("");
            }
        }

        if (textPatientFlags != null) {
            try {
                Boolean smoker = patientProfile.getSmoker();
                Boolean alcohol = patientProfile.getAlcoholUse();
                StringBuilder flags = new StringBuilder();

                if (smoker != null) {
                    flags.append(smoker
                            ? getString(R.string.profile_patient_smoker_yes)
                            : getString(R.string.profile_patient_smoker_no));
                }
                if (alcohol != null) {
                    if (flags.length() > 0) flags.append(" • ");
                    flags.append(alcohol
                            ? getString(R.string.profile_patient_alcohol_yes)
                            : getString(R.string.profile_patient_alcohol_no));
                }

                if (flags.length() == 0) {
                    textPatientFlags.setText(getString(R.string.profile_flags_none));
                } else {
                    textPatientFlags.setText(flags.toString());
                }
            } catch (Exception ignored) {
                textPatientFlags.setText("");
            }
        }

        if (textPatientNotes != null) {
            try {
                String notes = patientProfile.getNotes();
                textPatientNotes.setText(notes != null ? notes : "");
            } catch (Exception ignored) {
                textPatientNotes.setText("");
            }
        }
    }

    private void onEditProfileClicked() {
        if (currentUser == null) {
            return;
        }
        String role = currentUser.getRole();
        if (role != null && "DOCTOR".equalsIgnoreCase(role)) {
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_profileFragment_to_profileEditFragment);
        } else if (role != null && "PATIENT".equalsIgnoreCase(role)) {
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_profileFragment_to_editPatientProfileFragment);
        } else {
            Toast.makeText(requireContext(),
                    R.string.profile_error_unknown_role,
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Activity activity = getActivity();
        if (activity instanceof MainActivity) {
            ((MainActivity) activity).refreshUserProfileUi();
        }
    }

    // ------------------------------------------------------------
    // Image upload helpers
    // ------------------------------------------------------------

    private void uploadProfileImage(@NonNull Uri uri) {
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        MultipartBody.Part imagePart = createImagePartFromUri(uri);
        if (imagePart == null) {
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
            Toast.makeText(requireContext(),
                    R.string.profile_error_saving,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        profileRepository.uploadProfileImage(imagePart,
                new ProfileRepository.ProfileImageUpdateCallback() {
                    @Override
                    public void onSuccess(User updatedUser) {
                        if (!isAdded()) return;
                        if (progressBar != null) {
                            progressBar.setVisibility(View.GONE);
                        }

                        // Reload profile for fresh data (including avatar URL)
                        loadProfile();

                        Activity activity = getActivity();
                        if (activity instanceof MainActivity) {
                            ((MainActivity) activity).refreshUserProfileUi();
                        }
                    }

                    @Override
                    public void onError(Throwable throwable,
                                        Integer httpCode,
                                        String errorBody) {
                        if (!isAdded()) return;
                        if (progressBar != null) {
                            progressBar.setVisibility(View.GONE);
                        }
                        Toast.makeText(requireContext(),
                                R.string.profile_error_saving,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Nullable
    private MultipartBody.Part createImagePartFromUri(@NonNull Uri uri) {
        try {
            ContentResolver resolver = requireContext().getContentResolver();
            String mimeType = resolver.getType(uri);
            if (mimeType == null) {
                mimeType = "image/*";
            }

            InputStream inputStream = resolver.openInputStream(uri);
            if (inputStream == null) {
                return null;
            }

            byte[] bytes = readAllBytes(inputStream);
            RequestBody requestBody = RequestBody.create(
                    MediaType.parse(mimeType),
                    bytes
            );

            // Backend expects field name "image"
            return MultipartBody.Part.createFormData(
                    "image",
                    "profile.jpg",
                    requestBody
            );
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private byte[] readAllBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[4096];
        int nRead;
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        return buffer.toByteArray();
    }
}
