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
    private android.widget.TextView textHeaderContact;

    // Doctor section
    private android.widget.TextView textDoctorSectionTitle;
    private android.widget.TextView textDoctorProfileEmpty;
    private android.widget.TextView textDoctorName;
    private android.widget.TextView textDoctorSpecialty;
    private android.widget.TextView textDoctorLocation;
    private android.widget.TextView textDoctorFee;
    private android.widget.TextView textDoctorBio;
    private android.widget.TextView textDoctorAcceptsNew;
    private android.widget.TextView textDoctorTeleconsult;

    // Patient section
    private android.widget.TextView textPatientSectionTitle;
    private android.widget.TextView textPatientProfileEmpty;
    private android.widget.TextView textPatientMeta;
    private android.widget.TextView textPatientLocation;
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
        textHeaderContact = view.findViewById(R.id.text_profile_header_contact);

        textDoctorSectionTitle = view.findViewById(R.id.text_profile_doctor_section_title);
        textDoctorProfileEmpty = view.findViewById(R.id.text_doctor_profile_empty);
        textDoctorName = view.findViewById(R.id.text_profile_name);
        textDoctorSpecialty = view.findViewById(R.id.text_profile_specialty);
        textDoctorLocation = view.findViewById(R.id.text_profile_location);
        textDoctorFee = view.findViewById(R.id.text_profile_fee);
        textDoctorBio = view.findViewById(R.id.text_profile_bio);
        textDoctorAcceptsNew = view.findViewById(R.id.text_profile_accepts_new);
        textDoctorTeleconsult = view.findViewById(R.id.text_profile_teleconsultation);

        textPatientSectionTitle = view.findViewById(R.id.text_patient_section_title);
        textPatientProfileEmpty = view.findViewById(R.id.text_patient_profile_empty);
        textPatientMeta = view.findViewById(R.id.text_patient_meta);
        textPatientLocation = view.findViewById(R.id.text_patient_location);
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

        if (imageProfileAvatar != null) {
            imageProfileAvatar.setOnClickListener(v -> {
                if (imagePickerLauncher != null) {
                    imagePickerLauncher.launch("image/*");
                }
            });
        }

        View cameraOverlay = view.findViewById(R.id.image_profile_avatar_camera);
        if (cameraOverlay != null) {
            cameraOverlay.setOnClickListener(v -> {
                if (imagePickerLauncher != null) {
                    imagePickerLauncher.launch("image/*");
                }
            });
        }

        if (textHeaderContact != null) {
            textHeaderContact.setOnClickListener(v -> onHeaderContactClicked());
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
        String email = null;
        String phone = null;

        if (user != null) {
            email = user.getEmail();
            phone = user.getPhone();

            String first = user.getFirstname() != null ? user.getFirstname() : "";
            String last = user.getLastname() != null ? user.getLastname() : "";
            String combined = (first + " " + last).trim();
            if (!combined.isEmpty()) {
                displayName = combined;
            } else if (!TextUtils.isEmpty(email)) {
                displayName = email;
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

        // Contact line
        String contact = null;
        if (!TextUtils.isEmpty(email) && !TextUtils.isEmpty(phone)) {
            contact = email + " • " + phone;
        } else if (!TextUtils.isEmpty(email)) {
            contact = email;
        } else if (!TextUtils.isEmpty(phone)) {
            contact = phone;
        }

        if (textHeaderContact != null) {
            if (!TextUtils.isEmpty(contact)) {
                textHeaderContact.setText(contact);
                textHeaderContact.setVisibility(View.VISIBLE);
            } else {
                textHeaderContact.setText("");
                textHeaderContact.setVisibility(View.GONE);
            }
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

        // If doctor profile is not created yet: show empty message, hide details
        if (doctorProfile == null) {
            if (textDoctorProfileEmpty != null) {
                textDoctorProfileEmpty.setVisibility(View.VISIBLE);
            }
            if (textDoctorSpecialty != null) textDoctorSpecialty.setVisibility(View.GONE);
            if (textDoctorLocation != null) textDoctorLocation.setVisibility(View.GONE);
            if (textDoctorFee != null) textDoctorFee.setVisibility(View.GONE);
            if (textDoctorBio != null) textDoctorBio.setVisibility(View.GONE);
            if (textDoctorAcceptsNew != null) textDoctorAcceptsNew.setVisibility(View.GONE);
            if (textDoctorTeleconsult != null) textDoctorTeleconsult.setVisibility(View.GONE);
            return;
        } else {
            if (textDoctorProfileEmpty != null) {
                textDoctorProfileEmpty.setVisibility(View.GONE);
            }
        }

        // Specialty
        if (textDoctorSpecialty != null) {
            try {
                DoctorProfile.Specialty specialty = doctorProfile.getSpecialty();
                String specName = (specialty != null && !TextUtils.isEmpty(specialty.getName()))
                        ? specialty.getName()
                        : "";
                if (!TextUtils.isEmpty(specName)) {
                    textDoctorSpecialty.setText(specName);
                    textDoctorSpecialty.setVisibility(View.VISIBLE);
                } else {
                    textDoctorSpecialty.setText("");
                    textDoctorSpecialty.setVisibility(View.GONE);
                }
            } catch (Exception ignored) {
                textDoctorSpecialty.setText("");
                textDoctorSpecialty.setVisibility(View.GONE);
            }
        }

        // Location
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
            String locationStr = location.toString().trim();
            if (!TextUtils.isEmpty(locationStr)) {
                textDoctorLocation.setText(locationStr);
                textDoctorLocation.setVisibility(View.VISIBLE);
            } else {
                textDoctorLocation.setText("");
                textDoctorLocation.setVisibility(View.GONE);
            }
        }

        // Fee
        if (textDoctorFee != null) {
            try {
                BigDecimal fee = doctorProfile.getConsultationFee();
                if (fee != null) {
                    String feeText = getString(R.string.profile_doctor_fee_format, fee.toPlainString());
                    textDoctorFee.setText(feeText);
                    textDoctorFee.setVisibility(View.VISIBLE);
                } else {
                    textDoctorFee.setText("");
                    textDoctorFee.setVisibility(View.GONE);
                }
            } catch (Exception ignored) {
                textDoctorFee.setText("");
                textDoctorFee.setVisibility(View.GONE);
            }
        }

        // Bio
        if (textDoctorBio != null) {
            try {
                String bio = doctorProfile.getBio();
                if (!TextUtils.isEmpty(bio)) {
                    textDoctorBio.setText(bio);
                    textDoctorBio.setVisibility(View.VISIBLE);
                } else {
                    textDoctorBio.setText("");
                    textDoctorBio.setVisibility(View.GONE);
                }
            } catch (Exception ignored) {
                textDoctorBio.setText("");
                textDoctorBio.setVisibility(View.GONE);
            }
        }

        // Accepts new patients (flag pill)
        if (textDoctorAcceptsNew != null) {
            try {
                Boolean accepts = doctorProfile.getAcceptsNewPatients();
                if (accepts != null) {
                    textDoctorAcceptsNew.setVisibility(View.VISIBLE);
                    if (accepts) {
                        textDoctorAcceptsNew.setText(getString(R.string.profile_flag_accepts_new));
                        textDoctorAcceptsNew.setBackgroundResource(R.drawable.bg_profile_flag_positive);
                    } else {
                        textDoctorAcceptsNew.setText(getString(R.string.profile_flag_not_accepts_new));
                        textDoctorAcceptsNew.setBackgroundResource(R.drawable.bg_profile_flag_negative);
                    }
                } else {
                    textDoctorAcceptsNew.setText("");
                    textDoctorAcceptsNew.setVisibility(View.GONE);
                }
            } catch (Exception ignored) {
                textDoctorAcceptsNew.setText("");
                textDoctorAcceptsNew.setVisibility(View.GONE);
            }
        }

        // Teleconsultation (flag pill)
        if (textDoctorTeleconsult != null) {
            try {
                Boolean tele = doctorProfile.getTeleconsultationEnabled();
                if (tele != null) {
                    textDoctorTeleconsult.setVisibility(View.VISIBLE);
                    if (tele) {
                        textDoctorTeleconsult.setText(getString(R.string.profile_flag_teleconsultation));
                        textDoctorTeleconsult.setBackgroundResource(R.drawable.bg_profile_flag_positive);
                    } else {
                        textDoctorTeleconsult.setText(getString(R.string.profile_flag_no_teleconsultation));
                        textDoctorTeleconsult.setBackgroundResource(R.drawable.bg_profile_flag_negative);
                    }
                } else {
                    textDoctorTeleconsult.setText("");
                    textDoctorTeleconsult.setVisibility(View.GONE);
                }
            } catch (Exception ignored) {
                textDoctorTeleconsult.setText("");
                textDoctorTeleconsult.setVisibility(View.GONE);
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

        // If patient profile is not created yet: show empty message, hide details
        if (patientProfile == null) {
            if (textPatientProfileEmpty != null) {
                textPatientProfileEmpty.setVisibility(View.VISIBLE);
            }
            if (textPatientMeta != null) textPatientMeta.setVisibility(View.GONE);
            if (textPatientLocation != null) textPatientLocation.setVisibility(View.GONE);
            if (textPatientBloodType != null) textPatientBloodType.setVisibility(View.GONE);
            if (textPatientHeightWeight != null) textPatientHeightWeight.setVisibility(View.GONE);
            if (textPatientFlags != null) textPatientFlags.setVisibility(View.GONE);
            if (textPatientNotes != null) textPatientNotes.setVisibility(View.GONE);
            return;
        } else {
            if (textPatientProfileEmpty != null) {
                textPatientProfileEmpty.setVisibility(View.GONE);
            }
        }

        // META: DOB + gender
        if (textPatientMeta != null) {
            String meta = "";
            try {
                String gender = patientProfile.getGender();
                String dob = patientProfile.getDateOfBirth();

                gender = gender != null ? gender.trim() : "";
                dob = dob != null ? dob.trim() : "";

                if (!TextUtils.isEmpty(dob) && !TextUtils.isEmpty(gender)) {
                    meta = dob + " \u00b7 " + gender; // "yyyy-MM-dd · FEMALE"
                } else if (!TextUtils.isEmpty(dob)) {
                    meta = dob;
                } else if (!TextUtils.isEmpty(gender)) {
                    meta = gender;
                }
            } catch (Exception ignored) {
            }

            if (!TextUtils.isEmpty(meta)) {
                textPatientMeta.setText(meta);
                textPatientMeta.setVisibility(View.VISIBLE);
            } else {
                textPatientMeta.setText("");
                textPatientMeta.setVisibility(View.GONE);
            }
        }

        // LOCATION: city, country
        if (textPatientLocation != null) {
            String location = "";
            try {
                String label = patientProfile.getCityCountryLabel();
                if (!TextUtils.isEmpty(label)) {
                    location = label;
                } else {
                    String city = patientProfile.getCity();
                    String country = patientProfile.getCountry();
                    StringBuilder sb = new StringBuilder();
                    if (!TextUtils.isEmpty(city)) {
                        sb.append(city);
                    }
                    if (!TextUtils.isEmpty(country)) {
                        if (sb.length() > 0) sb.append(", ");
                        sb.append(country);
                    }
                    location = sb.toString();
                }
            } catch (Exception ignored) {
            }

            if (!TextUtils.isEmpty(location)) {
                textPatientLocation.setText(location);
                textPatientLocation.setVisibility(View.VISIBLE);
            } else {
                textPatientLocation.setText("");
                textPatientLocation.setVisibility(View.GONE);
            }
        }

        // Blood type
        if (textPatientBloodType != null) {
            try {
                String blood = patientProfile.getBloodType();
                if (!TextUtils.isEmpty(blood)) {
                    textPatientBloodType.setText(
                            getString(R.string.profile_patient_blood_type_format, blood)
                    );
                    textPatientBloodType.setVisibility(View.VISIBLE);
                } else {
                    textPatientBloodType.setText(
                            getString(R.string.profile_patient_blood_type_not_set)
                    );
                    textPatientBloodType.setVisibility(View.VISIBLE);
                }
            } catch (Exception ignored) {
                textPatientBloodType.setText(
                        getString(R.string.profile_patient_blood_type_not_set)
                );
                textPatientBloodType.setVisibility(View.VISIBLE);
            }
        }

        // Height / weight
        if (textPatientHeightWeight != null) {
            try {
                Integer h = patientProfile.getHeightCm();
                Integer w = patientProfile.getWeightKg();
                if (h != null && w != null) {
                    String hw = getString(R.string.profile_patient_height_weight_format, h, w);
                    textPatientHeightWeight.setText(hw);
                    textPatientHeightWeight.setVisibility(View.VISIBLE);
                } else {
                    textPatientHeightWeight.setText("");
                    textPatientHeightWeight.setVisibility(View.GONE);
                }
            } catch (Exception ignored) {
                textPatientHeightWeight.setText("");
                textPatientHeightWeight.setVisibility(View.GONE);
            }
        }

        // Lifestyle flags text
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
                textPatientFlags.setVisibility(View.VISIBLE);
            } catch (Exception ignored) {
                textPatientFlags.setText("");
                textPatientFlags.setVisibility(View.GONE);
            }
        }

        // Notes
        if (textPatientNotes != null) {
            try {
                String notes = patientProfile.getNotes();
                if (!TextUtils.isEmpty(notes)) {
                    textPatientNotes.setText(notes);
                    textPatientNotes.setVisibility(View.VISIBLE);
                } else {
                    textPatientNotes.setText("");
                    textPatientNotes.setVisibility(View.GONE);
                }
            } catch (Exception ignored) {
                textPatientNotes.setText("");
                textPatientNotes.setVisibility(View.GONE);
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

    private void onHeaderContactClicked() {
        NavHostFragment.findNavController(this)
                .navigate(R.id.action_profileFragment_to_userBaseInfoFragment);
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
