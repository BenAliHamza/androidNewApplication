package tn.esprit.presentation.onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import android.widget.CheckBox;

import java.math.BigDecimal;

import tn.esprit.MainActivity;
import tn.esprit.R;
import tn.esprit.data.auth.AuthLocalDataSource;
import tn.esprit.data.remote.ApiClient;
import tn.esprit.data.remote.doctor.DoctorApiService;
import tn.esprit.domain.auth.AuthTokens;
import tn.esprit.domain.doctor.DoctorProfile;
import tn.esprit.presentation.auth.AuthGateActivity;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DoctorProfileOnboardingActivity extends AppCompatActivity {

    private View rootView;
    private View loadingOverlay;

    private TextInputEditText inputBio;
    private TextInputEditText inputYearsExperience;
    private TextInputEditText inputClinicAddress;
    private TextInputEditText inputCity;
    private TextInputEditText inputCountry;
    private TextInputEditText inputMedicalReg;
    private TextInputEditText inputConsultationFee;
    private TextInputEditText inputMaxDaily;
    private TextInputEditText inputAvgDuration;
    private CheckBox checkAcceptsNew;
    private CheckBox checkTeleconsultation;
    private MaterialButton buttonSave;
    private MaterialButton buttonSkip;

    private AuthLocalDataSource authLocalDataSource;
    private DoctorApiService doctorApiService;
    private String authHeader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_doctor_profile_onboarding);

        authLocalDataSource = new AuthLocalDataSource(getApplicationContext());
        doctorApiService = ApiClient.createService(DoctorApiService.class);

        bindViews();
        applyWindowInsets();
        initAuthHeader();
        setupListeners();
        loadExistingProfile();
    }

    private void bindViews() {
        rootView = findViewById(R.id.doctor_profile_onboarding_root);
        loadingOverlay = findViewById(R.id.doctor_profile_loading_overlay);

        inputBio = findViewById(R.id.input_bio);
        inputYearsExperience = findViewById(R.id.input_years_experience);
        inputClinicAddress = findViewById(R.id.input_clinic_address);
        inputCity = findViewById(R.id.input_city);
        inputCountry = findViewById(R.id.input_country);
        inputMedicalReg = findViewById(R.id.input_medical_reg);
        inputConsultationFee = findViewById(R.id.input_consultation_fee);
        inputMaxDaily = findViewById(R.id.input_max_daily);
        inputAvgDuration = findViewById(R.id.input_avg_duration);

        checkAcceptsNew = findViewById(R.id.check_accepts_new);
        checkTeleconsultation = findViewById(R.id.check_teleconsultation);

        buttonSave = findViewById(R.id.button_save_profile);
        buttonSkip = findViewById(R.id.button_skip_profile);
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

    private void initAuthHeader() {
        AuthTokens tokens = authLocalDataSource.getTokens();
        if (tokens == null || tokens.getAccessToken() == null) {
            goToLoginFallback();
            return;
        }
        String type = tokens.getTokenType() != null ? tokens.getTokenType() : "Bearer";
        authHeader = type + " " + tokens.getAccessToken();
    }

    private void setupListeners() {
        // Optional step: doctor can skip and go to home
        buttonSave.setOnClickListener(v -> submitProfile());
        buttonSkip.setOnClickListener(v -> goToMain());
    }

    private void loadExistingProfile() {
        if (authHeader == null) return;

        showLoading(true);

        doctorApiService.getMyProfile(authHeader)
                .enqueue(new Callback<DoctorProfile>() {
                    @Override
                    public void onResponse(Call<DoctorProfile> call, Response<DoctorProfile> response) {
                        showLoading(false);
                        if (!response.isSuccessful() || response.body() == null) {
                            return;
                        }
                        bindProfile(response.body());
                    }

                    @Override
                    public void onFailure(Call<DoctorProfile> call, Throwable t) {
                        showLoading(false);
                    }
                });
    }

    private void bindProfile(DoctorProfile profile) {
        if (profile.getBio() != null) {
            inputBio.setText(profile.getBio());
        }
        if (profile.getYearsOfExperience() != null) {
            inputYearsExperience.setText(String.valueOf(profile.getYearsOfExperience()));
        }
        if (profile.getClinicAddress() != null) {
            inputClinicAddress.setText(profile.getClinicAddress());
        }
        if (profile.getCity() != null) {
            inputCity.setText(profile.getCity());
        }
        if (profile.getCountry() != null) {
            inputCountry.setText(profile.getCountry());
        }
        if (profile.getMedicalRegistrationNumber() != null) {
            inputMedicalReg.setText(profile.getMedicalRegistrationNumber());
        }
        if (profile.getConsultationFee() != null) {
            inputConsultationFee.setText(profile.getConsultationFee().toPlainString());
        }
        if (profile.getMaxDailyAppointments() != null) {
            inputMaxDaily.setText(String.valueOf(profile.getMaxDailyAppointments()));
        }
        if (profile.getAverageConsultationDurationMinutes() != null) {
            inputAvgDuration.setText(String.valueOf(profile.getAverageConsultationDurationMinutes()));
        }
        if (profile.getAcceptsNewPatients() != null) {
            checkAcceptsNew.setChecked(profile.getAcceptsNewPatients());
        }
        if (profile.getTeleconsultationEnabled() != null) {
            checkTeleconsultation.setChecked(profile.getTeleconsultationEnabled());
        }
    }

    private void submitProfile() {
        if (authHeader == null) {
            goToLoginFallback();
            return;
        }

        DoctorApiService.DoctorProfileUpdateRequestDto request =
                new DoctorApiService.DoctorProfileUpdateRequestDto();

        request.setBio(textOf(inputBio));
        request.setYearsOfExperience(parseIntegerOrNull(textOf(inputYearsExperience)));
        request.setClinicAddress(textOf(inputClinicAddress));
        request.setCity(textOf(inputCity));
        request.setCountry(textOf(inputCountry));
        request.setMedicalRegistrationNumber(textOf(inputMedicalReg));
        request.setConsultationFee(parseBigDecimalOrNull(textOf(inputConsultationFee)));
        request.setMaxDailyAppointments(parseIntegerOrNull(textOf(inputMaxDaily)));
        request.setAverageConsultationDurationMinutes(parseIntegerOrNull(textOf(inputAvgDuration)));
        request.setAcceptsNewPatients(checkAcceptsNew.isChecked());
        request.setTeleconsultationEnabled(checkTeleconsultation.isChecked());

        showLoading(true);

        doctorApiService.updateMyProfile(authHeader, request)
                .enqueue(new Callback<DoctorProfile>() {
                    @Override
                    public void onResponse(Call<DoctorProfile> call, Response<DoctorProfile> response) {
                        showLoading(false);
                        if (!response.isSuccessful()) {
                            Snackbar.make(rootView,
                                    getString(R.string.doctor_onboarding_profile_error_generic),
                                    Snackbar.LENGTH_LONG).show();
                            return;
                        }

                        Snackbar.make(rootView,
                                getString(R.string.doctor_onboarding_profile_success),
                                Snackbar.LENGTH_LONG).show();
                        goToMain();
                    }

                    @Override
                    public void onFailure(Call<DoctorProfile> call, Throwable t) {
                        showLoading(false);
                        Snackbar.make(rootView,
                                getString(R.string.doctor_onboarding_profile_error_network),
                                Snackbar.LENGTH_LONG).show();
                    }
                });
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

    private BigDecimal parseBigDecimalOrNull(String value) {
        if (TextUtils.isEmpty(value)) return null;
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private void showLoading(boolean show) {
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        buttonSave.setEnabled(!show);
        buttonSkip.setEnabled(!show);
        inputBio.setEnabled(!show);
        inputYearsExperience.setEnabled(!show);
        inputClinicAddress.setEnabled(!show);
        inputCity.setEnabled(!show);
        inputCountry.setEnabled(!show);
        inputMedicalReg.setEnabled(!show);
        inputConsultationFee.setEnabled(!show);
        inputMaxDaily.setEnabled(!show);
        inputAvgDuration.setEnabled(!show);
        checkAcceptsNew.setEnabled(!show);
        checkTeleconsultation.setEnabled(!show);
    }

    private void goToMain() {
        Intent intent = new Intent(DoctorProfileOnboardingActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void goToLoginFallback() {
        Intent intent = new Intent(
                DoctorProfileOnboardingActivity.this,
                AuthGateActivity.class
        );
        startActivity(intent);
        finish();
    }
}
