package tn.esprit.presentation.profile;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

import java.math.BigDecimal;

import tn.esprit.MainActivity;
import tn.esprit.R;
import tn.esprit.data.profile.ProfileRepository;
import tn.esprit.data.remote.doctor.DoctorApiService.DoctorProfileUpdateRequestDto;
import tn.esprit.domain.doctor.DoctorProfile;
import tn.esprit.domain.patient.PatientProfile;
import tn.esprit.domain.user.User;

public class ProfileEditFragment extends Fragment {

    private TextInputEditText inputName;
    private TextInputEditText inputPhone;
    private TextInputEditText inputClinic;
    private TextInputEditText inputCity;
    private TextInputEditText inputCountry;
    private TextInputEditText inputRegNumber;
    private TextInputEditText inputFee;
    private TextInputEditText inputBio;
    private SwitchMaterial switchAcceptsNew;
    private SwitchMaterial switchTeleconsult;
    private View loadingOverlay;

    private ProfileRepository profileRepository;
    private User currentUser;
    private DoctorProfile currentDoctorProfile;

    public ProfileEditFragment() {
        // Required empty constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        profileRepository = new ProfileRepository(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile_edit, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageButton buttonBack = view.findViewById(R.id.button_back);
        inputName = view.findViewById(R.id.input_name);
        inputPhone = view.findViewById(R.id.input_phone);
        inputClinic = view.findViewById(R.id.input_clinic);
        inputCity = view.findViewById(R.id.input_city);
        inputCountry = view.findViewById(R.id.input_country);
        inputRegNumber = view.findViewById(R.id.input_reg_number);
        inputFee = view.findViewById(R.id.input_fee);
        inputBio = view.findViewById(R.id.input_bio);
        switchAcceptsNew = view.findViewById(R.id.switch_accepts_new);
        switchTeleconsult = view.findViewById(R.id.switch_teleconsult);
        MaterialButton buttonSave = view.findViewById(R.id.button_save);
        MaterialButton buttonCancel = view.findViewById(R.id.button_cancel);
        MaterialButton buttonBaseInfo = view.findViewById(R.id.button_base_info);
        loadingOverlay = view.findViewById(R.id.edit_loading_overlay);

        if (buttonBack != null) {
            buttonBack.setOnClickListener(v ->
                    NavHostFragment.findNavController(this).navigateUp());
        }
        if (buttonCancel != null) {
            buttonCancel.setOnClickListener(v ->
                    NavHostFragment.findNavController(this).navigateUp());
        }
        if (buttonSave != null) {
            buttonSave.setOnClickListener(v -> saveDoctorProfile());
        }
        if (buttonBaseInfo != null) {
            buttonBaseInfo.setOnClickListener(v ->
                    NavHostFragment.findNavController(this)
                            .navigate(R.id.action_profileEditFragment_to_userBaseInfoFragment));
        }

        // Name and phone are base user fields, edited from the "Base info" screen.
        // Here they are read-only for display only, to avoid fake editable fields.
        if (inputName != null) {
            inputName.setEnabled(false);
            inputName.setFocusable(false);
            inputName.setFocusableInTouchMode(false);
        }
        if (inputPhone != null) {
            inputPhone.setEnabled(false);
            inputPhone.setFocusable(false);
            inputPhone.setFocusableInTouchMode(false);
        }

        loadProfileForEdit();
    }

