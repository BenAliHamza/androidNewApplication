package tn.esprit.presentation.home;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import tn.esprit.R;
import tn.esprit.data.auth.AuthLocalDataSource;
import tn.esprit.data.indicator.PatientIndicatorRepository;
import tn.esprit.data.remote.indicator.IndicatorApiService.PatientIndicatorCreateRequestDto;
import tn.esprit.domain.auth.AuthTokens;
import tn.esprit.domain.indicator.IndicatorType;
import tn.esprit.domain.indicator.PatientIndicator;
import tn.esprit.presentation.indicator.PatientIndicatorAdapter;

/**
 * Patient self "My indicators" screen.
 *
 * Features:
 *  - Loads indicator types and lets the user filter their own indicators.
 *  - Lists current patient's indicators (/indicators/me).
 *  - Allows adding a new indicator via dialog_add_indicator.xml.
 *  - Read-only list for now (no deletion yet).
 */
public class PatientIndicatorsFragment extends Fragment {

    private Spinner spinnerIndicatorType;
    private ProgressBar progressBar;
    private TextView textEmpty;
    private RecyclerView recyclerView;
    private Button buttonAddIndicator;

    private PatientIndicatorAdapter indicatorAdapter;
    private PatientIndicatorRepository indicatorRepository;
    private AuthLocalDataSource authLocalDataSource;

    private final List<IndicatorType> indicatorTypes = new ArrayList<>();
    private boolean typesLoaded = false;
    private Long selectedIndicatorTypeId = null;
    private boolean initialFilterApplied = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_patient_indicators, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        spinnerIndicatorType = view.findViewById(R.id.spinner_indicator_type);
        progressBar = view.findViewById(R.id.patient_indicators_progress);
        textEmpty = view.findViewById(R.id.text_patient_indicators_empty);
        recyclerView = view.findViewById(R.id.recycler_patient_indicators);
        buttonAddIndicator = view.findViewById(R.id.button_add_indicator);

        indicatorRepository = new PatientIndicatorRepository(requireContext());
        authLocalDataSource = new AuthLocalDataSource(requireContext().getApplicationContext());

        indicatorAdapter = new PatientIndicatorAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(indicatorAdapter);

        setupTypeSpinner();
        setupAddButton();

