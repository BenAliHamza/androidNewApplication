package tn.esprit.presentation.profile;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import tn.esprit.MainActivity;
import tn.esprit.R;
import tn.esprit.data.profile.ProfileRepository;
import tn.esprit.data.remote.patient.PatientApiService.PatientProfileUpdateRequestDto;
import tn.esprit.domain.doctor.DoctorProfile;
import tn.esprit.domain.patient.PatientProfile;
import tn.esprit.domain.user.User;

public class EditPatientProfileFragment extends Fragment {

    private TextInputEditText inputDob;
    private TextInputEditText inputGender;
    private TextInputEditText inputBloodType;
    private TextInputEditText inputHeightCm;
    private TextInputEditText inputWeightKg;
    private TextInputEditText inputAddress;
    private TextInputEditText inputCity;
    private TextInputEditText inputCountry;
    private TextInputEditText inputMaritalStatus;
    private TextInputEditText inputNotes;
    private CheckBox checkSmoker;
    private CheckBox checkAlcohol;
    private View loadingOverlay;

    private ProfileRepository profileRepository;
    private PatientProfile currentPatientProfile;
    private User currentUser;

    public EditPatientProfileFragment() {
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
        return inflater.inflate(R.layout.fragment_edit_patient_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar_edit_patient);
        inputDob = view.findViewById(R.id.input_dob);
        inputGender = view.findViewById(R.id.input_gender);
        inputBloodType = view.findViewById(R.id.input_blood_type);
        inputHeightCm = view.findViewById(R.id.input_height_cm);
        inputWeightKg = view.findViewById(R.id.input_weight_kg);
        inputAddress = view.findViewById(R.id.input_address);
        inputCity = view.findViewById(R.id.input_city);
        inputCountry = view.findViewById(R.id.input_country);
        inputMaritalStatus = view.findViewById(R.id.input_marital_status);
        inputNotes = view.findViewById(R.id.input_notes);
        checkSmoker = view.findViewById(R.id.check_smoker);
        checkAlcohol = view.findViewById(R.id.check_alcohol);
        MaterialButton buttonComplete = view.findViewById(R.id.button_complete_profile);
        MaterialButton buttonBaseInfo = view.findViewById(R.id.button_base_info);
        loadingOverlay = view.findViewById(R.id.edit_patient_loading_overlay);

        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v ->
                    NavHostFragment.findNavController(this).navigateUp());
        }
        if (buttonComplete != null) {
            buttonComplete.setOnClickListener(v -> savePatientProfile());
        }
        if (buttonBaseInfo != null) {
            buttonBaseInfo.setOnClickListener(v ->
                    NavHostFragment.findNavController(this)
                            .navigate(R.id.action_editPatientProfileFragment_to_userBaseInfoFragment));
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
                currentPatientProfile = patientProfile;

                if (patientProfile != null) {
                    try {
                        if (inputDob != null) {
                            // Assuming PatientProfile keeps dateOfBirth as a String or a
                            // type whose toString() is ISO date.
                            Object dob = patientProfile.getDateOfBirth();
                            inputDob.setText(dob != null ? dob.toString() : "");
                        }
                    } catch (Exception ignored) {
                    }
                    if (inputGender != null) {
                        try {
                            inputGender.setText(patientProfile.getGender());
                        } catch (Exception ignored) {
                        }
                    }
                    if (inputBloodType != null) {
                        try {
                            inputBloodType.setText(patientProfile.getBloodType());
                        } catch (Exception ignored) {
                        }
                    }
                    if (inputHeightCm != null) {
                        try {
                            Integer h = patientProfile.getHeightCm();
                            inputHeightCm.setText(h != null ? String.valueOf(h) : "");
                        } catch (Exception ignored) {
                        }
                    }
                    if (inputWeightKg != null) {
                        try {
                            Integer w = patientProfile.getWeightKg();
                            inputWeightKg.setText(w != null ? String.valueOf(w) : "");
                        } catch (Exception ignored) {
                        }
                    }
                    if (inputAddress != null) {
                        try {
                            inputAddress.setText(patientProfile.getAddress());
                        } catch (Exception ignored) {
                        }
                    }
                    if (inputCity != null) {
                        try {
                            inputCity.setText(patientProfile.getCity());
                        } catch (Exception ignored) {
                        }
                    }
                    if (inputCountry != null) {
                        try {
                            inputCountry.setText(patientProfile.getCountry());
                        } catch (Exception ignored) {
                        }
                    }
                    if (inputMaritalStatus != null) {
                        try {
                            inputMaritalStatus.setText(patientProfile.getMaritalStatus());
                        } catch (Exception ignored) {
                        }
                    }
                    if (inputNotes != null) {
                        try {
                            inputNotes.setText(patientProfile.getNotes());
                        } catch (Exception ignored) {
                        }
                    }
                    if (checkSmoker != null) {
                        try {
                            Boolean smoker = patientProfile.getSmoker();
                            checkSmoker.setChecked(smoker != null && smoker);
                        } catch (Exception ignored) {
                        }
                    }
                    if (checkAlcohol != null) {
                        try {
                            Boolean alcohol = patientProfile.getAlcoholUse();
                            checkAlcohol.setChecked(alcohol != null && alcohol);
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

    private void savePatientProfile() {
        if (currentUser == null) {
            Toast.makeText(requireContext(),
                    R.string.profile_error_unknown_role,
                    Toast.LENGTH_SHORT).show();
            return;
        }
        String role = currentUser.getRole();
        if (role == null || !"PATIENT".equalsIgnoreCase(role)) {
            Toast.makeText(requireContext(),
                    R.string.profile_error_unknown_role,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        PatientProfileUpdateRequestDto request = new PatientProfileUpdateRequestDto();

        if (inputDob != null) {
            request.setDateOfBirth(trimOrNull(inputDob.getText()));
        }
        if (inputGender != null) {
            request.setGender(trimOrNull(inputGender.getText()));
        }
        if (inputBloodType != null) {
            request.setBloodType(trimOrNull(inputBloodType.getText()));
        }
        if (inputAddress != null) {
            request.setAddress(trimOrNull(inputAddress.getText()));
        }
        if (inputCity != null) {
            request.setCity(trimOrNull(inputCity.getText()));
        }
        if (inputCountry != null) {
            request.setCountry(trimOrNull(inputCountry.getText()));
        }
        if (inputMaritalStatus != null) {
            request.setMaritalStatus(trimOrNull(inputMaritalStatus.getText()));
        }
        if (inputNotes != null) {
            request.setNotes(trimOrNull(inputNotes.getText()));
        }

        if (inputHeightCm != null) {
            String hText = trimOrNull(inputHeightCm.getText());
            if (!TextUtils.isEmpty(hText)) {
                try {
                    request.setHeightCm(Integer.parseInt(hText));
                } catch (NumberFormatException e) {
                    Toast.makeText(requireContext(),
                            R.string.profile_error_invalid_height,
                            Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }
        if (inputWeightKg != null) {
            String wText = trimOrNull(inputWeightKg.getText());
            if (!TextUtils.isEmpty(wText)) {
                try {
                    request.setWeightKg(Integer.parseInt(wText));
                } catch (NumberFormatException e) {
                    Toast.makeText(requireContext(),
                            R.string.profile_error_invalid_weight,
                            Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }

        if (checkSmoker != null) {
            request.setSmoker(checkSmoker.isChecked());
        }
        if (checkAlcohol != null) {
            request.setAlcoholUse(checkAlcohol.isChecked());
        }

        showLoading(true);
        profileRepository.updatePatientProfile(request, new ProfileRepository.PatientProfileUpdateCallback() {
            @Override
            public void onSuccess(PatientProfile updatedProfile) {
                if (!isAdded()) return;
                showLoading(false);
                Toast.makeText(requireContext(),
                        R.string.profile_saved,
                        Toast.LENGTH_SHORT).show();

                Activity activity = getActivity();
                if (activity instanceof MainActivity) {
                    ((MainActivity) activity).refreshUserProfileUi();
                }
                NavHostFragment.findNavController(EditPatientProfileFragment.this).navigateUp();
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