    private void showLoading(boolean show) {
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void loadProfileForEdit() {
        showLoading(true);
        profileRepository.loadProfile(new ProfileRepository.ProfileCallback() {
            @Override
            public void onSuccess(User user,
                                  DoctorProfile doctorProfile,
                                  PatientProfile patientProfile) {
                if (!isAdded()) return;
                showLoading(false);

                currentUser = user;
                currentDoctorProfile = doctorProfile;

                // Prefill base user info (read-only here; real editing is in base info screen).
                if (inputName != null && user != null) {
                    String first = user.getFirstname() != null ? user.getFirstname() : "";
                    String last = user.getLastname() != null ? user.getLastname() : "";
                    String combined = (first + " " + last).trim();
                    if (TextUtils.isEmpty(combined)) {
                        combined = user.getEmail();
                    }
                    inputName.setText(combined);
                }
                if (inputPhone != null && user != null) {
                    inputPhone.setText(user.getPhone());
                }

                if (doctorProfile != null) {
                    if (inputClinic != null) {
                        try {
                            inputClinic.setText(doctorProfile.getClinicAddress());
                        } catch (Exception ignored) {
                        }
                    }
                    if (inputCity != null) {
                        try {
                            inputCity.setText(doctorProfile.getCity());
                        } catch (Exception ignored) {
                        }
                    }
                    if (inputCountry != null) {
                        try {
                            inputCountry.setText(doctorProfile.getCountry());
                        } catch (Exception ignored) {
                        }
                    }
                    if (inputRegNumber != null) {
                        try {
                            inputRegNumber.setText(doctorProfile.getMedicalRegistrationNumber());
                        } catch (Exception ignored) {
                        }
                    }
                    if (inputFee != null) {
                        try {
                            BigDecimal fee = doctorProfile.getConsultationFee();
                            inputFee.setText(fee != null ? fee.toPlainString() : "");
                        } catch (Exception ignored) {
                        }
                    }
                    if (inputBio != null) {
                        try {
                            inputBio.setText(doctorProfile.getBio());
                        } catch (Exception ignored) {
                        }
                    }
                    if (switchAcceptsNew != null) {
                        try {
                            Boolean accepts = doctorProfile.getAcceptsNewPatients();
                            switchAcceptsNew.setChecked(accepts != null && accepts);
                        } catch (Exception ignored) {
                        }
                    }
                    if (switchTeleconsult != null) {
                        try {
                            Boolean tele = doctorProfile.getTeleconsultationEnabled();
                            switchTeleconsult.setChecked(tele != null && tele);
                        } catch (Exception ignored) {
                        }
                    }
                }
            }

            @Override
            public void onError(Throwable throwable, Integer httpCode, String errorBody) {
                if (!isAdded()) return;
                showLoading(false);
                Toast.makeText(requireContext(),
                        R.string.profile_error_loading,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveDoctorProfile() {
        if (currentUser == null) {
            Toast.makeText(requireContext(),
                    R.string.profile_error_unknown_role,
                    Toast.LENGTH_SHORT).show();
            return;
        }
        String role = currentUser.getRole();
        if (role == null || !"DOCTOR".equalsIgnoreCase(role)) {
            Toast.makeText(requireContext(),
                    R.string.profile_error_unknown_role,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        DoctorProfileUpdateRequestDto request = new DoctorProfileUpdateRequestDto();

        // We only edit clinic, city, country, reg number, fee, bio, flags here.
        if (inputClinic != null) {
            request.setClinicAddress(trimOrNull(inputClinic.getText()));
        }
        if (inputCity != null) {
            request.setCity(trimOrNull(inputCity.getText()));
        }
        if (inputCountry != null) {
            request.setCountry(trimOrNull(inputCountry.getText()));
        }
        if (inputRegNumber != null) {
            request.setMedicalRegistrationNumber(trimOrNull(inputRegNumber.getText()));
        }
        if (inputBio != null) {
            request.setBio(trimOrNull(inputBio.getText()));
        }

        if (inputFee != null) {
            String feeText = trimOrNull(inputFee.getText());
            if (!TextUtils.isEmpty(feeText)) {
                try {
                    request.setConsultationFee(new BigDecimal(feeText));
                } catch (NumberFormatException e) {
                    Toast.makeText(requireContext(),
                            R.string.profile_error_invalid_fee,
                            Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }

        if (switchAcceptsNew != null) {
            request.setAcceptsNewPatients(switchAcceptsNew.isChecked());
        }
        if (switchTeleconsult != null) {
            request.setTeleconsultationEnabled(switchTeleconsult.isChecked());
        }

        // Preserve fields that are not editable from UI so we don't accidentally null them.
        if (currentDoctorProfile != null) {
            try {
                if (request.getYearsOfExperience() == null) {
                    request.setYearsOfExperience(currentDoctorProfile.getYearsOfExperience());
                }
            } catch (Exception ignored) {
            }
            try {
                if (request.getMaxDailyAppointments() == null) {
                    request.setMaxDailyAppointments(currentDoctorProfile.getMaxDailyAppointments());
                }
            } catch (Exception ignored) {
            }
            try {
                if (request.getAverageConsultationDurationMinutes() == null) {
                    request.setAverageConsultationDurationMinutes(
                            currentDoctorProfile.getAverageConsultationDurationMinutes()
                    );
                }
            } catch (Exception ignored) {
            }
        }

        showLoading(true);
        profileRepository.updateDoctorProfile(request, new ProfileRepository.DoctorProfileUpdateCallback() {
            @Override
            public void onSuccess(DoctorProfile updatedProfile) {
                if (!isAdded()) return;
                showLoading(false);
                Toast.makeText(requireContext(),
                        R.string.profile_saved,
                        Toast.LENGTH_SHORT).show();

                Activity activity = getActivity();
                if (activity instanceof MainActivity) {
                    ((MainActivity) activity).refreshUserProfileUi();
                }
                NavHostFragment.findNavController(ProfileEditFragment.this).navigateUp();
            }

            @Override
            public void onError(Throwable throwable, Integer httpCode, String errorBody) {
                if (!isAdded()) return;
                showLoading(false);
                Toast.makeText(requireContext(),
                        R.string.profile_error_saving,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Nullable
    private String trimOrNull(@Nullable CharSequence cs) {
        if (cs == null) return null;
        String s = cs.toString().trim();
        return s.isEmpty() ? null : s;
    }
}
