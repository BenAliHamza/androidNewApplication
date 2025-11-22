package tn.esprit.presentation.profile;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import tn.esprit.R;
import tn.esprit.data.auth.AuthLocalDataSource;
import tn.esprit.data.profile.ProfileRepository;
import tn.esprit.data.remote.ApiClient;
import tn.esprit.data.remote.patient.PatientApiService;
import tn.esprit.data.remote.patient.PatientApiService.PatientProfileUpdateRequestDto;
import tn.esprit.domain.auth.AuthTokens;
import tn.esprit.domain.doctor.DoctorProfile;
import tn.esprit.domain.patient.PatientProfile;
import tn.esprit.domain.user.User;
import tn.esprit.presentation.auth.AuthGateActivity;

public class EditPatientProfileFragment extends Fragment {

    private View rootView;
    private View loadingOverlay;

    private MaterialToolbar toolbar;

    private TextInputEditText inputDob;
    private TextInputEditText inputGender;
    private TextInputEditText inputBloodType;
    private TextInputEditText inputHeight;
    private TextInputEditText inputWeight;
    private TextInputEditText inputAddress;
    private TextInputEditText inputCity;
    private TextInputEditText inputCountry;
    private TextInputEditText inputMaritalStatus;
    private TextInputEditText inputNotes;
    private CheckBox checkSmoker;
    private CheckBox checkAlcohol;
    private MaterialButton buttonSave;
    private MaterialButton buttonBaseInfo;

    private ProfileRepository profileRepository;
    private AuthLocalDataSource authLocalDataSource;
    private PatientApiService patientApiService;

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

        rootView = view.findViewById(R.id.edit_patient_root);
        loadingOverlay = view.findViewById(R.id.edit_patient_loading_overlay);
        toolbar = view.findViewById(R.id.toolbar_edit_patient);

        inputDob = view.findViewById(R.id.input_dob);
        inputGender = view.findViewById(R.id.input_gender);
        inputBloodType = view.findViewById(R.id.input_blood_type);
        inputHeight = view.findViewById(R.id.input_height_cm);
        inputWeight = view.findViewById(R.id.input_weight_kg);
        inputAddress = view.findViewById(R.id.input_address);
        inputCity = view.findViewById(R.id.input_city);
        inputCountry = view.findViewById(R.id.input_country);
        inputMaritalStatus = view.findViewById(R.id.input_marital_status);
        inputNotes = view.findViewById(R.id.input_notes);
        checkSmoker = view.findViewById(R.id.check_smoker);
        checkAlcohol = view.findViewById(R.id.check_alcohol);
        buttonSave = view.findViewById(R.id.button_complete_profile);
        buttonBaseInfo = view.findViewById(R.id.button_base_info);

        profileRepository = new ProfileRepository(requireContext());
        authLocalDataSource = new AuthLocalDataSource(requireContext().getApplicationContext());
        patientApiService = ApiClient.createService(PatientApiService.class);

