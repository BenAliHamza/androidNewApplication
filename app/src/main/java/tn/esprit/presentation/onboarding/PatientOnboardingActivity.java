package tn.esprit.presentation.onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import tn.esprit.MainActivity;
import tn.esprit.R;
import tn.esprit.data.auth.AuthLocalDataSource;
import tn.esprit.data.remote.ApiClient;
import tn.esprit.data.remote.patient.PatientApiService;
import tn.esprit.data.remote.patient.PatientApiService.PatientProfileUpdateRequestDto;
import tn.esprit.domain.auth.AuthTokens;
import tn.esprit.domain.patient.PatientProfile;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PatientOnboardingActivity extends AppCompatActivity {

    private View rootView;
    private View loadingOverlay;

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
    private MaterialButton buttonComplete;
    private MaterialButton buttonSkip;

    private AuthLocalDataSource authLocalDataSource;
    private PatientApiService patientApiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_patient_onboarding);

        authLocalDataSource = new AuthLocalDataSource(getApplicationContext());
        patientApiService = ApiClient.createService(PatientApiService.class);

        bindViews();
        applyWindowInsets();
        setupListeners();
    }

    private void bindViews() {
        rootView = findViewById(R.id.onboarding_root);
        loadingOverlay = findViewById(R.id.onboarding_loading_overlay);

        inputDob = findViewById(R.id.input_dob);
        inputGender = findViewById(R.id.input_gender);
        inputBloodType = findViewById(R.id.input_blood_type);
        inputHeight = findViewById(R.id.input_height_cm);
        inputWeight = findViewById(R.id.input_weight_kg);
        inputAddress = findViewById(R.id.input_address);
        inputCity = findViewById(R.id.input_city);
        inputCountry = findViewById(R.id.input_country);
        inputMaritalStatus = findViewById(R.id.input_marital_status);
        inputNotes = findViewById(R.id.input_notes);

        checkSmoker = findViewById(R.id.check_smoker);
        checkAlcohol = findViewById(R.id.check_alcohol);

        buttonComplete = findViewById(R.id.button_complete_profile);
        buttonSkip = findViewById(R.id.button_skip_for_now);
    }

    private void applyWindowInsets() {
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

    private void setupListeners() {
        // Save & continue
        buttonComplete.setOnClickListener(v -> submitProfile());

        // Skip for now -> go directly to home
        buttonSkip.setOnClickListener(v -> {
            Snackbar.make(
                    rootView,
                    getString(R.string.onboarding_patient_skip_toast),
                    Snackbar.LENGTH_SHORT
            ).show();
            goToMain();
        });
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

        // Only validate DOB format if user filled it
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
        request.setSmoker(checkSmoker.isChecked());
        request.setAlcoholUse(checkAlcohol.isChecked());
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
                    public void onResponse(Call<PatientProfile> call, Response<PatientProfile> response) {
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
                        goToMain();
                    }

                    @Override
                    public void onFailure(Call<PatientProfile> call, Throwable t) {
                        showLoading(false);
                        Snackbar.make(
                                rootView,
                                getString(R.string.onboarding_patient_error_network),
                                Snackbar.LENGTH_LONG
                        ).show();
                    }
                });
    }

    private String buildAuthHeader(AuthTokens tokens) {
        String type = tokens.getTokenType() != null ? tokens.getTokenType() : "Bearer";
        return type + " " + tokens.getAccessToken();
    }

    private String textOf(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
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

    private void showLoading(boolean show) {
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        }

        boolean enabled = !show;

        buttonComplete.setEnabled(enabled);
        buttonSkip.setEnabled(enabled);
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
    }

    private void goToMain() {
        Intent intent = new Intent(PatientOnboardingActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void goToLoginFallback() {
        Intent intent = new Intent(
                PatientOnboardingActivity.this,
                tn.esprit.presentation.auth.LoginActivity.class
        );
        startActivity(intent);
        finish();
    }
}
