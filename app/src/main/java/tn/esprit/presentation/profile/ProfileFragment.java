package tn.esprit.presentation.profile;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;

import tn.esprit.R;
import tn.esprit.data.profile.ProfileRepository;
import tn.esprit.domain.doctor.DoctorProfile;
import tn.esprit.domain.patient.PatientProfile;
import tn.esprit.domain.user.User;

public class ProfileFragment extends Fragment {

    private View rootView;
    private View loadingOverlay;
    private View contentContainer;

    // Header
    private TextView textName;
    private TextView textRoleChip;
    private TextView textEmail;
    private TextView textPhone;
    private TextView textStatus;

    // Doctor section
    private View groupDoctorSection;
    private TextView textDoctorTitle;
    private TextView textDoctorClinic;
    private TextView textDoctorLocation;
    private TextView textDoctorExperience;
    private TextView textDoctorReg;
    private TextView textDoctorFee;
    private TextView textDoctorFlags;

    // Patient section
    private View groupPatientSection;
    private TextView textPatientTitle;
    private TextView textPatientDob;
    private TextView textPatientGender;
    private TextView textPatientBlood;
    private TextView textPatientHeightWeight;
    private TextView textPatientAddress;
    private TextView textPatientLifestyle;

    private ProfileRepository profileRepository;

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

        rootView = view.findViewById(R.id.profile_root);
        loadingOverlay = view.findViewById(R.id.profile_loading_overlay);
        contentContainer = view.findViewById(R.id.profile_content_container);

        // Header
        textName = view.findViewById(R.id.text_profile_name);
        textRoleChip = view.findViewById(R.id.text_profile_role_chip);
        textEmail = view.findViewById(R.id.text_profile_email);
        textPhone = view.findViewById(R.id.text_profile_phone);
        textStatus = view.findViewById(R.id.text_profile_status_chip);

        // Doctor section
        groupDoctorSection = view.findViewById(R.id.group_doctor_section);
        textDoctorTitle = view.findViewById(R.id.text_doctor_section_title);
        textDoctorClinic = view.findViewById(R.id.text_doctor_clinic);
        textDoctorLocation = view.findViewById(R.id.text_doctor_location);
        textDoctorExperience = view.findViewById(R.id.text_doctor_experience);
        textDoctorReg = view.findViewById(R.id.text_doctor_reg);
        textDoctorFee = view.findViewById(R.id.text_doctor_fee);
        textDoctorFlags = view.findViewById(R.id.text_doctor_flags);

        // Patient section
        groupPatientSection = view.findViewById(R.id.group_patient_section);
        textPatientTitle = view.findViewById(R.id.text_patient_section_title);
        textPatientDob = view.findViewById(R.id.text_patient_dob);
        textPatientGender = view.findViewById(R.id.text_patient_gender);
        textPatientBlood = view.findViewById(R.id.text_patient_blood);
        textPatientHeightWeight = view.findViewById(R.id.text_patient_height_weight);
        textPatientAddress = view.findViewById(R.id.text_patient_address);
        textPatientLifestyle = view.findViewById(R.id.text_patient_lifestyle);

        profileRepository = new ProfileRepository(requireContext());