        setupToolbar();
        setupListeners();
        loadExistingProfile();
    }

    private void setupToolbar() {
        if (toolbar != null) {
            toolbar.setTitle(R.string.profile_patient_edit_title);
            // Use a system drawable to avoid custom drawables
            toolbar.setNavigationIcon(android.R.drawable.ic_media_previous);
            toolbar.setNavigationOnClickListener(v -> {
                try {
                    NavHostFragment.findNavController(this).navigateUp();
                } catch (IllegalStateException e) {
                    requireActivity().onBackPressed();
                }
            });
        }
    }

    private void setupListeners() {
        buttonSave.setOnClickListener(v -> submitProfile());
        if (buttonBaseInfo != null) {
            buttonBaseInfo.setOnClickListener(v ->
                    NavHostFragment.findNavController(EditPatientProfileFragment.this)
                            .navigate(R.id.userBaseInfoFragment)
            );
        }
    }

    private void loadExistingProfile() {
        showLoading(true);

        profileRepository.loadProfile(new ProfileRepository.ProfileCallback() {
            @Override
            public void onSuccess(User user,
                                  DoctorProfile doctorProfile,
                                  PatientProfile patientProfile) {
                if (!isAdded()) return;
                showLoading(false);

                if (patientProfile != null) {
                    bindPatientProfile(patientProfile);
                }
            }

            @Override
            public void onError(Throwable throwable,
                                Integer httpCode,
                                String errorBody) {
                if (!isAdded()) return;
                showLoading(false);

                Snackbar.make(
                        rootView,
                        getString(R.string.profile_error_generic),
                        Snackbar.LENGTH_LONG
                ).show();
            }
        });
    }

    private void bindPatientProfile(@NonNull PatientProfile profile) {
        if (profile.getDateOfBirth() != null) {
            inputDob.setText(profile.getDateOfBirth());
        }
        if (profile.getGender() != null) {
            inputGender.setText(profile.getGender());
        }
        if (profile.getBloodType() != null) {
            inputBloodType.setText(profile.getBloodType());
        }
        if (profile.getHeightCm() != null) {
            inputHeight.setText(String.valueOf(profile.getHeightCm()));
        }
        if (profile.getWeightKg() != null) {
            inputWeight.setText(String.valueOf(profile.getWeightKg()));
        }
        if (profile.getAddress() != null) {
            inputAddress.setText(profile.getAddress());
        }
        if (profile.getCity() != null) {
            inputCity.setText(profile.getCity());
        }
        if (profile.getCountry() != null) {
            inputCountry.setText(profile.getCountry());
        }
        if (profile.getMaritalStatus() != null) {
            inputMaritalStatus.setText(profile.getMaritalStatus());
        }
        if (profile.getNotes() != null) {
            inputNotes.setText(profile.getNotes());
        }
        if (profile.getSmoker() != null) {
            checkSmoker.setChecked(profile.getSmoker());
        }
        if (profile.getAlcoholUse() != null) {
            checkAlcohol.setChecked(profile.getAlcoholUse());
        }
    }

    private void submitProfile() {
        String dob = textOf(inputDob);
        String gender = textOf(inputGender);
        String bloodType = textOf(inputBloodType);
        String heightStr = textOf(inputHeight);
        String weightStr = textOf(inputWeight);
        String address = textOf(inputAddress);
        String city = textOf(inputCity);
        String country = textOf(inputCountry);
        String maritalStatus = textOf(inputMaritalStatus);
        String notes = textOf(inputNotes);
        boolean smoker = checkSmoker.isChecked();
        boolean alcoholUse = checkAlcohol.isChecked();

        // Validate DOB format if provided
        if (!TextUtils.isEmpty(dob) && !dob.matches("\\d{4}-\\d{2}-\\d{2}")) {
            inputDob.setError(getString(R.string.onboarding_patient_error_dob_format));
            return;
        } else {
            inputDob.setError(null);
        }

        Integer height = parseIntegerOrNull(heightStr);
        Integer weight = parseIntegerOrNull(weightStr);

        PatientProfileUpdateRequestDto request = new PatientProfileUpdateRequestDto();
        request.setDateOfBirth(emptyToNull(dob));
        request.setGender(emptyToNull(gender));
        request.setBloodType(emptyToNull(bloodType));
        request.setHeightCm(height);
        request.setWeightKg(weight);
        request.setAddress(emptyToNull(address));
        request.setCity(emptyToNull(city));
        request.setCountry(emptyToNull(country));
        request.setMaritalStatus(emptyToNull(maritalStatus));
        request.setSmoker(smoker);
        request.setAlcoholUse(alcoholUse);
        request.setNotes(emptyToNull(notes));

        AuthTokens tokens = authLocalDataSource.getTokens();
        if (tokens == null || tokens.getAccessToken() == null) {
            Snackbar.make(
                    rootView,
                    getString(R.string.onboarding_patient_error_not_authenticated),
                    Snackbar.LENGTH_LONG
            ).show();
            goToLoginFallback();
            return;
        }

        String authHeader = buildAuthHeader(tokens);

        showLoading(true);

        patientApiService.updateMyProfile(authHeader, request)
                .enqueue(new Callback<PatientProfile>() {
                    @Override
                    public void onResponse(Call<PatientProfile> call,
                                           Response<PatientProfile> response) {
                        if (!isAdded()) {
                            return;
                        }
                        showLoading(false);

                        if (!response.isSuccessful()) {
                            Snackbar.make(
                                    rootView,
                                    getString(R.string.onboarding_patient_error_generic),
                                    Snackbar.LENGTH_LONG
                            ).show();
                            return;
                        }

                        Snackbar.make(
                                rootView,
                                getString(R.string.onboarding_patient_success),
                                Snackbar.LENGTH_LONG
                        ).show();

                        try {
                            NavHostFragment.findNavController(EditPatientProfileFragment.this)
                                    .navigateUp();
                        } catch (IllegalStateException e) {
                            requireActivity().onBackPressed();
                        }
                    }

                    @Override
                    public void onFailure(Call<PatientProfile> call, Throwable t) {
                        if (!isAdded()) {
                            return;
                        }
                        showLoading(false);
                        Snackbar.make(
                                rootView,
                                getString(R.string.onboarding_patient_error_network),
                                Snackbar.LENGTH_LONG
                        ).show();
                    }
                });
    }

    private String textOf(TextInputEditText editText) {
        return editText.getText() != null
                ? editText.getText().toString().trim()
                : "";
    }

    private Integer parseIntegerOrNull(String value) {
        if (TextUtils.isEmpty(value)) return null;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String emptyToNull(String value) {
        return TextUtils.isEmpty(value) ? null : value;
    }

    private String buildAuthHeader(AuthTokens tokens) {
        String type = tokens.getTokenType() != null ? tokens.getTokenType() : "Bearer";
        return type + " " + tokens.getAccessToken();
    }

    private void showLoading(boolean show) {
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        }

        boolean enabled = !show;

        inputDob.setEnabled(enabled);
        inputGender.setEnabled(enabled);
        inputBloodType.setEnabled(enabled);
        inputHeight.setEnabled(enabled);
        inputWeight.setEnabled(enabled);
        inputAddress.setEnabled(enabled);
        inputCity.setEnabled(enabled);
        inputCountry.setEnabled(enabled);
        inputMaritalStatus.setEnabled(enabled);
        inputNotes.setEnabled(enabled);
        checkSmoker.setEnabled(enabled);
        checkAlcohol.setEnabled(enabled);
        buttonSave.setEnabled(enabled);
        if (buttonBaseInfo != null) {
            buttonBaseInfo.setEnabled(enabled);
        }
    }

    private void goToLoginFallback() {
        if (!isAdded()) return;
        Intent intent = new Intent(requireContext(), AuthGateActivity.class);
        startActivity(intent);
        requireActivity().finish();
    }
}
