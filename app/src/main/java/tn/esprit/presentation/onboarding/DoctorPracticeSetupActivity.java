package tn.esprit.presentation.onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import tn.esprit.R;
import tn.esprit.data.auth.AuthLocalDataSource;
import tn.esprit.data.remote.ApiClient;
import tn.esprit.data.remote.doctor.DoctorApiService;
import tn.esprit.data.remote.specialty.SpecialtyApiService;
import tn.esprit.domain.auth.AuthTokens;

public class DoctorPracticeSetupActivity extends AppCompatActivity {

    private static final String TAG = "DoctorPracticeSetup";

    private View rootView;
    private View loadingOverlay;

    private MaterialAutoCompleteTextView inputSpecialty;
    private ChipGroup chipGroupActs;
    private TextView textActsEmptyState;
    private MaterialButton buttonSaveContinue;

    private AuthLocalDataSource authLocalDataSource;
    private SpecialtyApiService specialtyApiService;
    private DoctorApiService doctorApiService;

    private List<SpecialtyApiService.SpecialtyDto> specialties = new ArrayList<>();
    private List<SpecialtyApiService.ActDto> actsForSelectedSpecialty = new ArrayList<>();
    private SpecialtyApiService.SpecialtyDto selectedSpecialty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_doctor_practice_setup);

        bindViews();
        applyWindowInsets();
        initServices();
        setupListeners();
        loadSpecialties();
    }

    private void bindViews() {
        rootView = findViewById(R.id.doctor_setup_root);
        loadingOverlay = findViewById(R.id.doctor_setup_loading_overlay);

        inputSpecialty = findViewById(R.id.input_specialty);
        chipGroupActs = findViewById(R.id.chip_group_acts);
        textActsEmptyState = findViewById(R.id.text_acts_empty);
        buttonSaveContinue = findViewById(R.id.button_save_continue);

        // required step → disabled until valid selection
        buttonSaveContinue.setEnabled(false);
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

    private void initServices() {
        authLocalDataSource = new AuthLocalDataSource(getApplicationContext());
        specialtyApiService = ApiClient.createService(SpecialtyApiService.class);
        doctorApiService = ApiClient.createService(DoctorApiService.class);
    }

    private void setupListeners() {
        inputSpecialty.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < specialties.size()) {
                    selectedSpecialty = specialties.get(position);
                    Log.d(TAG, "Selected specialty: id=" + selectedSpecialty.id + ", name=" + selectedSpecialty.name);
                    loadActsForSpecialty(selectedSpecialty.id);
                }
            }
        });

        // ensure dropdown opens when tapping field
        inputSpecialty.setOnClickListener(v -> {
            if (inputSpecialty.getAdapter() != null && inputSpecialty.getAdapter().getCount() > 0) {
                inputSpecialty.showDropDown();
            }
        });

        buttonSaveContinue.setOnClickListener(v -> submitPracticeSetup());
    }

    // ----------------- Network helpers -------------------

    private String getAuthHeaderOrFallback() {
        AuthTokens tokens = authLocalDataSource.getTokens();
        if (tokens == null || TextUtils.isEmpty(tokens.getAccessToken())) {
            Intent intent = new Intent(this, tn.esprit.presentation.auth.AuthGateActivity.class);
            startActivity(intent);
            finish();
            return null;
        }
        String type = tokens.getTokenType() != null ? tokens.getTokenType() : "Bearer";
        return type + " " + tokens.getAccessToken();
    }

    private void loadSpecialties() {
        String authHeader = getAuthHeaderOrFallback();
        if (authHeader == null) return;

        showLoading(true);

        specialtyApiService.getAllSpecialties(authHeader)
                .enqueue(new Callback<List<SpecialtyApiService.SpecialtyDto>>() {
                    @Override
                    public void onResponse(
                            Call<List<SpecialtyApiService.SpecialtyDto>> call,
                            Response<List<SpecialtyApiService.SpecialtyDto>> response) {

                        showLoading(false);

                        if (!response.isSuccessful() || response.body() == null) {
                            Log.w(TAG, "getAllSpecialties not successful. code=" + response.code());
                            Snackbar.make(rootView,
                                    "Could not load specialties. Please try again.",
                                    Snackbar.LENGTH_LONG).show();
                            return;
                        }

                        specialties = response.body();
                        Log.d(TAG, "Specialties loaded, count=" + specialties.size());
                        bindSpecialtiesToDropdown();
                    }

                    @Override
                    public void onFailure(
                            Call<List<SpecialtyApiService.SpecialtyDto>> call,
                            Throwable t) {

                        showLoading(false);
                        Log.e(TAG, "getAllSpecialties failed", t);
                        Snackbar.make(rootView,
                                "Network error while loading specialties.",
                                Snackbar.LENGTH_LONG).show();
                    }
                });
    }

    private void bindSpecialtiesToDropdown() {
        if (specialties == null) {
            specialties = new ArrayList<>();
        }

        List<String> names = new ArrayList<>();
        for (SpecialtyApiService.SpecialtyDto s : specialties) {
            String label;
            if (!TextUtils.isEmpty(s.name)) {
                label = s.name;
            } else if (!TextUtils.isEmpty(s.code)) {
                label = s.code;
            } else {
                label = "Specialty #" + s.id;
            }
            names.add(label);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                names
        );
        inputSpecialty.setAdapter(adapter);

        if (selectedSpecialty != null) {
            for (int i = 0; i < specialties.size(); i++) {
                if (specialties.get(i).id != null
                        && specialties.get(i).id.equals(selectedSpecialty.id)) {
                    inputSpecialty.setText(names.get(i), false);
                    break;
                }
            }
        }

        updateSaveButtonEnabledState();
    }

    private void loadActsForSpecialty(Long specialtyId) {
        String authHeader = getAuthHeaderOrFallback();
        if (authHeader == null) return;

        showLoading(true);
        chipGroupActs.removeAllViews();
        textActsEmptyState.setVisibility(View.GONE);

        specialtyApiService.getActsBySpecialty(authHeader, specialtyId)
                .enqueue(new Callback<List<SpecialtyApiService.ActDto>>() {
                    @Override
                    public void onResponse(
                            Call<List<SpecialtyApiService.ActDto>> call,
                            Response<List<SpecialtyApiService.ActDto>> response) {

                        showLoading(false);

                        if (!response.isSuccessful() || response.body() == null) {
                            Log.w(TAG, "getActsBySpecialty not successful. code=" + response.code());

                            String message = "Could not load acts for this specialty.";
                            if (response.code() == 403 && response.errorBody() != null) {
                                try {
                                    String raw = response.errorBody().string();
                                    if (raw.contains("Doctor is not associated with this specialty")) {
                                        message = "Doctor is not associated with this specialty (backend rule).";
                                    }
                                } catch (Exception ignored) {
                                }
                            }

                            textActsEmptyState.setText(message);
                            textActsEmptyState.setVisibility(View.VISIBLE);
                            actsForSelectedSpecialty = new ArrayList<>();
                            updateSaveButtonEnabledState();
                            return;
                        }

                        actsForSelectedSpecialty = response.body();
                        Log.d(TAG, "Acts loaded for specialty " + specialtyId +
                                ", count=" + actsForSelectedSpecialty.size());
                        bindActsToChips();
                    }

                    @Override
                    public void onFailure(
                            Call<List<SpecialtyApiService.ActDto>> call,
                            Throwable t) {

                        showLoading(false);
                        Log.e(TAG, "getActsBySpecialty failed", t);
                        textActsEmptyState.setText("Network error while loading acts.");
                        textActsEmptyState.setVisibility(View.VISIBLE);
                        actsForSelectedSpecialty = new ArrayList<>();
                        updateSaveButtonEnabledState();
                    }
                });
    }

    private void bindActsToChips() {
        chipGroupActs.removeAllViews();

        if (actsForSelectedSpecialty == null || actsForSelectedSpecialty.isEmpty()) {
            textActsEmptyState.setText("No acts available for this specialty.");
            textActsEmptyState.setVisibility(View.VISIBLE);
            updateSaveButtonEnabledState();
            return;
        }

        textActsEmptyState.setVisibility(View.GONE);

        for (SpecialtyApiService.ActDto act : actsForSelectedSpecialty) {
            Chip chip = new Chip(this);
            chip.setText(!TextUtils.isEmpty(act.name) ? act.name : ("Act #" + act.id));
            chip.setCheckable(true);
            chip.setClickable(true);
            chip.setTag(act.id);

            chip.setChipStrokeWidth(1f);
            chip.setChipStrokeColorResource(R.color.color_primary_soft);
            chip.setTextColor(getColor(R.color.color_on_background_secondary));

            chip.setOnCheckedChangeListener((buttonView, isChecked) -> updateSaveButtonEnabledState());

            chipGroupActs.addView(chip);
        }

        updateSaveButtonEnabledState();
    }

    private void submitPracticeSetup() {
        if (selectedSpecialty == null) {
            Snackbar.make(rootView,
                    "Please choose your main specialty.",
                    Snackbar.LENGTH_LONG).show();
            return;
        }

        List<Long> selectedActIds = collectSelectedActIds();
        if (selectedActIds.isEmpty()) {
            Snackbar.make(rootView,
                    "Please select at least one act you perform.",
                    Snackbar.LENGTH_LONG).show();
            return;
        }

        String authHeader = getAuthHeaderOrFallback();
        if (authHeader == null) return;

        DoctorApiService.DoctorPracticeSetupRequestDto request =
                new DoctorApiService.DoctorPracticeSetupRequestDto();
        request.specialtyId = selectedSpecialty.id;
        request.actIds = selectedActIds;

        showLoading(true);

        doctorApiService.setupPracticeForCurrentDoctor(authHeader, request)
                .enqueue(new Callback<tn.esprit.domain.doctor.DoctorProfile>() {
                    @Override
                    public void onResponse(
                            Call<tn.esprit.domain.doctor.DoctorProfile> call,
                            Response<tn.esprit.domain.doctor.DoctorProfile> response) {

                        showLoading(false);

                        if (!response.isSuccessful()) {
                            Snackbar.make(rootView,
                                    "Could not save your practice setup. Please try again.",
                                    Snackbar.LENGTH_LONG).show();
                            return;
                        }

                        Snackbar.make(rootView,
                                "Your practice has been configured.",
                                Snackbar.LENGTH_LONG).show();

                        goToDoctorProfileOnboarding();
                    }

                    @Override
                    public void onFailure(
                            Call<tn.esprit.domain.doctor.DoctorProfile> call,
                            Throwable t) {

                        showLoading(false);
                        Snackbar.make(rootView,
                                "Network error while saving your practice setup.",
                                Snackbar.LENGTH_LONG).show();
                    }
                });
    }

    // --------- Helpers ---------

    private List<Long> collectSelectedActIds() {
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < chipGroupActs.getChildCount(); i++) {
            View child = chipGroupActs.getChildAt(i);
            if (child instanceof Chip) {
                Chip chip = (Chip) child;
                if (chip.isChecked()) {
                    Object tag = chip.getTag();
                    if (tag instanceof Long) {
                        ids.add((Long) tag);
                    } else if (tag instanceof Integer) {
                        ids.add(((Integer) tag).longValue());
                    }
                }
            }
        }
        return ids;
    }

    private boolean hasAnySelectedAct() {
        for (int i = 0; i < chipGroupActs.getChildCount(); i++) {
            View child = chipGroupActs.getChildAt(i);
            if (child instanceof Chip && ((Chip) child).isChecked()) {
                return true;
            }
        }
        return false;
    }

    private void updateSaveButtonEnabledState() {
        boolean enable = selectedSpecialty != null && hasAnySelectedAct();
        buttonSaveContinue.setEnabled(enable);
    }

    private void showLoading(boolean show) {
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (show) {
            inputSpecialty.setEnabled(false);
            buttonSaveContinue.setEnabled(false);
        } else {
            inputSpecialty.setEnabled(true);
            updateSaveButtonEnabledState();
        }
    }

    private void goToDoctorProfileOnboarding() {
        Intent intent = new Intent(
                DoctorPracticeSetupActivity.this,
                DoctorProfileOnboardingActivity.class
        );
        startActivity(intent);
        finish();
    }
}
