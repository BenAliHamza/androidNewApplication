package tn.esprit.presentation.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import tn.esprit.R;
import tn.esprit.domain.indicator.IndicatorType;
import tn.esprit.domain.indicator.PatientIndicator;

/**
 * "My indicators" screen for the patient.
 *
 * Shows:
 *  - dropdown to filter by indicator type (All / Blood pressure / Heart rate / ...)
 *  - list of indicators
 *  - empty state text
 *  - basic loading state
 *
 * "Add indicator" opens a dialog and calls POST /indicators/me via ViewModel.
 * Swipe left/right on an item to delete it (with confirmation).
 */
public class PatientIndicatorsFragment extends Fragment {

    private PatientIndicatorsViewModel viewModel;

    private Spinner spinnerType;
    private RecyclerView recyclerIndicators;
    private ProgressBar progressBar;
    private TextView textEmpty;
    private Button buttonAdd;

    private PatientIndicatorsAdapter adapter;

    // Local cache of types for the spinner
    private final List<IndicatorType> currentTypes = new ArrayList<>();

    public PatientIndicatorsFragment() {
        // Required empty public constructor
    }

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

        spinnerType = view.findViewById(R.id.spinner_indicator_type);
        recyclerIndicators = view.findViewById(R.id.recycler_patient_indicators);
        progressBar = view.findViewById(R.id.patient_indicators_progress);
        textEmpty = view.findViewById(R.id.text_patient_indicators_empty);
        buttonAdd = view.findViewById(R.id.button_add_indicator);

        // Recycler & adapter
        adapter = new PatientIndicatorsAdapter();
        if (recyclerIndicators != null) {
            recyclerIndicators.setLayoutManager(new LinearLayoutManager(requireContext()));
            recyclerIndicators.setAdapter(adapter);
        }

        // ViewModel
        viewModel = new ViewModelProvider(this).get(PatientIndicatorsViewModel.class);

        setupSpinner();
        setupObservers();
        setupActions();
        attachSwipeToDelete();