        applyWindowInsets();
        loadProfile();
    }

    private void applyWindowInsets() {
        if (rootView == null) return;

        final int paddingLeft = rootView.getPaddingLeft();
        final int paddingTop = rootView.getPaddingTop();
        final int paddingRight = rootView.getPaddingRight();
        final int paddingBottom = rootView.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(
                    paddingLeft + systemBars.left,
                    paddingTop + systemBars.top,
                    paddingRight + systemBars.right,
                    paddingBottom + systemBars.bottom
            );
            return insets;
        });
    }

    private void loadProfile() {
        showLoading(true);

        profileRepository.loadProfile(new ProfileRepository.ProfileCallback() {
            @Override
            public void onSuccess(User user,
                                  DoctorProfile doctorProfile,
                                  PatientProfile patientProfile) {
                if (!isAdded()) return;
                showLoading(false);

                bindHeader(user);

                if (doctorProfile != null) {
                    bindDoctorProfile(doctorProfile);
                } else if (patientProfile != null) {
                    bindPatientProfile(patientProfile);
                } else {
                    groupDoctorSection.setVisibility(View.GONE);
                    groupPatientSection.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(Throwable throwable,
                                Integer httpCode,
                                String errorBody) {
                if (!isAdded()) return;
                showLoading(false);

                String message;
                if (throwable != null) {
                    message = getString(R.string.profile_error_network);
                } else if (httpCode != null && httpCode == 401) {
                    message = getString(R.string.profile_error_unauthorized);
                } else {
                    message = getString(R.string.profile_error_generic);
                }

                Snackbar.make(rootView, message, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void showLoading(boolean show) {
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (contentContainer != null) {
            contentContainer.setAlpha(show ? 0.4f : 1f);
        }
    }

    private void bindHeader(User user) {
        // Instead of user.getFullName(), build it from firstname + lastname
        String fullName = joinNonEmpty(" ",
                user.getFirstname(),
                user.getLastname()
        );
        textName.setText(fullName);

        String role = user.getRole();
        if ("DOCTOR".equalsIgnoreCase(role)) {
            textRoleChip.setText(getString(R.string.profile_role_doctor));
        } else if ("PATIENT".equalsIgnoreCase(role)) {
            textRoleChip.setText(getString(R.string.profile_role_patient));
        } else {
            textRoleChip.setText(getString(R.string.profile_role_unknown));
        }

        textEmail.setText(
                !TextUtils.isEmpty(user.getEmail()) ? user.getEmail() : "-"
        );

        if (!TextUtils.isEmpty(user.getPhone())) {
            textPhone.setText(user.getPhone());
        } else {
            textPhone.setText(getString(R.string.profile_phone_placeholder));
        }

        String status = user.getStatus();
        if (!TextUtils.isEmpty(status)) {
            textStatus.setText(status);
            textStatus.setVisibility(View.VISIBLE);
        } else {
            textStatus.setVisibility(View.GONE);
        }
    }

    private void bindDoctorProfile(DoctorProfile profile) {
        groupDoctorSection.setVisibility(View.VISIBLE);
        groupPatientSection.setVisibility(View.GONE);

        textDoctorTitle.setText(R.string.profile_section_doctor_title);

        textDoctorClinic.setText(
                valueOrDash(profile.getClinicAddress())
        );

        String location = joinNonEmpty(", ",
                profile.getCity(),
                profile.getCountry()
        );
        textDoctorLocation.setText(valueOrDash(location));

        if (profile.getYearsOfExperience() != null) {
            String exp = getString(
                    R.string.profile_doctor_experience_format,
                    profile.getYearsOfExperience()
            );
            textDoctorExperience.setText(exp);
        } else {
            textDoctorExperience.setText("-");
        }

        textDoctorReg.setText(
                valueOrDash(profile.getMedicalRegistrationNumber())
        );

        if (profile.getConsultationFee() != null) {
            String fee = getString(
                    R.string.profile_doctor_fee_format,
                    profile.getConsultationFee().toString()
            );
            textDoctorFee.setText(fee);
        } else {
            textDoctorFee.setText("-");
        }

        StringBuilder flags = new StringBuilder();
        if (Boolean.TRUE.equals(profile.getAcceptsNewPatients())) {
            flags.append(getString(R.string.profile_flag_accepts_new)).append(" • ");
        }
        if (Boolean.TRUE.equals(profile.getTeleconsultationEnabled())) {
            flags.append(getString(R.string.profile_flag_teleconsultation)).append(" • ");
        }
        if (flags.length() > 0) {
            flags.setLength(flags.length() - 3); // remove last separator
            textDoctorFlags.setText(flags.toString());
        } else {
            textDoctorFlags.setText(getString(R.string.profile_flags_none));
        }
    }

    private void bindPatientProfile(PatientProfile profile) {
        groupDoctorSection.setVisibility(View.GONE);
        groupPatientSection.setVisibility(View.VISIBLE);

        textPatientTitle.setText(R.string.profile_section_patient_title);

        textPatientDob.setText(
                valueOrDash(profile.getDateOfBirth())
        );

        textPatientGender.setText(
                valueOrDash(profile.getGender())
        );

        textPatientBlood.setText(
                valueOrDash(profile.getBloodType())
        );

        String heightWeight;
        if (profile.getHeightCm() != null && profile.getWeightKg() != null) {
            heightWeight = getString(
                    R.string.profile_patient_height_weight_format,
                    profile.getHeightCm(),
                    profile.getWeightKg()
            );
        } else if (profile.getHeightCm() != null) {
            heightWeight = getString(
                    R.string.profile_patient_height_only_format,
                    profile.getHeightCm()
            );
        } else if (profile.getWeightKg() != null) {
            heightWeight = getString(
                    R.string.profile_patient_weight_only_format,
                    profile.getWeightKg()
            );
        } else {
            heightWeight = "-";
        }
        textPatientHeightWeight.setText(heightWeight);

        String address = joinNonEmpty(", ",
                profile.getAddress(),
                profile.getCity(),
                profile.getCountry()
        );
        textPatientAddress.setText(valueOrDash(address));

        StringBuilder lifestyle = new StringBuilder();
        if (Boolean.TRUE.equals(profile.getSmoker())) {
            lifestyle.append(getString(R.string.profile_flag_smoker)).append(" • ");
        }
        if (Boolean.TRUE.equals(profile.getAlcoholUse())) {
            lifestyle.append(getString(R.string.profile_flag_alcohol)).append(" • ");
        }
        if (lifestyle.length() > 0) {
            lifestyle.setLength(lifestyle.length() - 3);
            textPatientLifestyle.setText(lifestyle.toString());
        } else {
            textPatientLifestyle.setText(getString(R.string.profile_flags_none));
        }
    }

    // --- Helpers ---

    private String valueOrDash(String value) {
        if (TextUtils.isEmpty(value)) return "-";
        return value;
    }

    private String joinNonEmpty(String separator, String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (!TextUtils.isEmpty(p)) {
                if (sb.length() > 0) sb.append(separator);
                sb.append(p);
            }
        }
        return sb.length() == 0 ? "" : sb.toString();
    }
}