        // 1) Load types (for spinner + add dialog)
        loadIndicatorTypes();
        // 2) Load indicators with default "All" filter (once types are known or immediately)
        loadIndicators();
    }

    // ---------------------------------------------------------------------
    // Types spinner
    // ---------------------------------------------------------------------

    private void setupTypeSpinner() {
        // Temporary adapter until real types are loaded
        List<String> labels = new ArrayList<>();
        labels.add(getString(R.string.patient_indicators_filter_all));
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                labels
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerIndicatorType.setAdapter(adapter);

        spinnerIndicatorType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent,
                                       View view,
                                       int position,
                                       long id) {
                // Position 0 → "All"
                if (position == 0) {
                    selectedIndicatorTypeId = null;
                } else {
                    int typeIndex = position - 1;
                    if (typeIndex >= 0 && typeIndex < indicatorTypes.size()) {
                        selectedIndicatorTypeId = indicatorTypes.get(typeIndex).getId();
                    } else {
                        selectedIndicatorTypeId = null;
                    }
                }

                // Avoid double-loading on initial setup if we want, but in practice it's fine.
                if (typesLoaded || initialFilterApplied) {
                    loadIndicators();
                }
                initialFilterApplied = true;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Keep current filter
            }
        });
    }

    private void bindTypesToSpinner(@NonNull List<IndicatorType> types) {
        indicatorTypes.clear();
        indicatorTypes.addAll(types);
        typesLoaded = true;

        List<String> labels = new ArrayList<>();
        // First "All"
        labels.add(getString(R.string.patient_indicators_filter_all));

        for (IndicatorType type : indicatorTypes) {
            String name = type.getName() != null ? type.getName().trim() : "";
            String unit = type.getUnit() != null ? type.getUnit().trim() : "";
            if (!TextUtils.isEmpty(unit)) {
                labels.add(name + " (" + unit + ")");
            } else {
                labels.add(name);
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                labels
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerIndicatorType.setAdapter(adapter);
        // Keep selection at "All" by default
        spinnerIndicatorType.setSelection(0);
    }

    private void loadIndicatorTypes() {
        indicatorRepository.getIndicatorTypes(new PatientIndicatorRepository.IndicatorTypesCallback() {
            @Override
            public void onSuccess(List<IndicatorType> types) {
                if (!isAdded()) return;
                bindTypesToSpinner(types);
            }

            @Override
            public void onError(@Nullable Throwable throwable,
                                @Nullable Integer httpCode,
                                @Nullable String errorBody) {
                if (!isAdded()) return;
                // Soft failure: user can still see indicators without type filtering.
                Toast.makeText(
                        requireContext(),
                        getString(R.string.patient_indicators_types_not_loaded),
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    // ---------------------------------------------------------------------
    // Indicators list for current patient
    // ---------------------------------------------------------------------

    private void loadIndicators() {
        String authHeader = buildAuthHeaderIfAvailable();
        showLoading(true);
        showEmpty(false);

        indicatorRepository.getMyIndicators(
                authHeader,
                selectedIndicatorTypeId,
                null,
                null,
                new PatientIndicatorRepository.IndicatorsCallback() {
                    @Override
                    public void onSuccess(List<PatientIndicator> indicators) {
                        if (!isAdded()) return;

                        showLoading(false);
                        indicatorAdapter.submitList(indicators);

                        if (indicators == null || indicators.isEmpty()) {
                            showEmpty(true);
                        } else {
                            showEmpty(false);
                        }
                    }

                    @Override
                    public void onError(@Nullable Throwable throwable,
                                        @Nullable Integer httpCode,
                                        @Nullable String errorBody) {
                        if (!isAdded()) return;

                        showLoading(false);
                        textEmpty.setText(R.string.patient_indicators_error_generic);
                        showEmpty(true);
                    }
                }
        );
    }

    private void showLoading(boolean loading) {
        if (progressBar != null) {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        if (recyclerView != null) {
            recyclerView.setVisibility(loading ? View.GONE : View.VISIBLE);
        }
    }

    private void showEmpty(boolean empty) {
        if (textEmpty != null) {
            textEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        }
    }

    // ---------------------------------------------------------------------
    // Add indicator dialog
    // ---------------------------------------------------------------------

    private void setupAddButton() {
        buttonAddIndicator.setOnClickListener(v -> {
            if (!typesLoaded || indicatorTypes.isEmpty()) {
                Toast.makeText(
                        requireContext(),
                        getString(R.string.patient_indicators_types_not_loaded),
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }
            showAddIndicatorDialog();
        });
    }

    private void showAddIndicatorDialog() {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View dialogView = inflater.inflate(R.layout.dialog_add_indicator, null, false);

        Spinner spinnerDialogType = dialogView.findViewById(R.id.spinner_dialog_indicator_type);
        EditText inputValue = dialogView.findViewById(R.id.input_indicator_value);
        EditText inputNote = dialogView.findViewById(R.id.input_indicator_note);
        TextView labelValue = dialogView.findViewById(R.id.text_indicator_value_label);

        // Build type labels (no "All" here)
        List<String> typeLabels = new ArrayList<>();
        for (IndicatorType type : indicatorTypes) {
            String name = type.getName() != null ? type.getName().trim() : "";
            String unit = type.getUnit() != null ? type.getUnit().trim() : "";
            if (!TextUtils.isEmpty(unit)) {
                typeLabels.add(name + " (" + unit + ")");
            } else {
                typeLabels.add(name);
            }
        }

        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                typeLabels
        );
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDialogType.setAdapter(typeAdapter);

        // Optionally adapt hint to selected type's unit
        spinnerDialogType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent,
                                       View view,
                                       int position,
                                       long id) {
                IndicatorType type = getDialogSelectedType(position);
                if (type == null) return;

                String unit = type.getUnit();
                if (unit != null && !unit.trim().isEmpty()) {
                    String hint = getString(R.string.patient_indicators_add_value_hint_with_unit, unit.trim());
                    inputValue.setHint(hint);
                } else {
                    inputValue.setHint(R.string.patient_indicators_add_value_hint_generic);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // no-op
            }
        });

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.patient_indicators_add_dialog_title)
                .setView(dialogView)
                .setPositiveButton(R.string.patient_indicators_add_dialog_save, null)
                .setNegativeButton(android.R.string.cancel, (d, which) -> d.dismiss())
                .create();

        dialog.setOnShowListener(d -> {
            Button saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            saveButton.setOnClickListener(v -> {
                int selectedPos = spinnerDialogType.getSelectedItemPosition();
                IndicatorType selectedType = getDialogSelectedType(selectedPos);
                if (selectedType == null || selectedType.getId() == null) {
                    Toast.makeText(
                            requireContext(),
                            getString(R.string.patient_indicators_types_not_loaded),
                            Toast.LENGTH_SHORT
                    ).show();
                    return;
                }

                String valueText = inputValue.getText() != null
                        ? inputValue.getText().toString().trim()
                        : "";

                if (valueText.isEmpty()) {
                    inputValue.setError(getString(R.string.patient_indicators_add_value_required));
                    return;
                }

                BigDecimal numericValue;
                try {
                    numericValue = new BigDecimal(valueText);
                } catch (NumberFormatException e) {
                    inputValue.setError(getString(R.string.patient_indicators_add_value_invalid));
                    return;
                }

                String note = inputNote.getText() != null
                        ? inputNote.getText().toString().trim()
                        : null;

                PatientIndicatorCreateRequestDto request = new PatientIndicatorCreateRequestDto();
                request.setIndicatorTypeId(selectedType.getId());
                request.setNumericValue(numericValue);
                request.setTextValue(null);
                request.setMeasuredAt(null); // let backend use now()
                request.setNote(note);

                String authHeader = buildAuthHeaderIfAvailable();

                indicatorRepository.addMyIndicator(
                        authHeader,
                        request,
                        new PatientIndicatorRepository.AddIndicatorCallback() {
                            @Override
                            public void onSuccess(PatientIndicator created) {
                                if (!isAdded()) return;

                                Toast.makeText(
                                        requireContext(),
                                        getString(R.string.patient_indicators_add_success),
                                        Toast.LENGTH_SHORT
                                ).show();

                                dialog.dismiss();
                                // Reload indicators with current filter
                                loadIndicators();
                            }

                            @Override
                            public void onError(@Nullable Throwable throwable,
                                                @Nullable Integer httpCode,
                                                @Nullable String errorBody) {
                                if (!isAdded()) return;

                                Toast.makeText(
                                        requireContext(),
                                        getString(R.string.patient_indicators_error_generic),
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        }
                );
            });
        });

        dialog.show();
    }

    @Nullable
    private IndicatorType getDialogSelectedType(int position) {
        if (position < 0 || position >= indicatorTypes.size()) return null;
        return indicatorTypes.get(position);
    }

    // ---------------------------------------------------------------------
    // Auth header helper
    // ---------------------------------------------------------------------

    @Nullable
    private String buildAuthHeaderIfAvailable() {
        AuthTokens tokens = authLocalDataSource.getTokens();
        if (tokens == null || tokens.getAccessToken() == null) {
            return null;
        }
        return "Bearer " + tokens.getAccessToken();
    }
}