        // Initial loads
        viewModel.loadIndicatorTypes();
        viewModel.loadIndicators();
    }

    private void setupSpinner() {
        if (spinnerType == null) return;

        // Start with just "All"
        List<String> labels = new ArrayList<>();
        labels.add(getString(R.string.patient_indicators_filter_all));

        ArrayAdapter<String> spinnerAdapter =
                new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_spinner_item,
                        labels);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(spinnerAdapter);

        spinnerType.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent,
                                       View view,
                                       int position,
                                       long id) {
                // position 0 = "All"
                if (position == 0) {
                    viewModel.loadIndicators();
                    return;
                }

                // position > 0 => currentTypes index is position-1
                int typeIndex = position - 1;
                if (typeIndex >= 0 && typeIndex < currentTypes.size()) {
                    IndicatorType type = currentTypes.get(typeIndex);
                    viewModel.loadIndicatorsForType(type.getId());
                } else {
                    viewModel.loadIndicators();
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                // Do nothing; keep previous selection
            }
        });
    }

    private void setupObservers() {
        viewModel.getIndicators().observe(getViewLifecycleOwner(), this::bindIndicators);
        viewModel.getLoading().observe(getViewLifecycleOwner(), this::bindLoading);
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), this::bindError);
        viewModel.getIndicatorTypes().observe(getViewLifecycleOwner(), this::bindIndicatorTypes);
        viewModel.getLastAddSuccess().observe(getViewLifecycleOwner(), success -> {
            if (!Boolean.TRUE.equals(success)) return;
            if (!isAdded()) return;
            Toast.makeText(
                    requireContext(),
                    getString(R.string.patient_indicators_add_success),
                    Toast.LENGTH_SHORT
            ).show();
            viewModel.clearLastAddSuccess();
        });
    }

    private void setupActions() {
        if (buttonAdd != null) {
            buttonAdd.setOnClickListener(v -> openAddIndicatorDialog());
        }
    }

    private void attachSwipeToDelete() {
        if (recyclerIndicators == null) return;

        ItemTouchHelper.SimpleCallback callback =
                new ItemTouchHelper.SimpleCallback(0,
                        ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

                    @Override
                    public boolean onMove(@NonNull RecyclerView recyclerView,
                                          @NonNull RecyclerView.ViewHolder viewHolder,
                                          @NonNull RecyclerView.ViewHolder target) {
                        // We don't support drag & drop reordering
                        return false;
                    }

                    @Override
                    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder,
                                         int direction) {
                        if (!isAdded()) {
                            return;
                        }

                        int position = viewHolder.getBindingAdapterPosition();
                        if (position == RecyclerView.NO_POSITION) {
                            adapter.notifyDataSetChanged();
                            return;
                        }

                        List<PatientIndicator> currentList = viewModel.getIndicators().getValue();
                        if (currentList == null || position < 0 || position >= currentList.size()) {
                            adapter.notifyItemChanged(position);
                            return;
                        }

                        PatientIndicator indicator = currentList.get(position);
                        Long id = indicator.getId();
                        if (id == null) {
                            Toast.makeText(
                                    requireContext(),
                                    getString(R.string.patient_indicators_delete_error_missing_id),
                                    Toast.LENGTH_SHORT
                            ).show();
                            adapter.notifyItemChanged(position);
                            return;
                        }

                        // Track whether user confirmed delete
                        final boolean[] deleteConfirmed = {false};

                        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                                .setTitle(R.string.patient_indicators_delete_confirm_title)
                                .setMessage(R.string.patient_indicators_delete_confirm_message)
                                .setPositiveButton(android.R.string.ok, (d, which) -> {
                                    deleteConfirmed[0] = true;
                                    viewModel.deleteIndicator(id);
                                })
                                .setNegativeButton(android.R.string.cancel, (d, which) -> {
                                    // just dismiss; onDismiss will restore the item
                                })
                                .setOnDismissListener(d -> {
                                    // If user cancelled or dismissed dialog (back press),
                                    // restore the swiped item visually.
                                    if (!deleteConfirmed[0]) {
                                        adapter.notifyItemChanged(position);
                                    }
                                })
                                .create();

                        dialog.show();
                    }
                };

        new ItemTouchHelper(callback).attachToRecyclerView(recyclerIndicators);
    }

    // ---------------------------------------------------------------------
    // Bindings
    // ---------------------------------------------------------------------

    private void bindIndicators(@Nullable List<PatientIndicator> list) {
        List<PatientIndicator> safe = (list != null) ? list : new ArrayList<>();
        adapter.submitList(safe);

        if (textEmpty != null) {
            if (safe.isEmpty()) {
                textEmpty.setVisibility(View.VISIBLE);
            } else {
                textEmpty.setVisibility(View.GONE);
            }
        }
    }

    private void bindLoading(@Nullable Boolean isLoading) {
        boolean show = Boolean.TRUE.equals(isLoading);
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void bindError(@Nullable String error) {
        if (error == null || error.trim().isEmpty()) return;
        if (!isAdded()) return;
        Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
    }

    private void bindIndicatorTypes(@Nullable List<IndicatorType> types) {
        currentTypes.clear();
        if (types != null) {
            currentTypes.addAll(types);
        }

        if (spinnerType == null || !isAdded()) {
            return;
        }

        // Build labels: "All" + each type name
        List<String> labels = new ArrayList<>();
        labels.add(getString(R.string.patient_indicators_filter_all));
        for (IndicatorType type : currentTypes) {
            String name = type.getName();
            if (name == null || name.trim().isEmpty()) {
                name = type.getCode() != null ? type.getCode() : "";
            }
            labels.add(name);
        }

        ArrayAdapter<String> spinnerAdapter =
                new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_spinner_item,
                        labels);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(spinnerAdapter);
    }

    // ---------------------------------------------------------------------
    // Add indicator dialog
    // ---------------------------------------------------------------------

    private void openAddIndicatorDialog() {
        if (!isAdded()) return;

        if (currentTypes.isEmpty()) {
            Toast.makeText(
                    requireContext(),
                    getString(R.string.patient_indicators_types_not_loaded),
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View dialogView = inflater.inflate(R.layout.dialog_add_indicator, null);

        Spinner spinnerDialogType = dialogView.findViewById(R.id.spinner_dialog_indicator_type);
        EditText inputValue = dialogView.findViewById(R.id.input_indicator_value);
        EditText inputNote = dialogView.findViewById(R.id.input_indicator_note);

        // Build type labels for dialog (no "All" here)
        List<String> typeLabels = new ArrayList<>();
        for (IndicatorType type : currentTypes) {
            String name = type.getName();
            if (name == null || name.trim().isEmpty()) {
                name = type.getCode() != null ? type.getCode() : "";
            }
            typeLabels.add(name);
        }

        ArrayAdapter<String> typeAdapter =
                new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_spinner_item,
                        typeLabels);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDialogType.setAdapter(typeAdapter);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.patient_indicators_add_dialog_title)
                .setView(dialogView)
                .setPositiveButton(R.string.patient_indicators_add_dialog_save, null)
                .setNegativeButton(android.R.string.cancel, (d, which) -> d.dismiss())
                .create();

        dialog.setOnShowListener(dlg -> {
            Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positive.setOnClickListener(v -> {
                // Validate selection
                int selectedPosition = spinnerDialogType.getSelectedItemPosition();
                if (selectedPosition < 0 || selectedPosition >= currentTypes.size()) {
                    Toast.makeText(
                            requireContext(),
                            getString(R.string.patient_indicators_types_not_loaded),
                            Toast.LENGTH_SHORT
                    ).show();
                    return;
                }
                IndicatorType selectedType = currentTypes.get(selectedPosition);
                Long indicatorTypeId = selectedType.getId();
                if (indicatorTypeId == null) {
                    Toast.makeText(
                            requireContext(),
                            getString(R.string.patient_indicators_types_not_loaded),
                            Toast.LENGTH_SHORT
                    ).show();
                    return;
                }

                // Validate numeric value
                String valueStr = inputValue.getText() != null
                        ? inputValue.getText().toString().trim()
                        : "";
                if (valueStr.isEmpty()) {
                    inputValue.setError(getString(R.string.patient_indicators_add_value_required));
                    return;
                }

                BigDecimal numericValue;
                try {
                    numericValue = new BigDecimal(valueStr);
                } catch (NumberFormatException e) {
                    inputValue.setError(getString(R.string.patient_indicators_add_value_invalid));
                    return;
                }

                String note = inputNote.getText() != null
                        ? inputNote.getText().toString().trim()
                        : null;
                if (note != null && note.isEmpty()) {
                    note = null;
                }

                // For now we only use numericValue; textValue is null.
                viewModel.addIndicator(indicatorTypeId, numericValue, null, note);

                dialog.dismiss();
            });
        });

        dialog.show();
    }
}
