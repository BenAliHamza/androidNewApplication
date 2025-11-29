package tn.esprit.presentation.profile;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.annotation.ArrayRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import tn.esprit.MainActivity;
import tn.esprit.R;
import tn.esprit.data.profile.ProfileRepository;
import tn.esprit.data.remote.patient.PatientApiService.PatientProfileUpdateRequestDto;
import tn.esprit.domain.doctor.DoctorProfile;
import tn.esprit.domain.patient.PatientProfile;
import tn.esprit.domain.user.User;

public class EditPatientProfileFragment extends Fragment {

    private TextInputLayout layoutDob;
    private TextInputLayout layoutHeight;
    private TextInputLayout layoutWeight;
    private TextInputLayout layoutGender;
    private TextInputLayout layoutBloodType;
    private TextInputLayout layoutMaritalStatus;

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

        layoutDob = view.findViewById(R.id.layout_dob);
        layoutHeight = view.findViewById(R.id.layout_height_cm);
        layoutWeight = view.findViewById(R.id.layout_weight_kg);
        layoutGender = view.findViewById(R.id.layout_gender);
        layoutBloodType = view.findViewById(R.id.layout_blood_type);
        layoutMaritalStatus = view.findViewById(R.id.layout_marital_status);

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

        // DOB as date picker
        if (inputDob != null) {
            inputDob.setFocusable(false);
            inputDob.setFocusableInTouchMode(false);
            inputDob.setClickable(true);
            inputDob.setOnClickListener(v -> showDatePicker());
        }

        attachClearErrorTextWatchers();
        setupPickers();

        loadProfileForEdit();
    }

    // ------------------------------------------------------------
    // Picker dialogs for gender / blood type / marital status
    // using existing arrays: profile_genders, profile_blood_types, profile_marital_statuses
    // ------------------------------------------------------------
    private void setupPickers() {
        if (!isAdded()) return;

        setupPickerForField(inputGender, R.array.profile_genders);
        setupPickerForField(inputBloodType, R.array.profile_blood_types);
        setupPickerForField(inputMaritalStatus, R.array.profile_marital_statuses);
    }

    private void setupPickerForField(@Nullable TextInputEditText editText,
                                     @ArrayRes int arrayResId) {
        if (editText == null) return;

        // Prevent keyboard input → tap only
        editText.setFocusable(false);
        editText.setFocusableInTouchMode(false);
        editText.setClickable(true);

        editText.setOnClickListener(v -> showOptionsDialog(editText, arrayResId));
    }

    private void showOptionsDialog(@NonNull TextInputEditText target,
                                   @ArrayRes int arrayResId) {
        if (!isAdded()) return;

        String[] items;
        try {
            items = requireContext().getResources().getStringArray(arrayResId);
        } catch (Resources.NotFoundException e) {
            // Fail-safe: do nothing if array missing
            return;
        }

        String title = target.getHint() != null ? target.getHint().toString()
                : getString(R.string.profile_patient_edit_title);

        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setItems(items, (dialog, which) -> {
                    if (which >= 0 && which < items.length) {
                        target.setText(items[which]);
                    }
                })
                .show();
    }

    // ------------------------------------------------------------
    // Watchers to clear errors
    // ------------------------------------------------------------
    private void attachClearErrorTextWatchers() {
        if (inputDob != null && layoutDob != null) {
            inputDob.addTextChangedListener(new SimpleClearErrorWatcher(layoutDob));
        }
        if (inputHeightCm != null && layoutHeight != null) {
            inputHeightCm.addTextChangedListener(new SimpleClearErrorWatcher(layoutHeight));
        }
        if (inputWeightKg != null && layoutWeight != null) {
            inputWeightKg.addTextChangedListener(new SimpleClearErrorWatcher(layoutWeight));
        }
        if (inputGender != null && layoutGender != null) {
            inputGender.addTextChangedListener(new SimpleClearErrorWatcher(layoutGender));
        }
        if (inputBloodType != null && layoutBloodType != null) {
            inputBloodType.addTextChangedListener(new SimpleClearErrorWatcher(layoutBloodType));
        }
        if (inputMaritalStatus != null && layoutMaritalStatus != null) {
            inputMaritalStatus.addTextChangedListener(new SimpleClearErrorWatcher(layoutMaritalStatus));
        }
    }

    private static class SimpleClearErrorWatcher implements TextWatcher {

        private final TextInputLayout layout;

        SimpleClearErrorWatcher(TextInputLayout layout) {
            this.layout = layout;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // no-op
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (layout != null && layout.getError() != null) {
                layout.setError(null);
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
            // no-op
        }
    }

    private void showLoading(boolean show) {
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    // ------------------------------------------------------------
    // Load existing profile
    // ------------------------------------------------------------
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
                            String dob = patientProfile.getDateOfBirth();
                            inputDob.setText(dob != null ? dob : "");
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

    // ------------------------------------------------------------
    // Save
    // ------------------------------------------------------------
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

        if (!validateAndFillRequest(request)) {
            return; // errors already shown inline
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

    // ------------------------------------------------------------
    // Validation
    // ------------------------------------------------------------
    private boolean validateAndFillRequest(PatientProfileUpdateRequestDto request) {
        boolean hasError = false;

        // Clear old errors
        if (layoutDob != null) layoutDob.setError(null);
        if (layoutHeight != null) layoutHeight.setError(null);
        if (layoutWeight != null) layoutWeight.setError(null);
        if (layoutGender != null) layoutGender.setError(null);
        if (layoutBloodType != null) layoutBloodType.setError(null);
        if (layoutMaritalStatus != null) layoutMaritalStatus.setError(null);

        // DOB
        String dob = inputDob != null ? trimOrNull(inputDob.getText()) : null;
        if (!TextUtils.isEmpty(dob)) {
            if (!isValidPastDate(dob)) {
                if (layoutDob != null) {
                    layoutDob.setError(getString(R.string.profile_error_invalid_dob));
                }
                hasError = true;
            } else {
                request.setDateOfBirth(dob);
            }
        }

        // Gender: optional but if filled must be from array
        String gender = inputGender != null ? trimOrNull(inputGender.getText()) : null;
        if (!TextUtils.isEmpty(gender)) {
            if (!isInStringArray(gender, R.array.profile_genders)) {
                if (layoutGender != null) {
                    layoutGender.setError(getString(R.string.profile_error_invalid_gender));
                }
                hasError = true;
            } else {
                request.setGender(gender);
            }
        }

        // Blood type: optional but if filled must be valid blood group
        String blood = inputBloodType != null ? trimOrNull(inputBloodType.getText()) : null;
        if (!TextUtils.isEmpty(blood)) {
            if (!isInStringArray(blood, R.array.profile_blood_types)) {
                if (layoutBloodType != null) {
                    layoutBloodType.setError(getString(R.string.profile_error_invalid_blood_type));
                }
                hasError = true;
            } else {
                request.setBloodType(blood);
            }
        }

        // Address / city / country
        if (inputAddress != null) {
            request.setAddress(trimOrNull(inputAddress.getText()));
        }
        if (inputCity != null) {
            request.setCity(trimOrNull(inputCity.getText()));
        }
        if (inputCountry != null) {
            request.setCountry(trimOrNull(inputCountry.getText()));
        }

        // Marital status: optional but if filled must be from array
        String marital = inputMaritalStatus != null ? trimOrNull(inputMaritalStatus.getText()) : null;
        if (!TextUtils.isEmpty(marital)) {
            if (!isInStringArray(marital, R.array.profile_marital_statuses)) {
                if (layoutMaritalStatus != null) {
                    layoutMaritalStatus.setError(
                            getString(R.string.profile_error_invalid_marital_status)
                    );
                }
                hasError = true;
            } else {
                request.setMaritalStatus(marital);
            }
        }

        // Notes
        if (inputNotes != null) {
            request.setNotes(trimOrNull(inputNotes.getText()));
        }

        // Height
        if (inputHeightCm != null) {
            String hText = trimOrNull(inputHeightCm.getText());
            if (!TextUtils.isEmpty(hText)) {
                try {
                    int h = Integer.parseInt(hText);
                    // Reasonable human range in cm
                    if (h < 50 || h > 260) {
                        if (layoutHeight != null) {
                            layoutHeight.setError(getString(R.string.profile_error_invalid_height));
                        }
                        hasError = true;
                    } else {
                        request.setHeightCm(h);
                    }
                } catch (NumberFormatException e) {
                    if (layoutHeight != null) {
                        layoutHeight.setError(getString(R.string.profile_error_invalid_height));
                    }
                    hasError = true;
                }
            }
        }

        // Weight
        if (inputWeightKg != null) {
            String wText = trimOrNull(inputWeightKg.getText());
            if (!TextUtils.isEmpty(wText)) {
                try {
                    int w = Integer.parseInt(wText);
                    // Reasonable human range in kg
                    if (w < 3 || w > 350) {
                        if (layoutWeight != null) {
                            layoutWeight.setError(getString(R.string.profile_error_invalid_weight));
                        }
                        hasError = true;
                    } else {
                        request.setWeightKg(w);
                    }
                } catch (NumberFormatException e) {
                    if (layoutWeight != null) {
                        layoutWeight.setError(getString(R.string.profile_error_invalid_weight));
                    }
                    hasError = true;
                }
            }
        }

        if (checkSmoker != null) {
            request.setSmoker(checkSmoker.isChecked());
        }
        if (checkAlcohol != null) {
            request.setAlcoholUse(checkAlcohol.isChecked());
        }

        return !hasError;
    }

    private boolean isInStringArray(@NonNull String value, @ArrayRes int arrayResId) {
        if (!isAdded()) return true; // fail-safe, don't block save
        try {
            Resources res = requireContext().getResources();
            String[] items = res.getStringArray(arrayResId);
            String normalized = normalize(value);
            for (String item : items) {
                if (normalized.equals(normalize(item))) {
                    return true;
                }
            }
        } catch (Resources.NotFoundException ignored) {
            // If array is missing we don't hard-fail, just accept value.
            return true;
        }
        return false;
    }

    private String normalize(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isValidPastDate(@NonNull String isoDate) {
        // Expected format: yyyy-MM-dd
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        sdf.setLenient(false);
        try {
            long time = sdf.parse(isoDate).getTime();
            long now = System.currentTimeMillis();
            // allow today, disallow future
            return time <= now;
        } catch (ParseException e) {
            return false;
        }
    }

    private void showDatePicker() {
        if (!isAdded()) return;

        int year;
        int month;
        int day;

        // Fallback: 30 years ago from today
        Calendar fallback = Calendar.getInstance();
        fallback.add(Calendar.YEAR, -30);
        year = fallback.get(Calendar.YEAR);
        month = fallback.get(Calendar.MONTH);
        day = fallback.get(Calendar.DAY_OF_MONTH);

        String dobText = null;
        if (inputDob != null && inputDob.getText() != null) {
            dobText = inputDob.getText().toString().trim();
        }

        if (!TextUtils.isEmpty(dobText) && dobText.length() >= 10) {
            try {
                String[] parts = dobText.substring(0, 10).split("-");
                if (parts.length == 3) {
                    year = Integer.parseInt(parts[0]);
                    month = Integer.parseInt(parts[1]) - 1;
                    day = Integer.parseInt(parts[2]);
                }
            } catch (Exception ignored) {
            }
        } else if (currentPatientProfile != null &&
                !TextUtils.isEmpty(currentPatientProfile.getDateOfBirth())) {
            try {
                String[] parts = currentPatientProfile.getDateOfBirth().substring(0, 10).split("-");
                if (parts.length == 3) {
                    year = Integer.parseInt(parts[0]);
                    month = Integer.parseInt(parts[1]) - 1;
                    day = Integer.parseInt(parts[2]);
                }
            } catch (Exception ignored) {
            }
        }

        DatePickerDialog dialog = new DatePickerDialog(
                requireContext(),
                (view, y, m, d) -> {
                    String formatted = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d);
                    if (inputDob != null) {
                        inputDob.setText(formatted);
                    }
                },
                year,
                month,
                day
        );

        dialog.show();
    }

    @Nullable
    private String trimOrNull(@Nullable CharSequence cs) {
        if (cs == null) return null;
        String s = cs.toString().trim();
        return s.isEmpty() ? null : s;
    }
}
