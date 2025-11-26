package tn.esprit.presentation.medication;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import tn.esprit.R;
import tn.esprit.data.medication.PrescriptionRepository;
import tn.esprit.domain.medication.Prescription;
import tn.esprit.domain.medication.PrescriptionLine;

/**
 * Patient view: list of their own medications (flattened prescription lines),
 * with reminder toggles.
 *
 * Navigation:
 *  - This fragment is used in nav_main.xml for patient home bottom navigation.
 */
public class PatientMedicationsFragment extends Fragment
        implements PatientMedicationAdapter.OnMedicationInteractionListener {

    private ProgressBar progressBar;
    private TextView textEmpty;
    private RecyclerView recyclerView;
    private PatientMedicationAdapter adapter;
    private PrescriptionRepository prescriptionRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_patient_medications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        progressBar = view.findViewById(R.id.patient_medications_progress);
        textEmpty = view.findViewById(R.id.text_patient_medications_empty);
        recyclerView = view.findViewById(R.id.recycler_patient_medications);

        prescriptionRepository = new PrescriptionRepository(requireContext());

        adapter = new PatientMedicationAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadMyMedications();
    }

    // ---------------------------------------------------------------------
    // Load patient's medications
    // ---------------------------------------------------------------------

    private void loadMyMedications() {
        showLoading(true);

        // Patient endpoint: GET /api/prescriptions/me?activeOnly=true
        prescriptionRepository.getMyPrescriptions(
                true,
                new PrescriptionRepository.PrescriptionsCallback() {
                    @Override
                    public void onSuccess(List<Prescription> prescriptions) {
                        if (!isAdded()) return;

                        showLoading(false);

                        List<PrescriptionLine> allLines = new ArrayList<>();
                        if (prescriptions != null) {
                            for (Prescription p : prescriptions) {
                                if (p == null || p.getLines() == null) continue;
                                for (PrescriptionLine line : p.getLines()) {
                                    if (line == null) continue;
                                    allLines.add(line);
                                }
                            }
                        }

                        adapter.submitList(allLines);

                        if (allLines.isEmpty()) {
                            textEmpty.setVisibility(View.VISIBLE);
                        } else {
                            textEmpty.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onError(@Nullable Throwable throwable,
                                        @Nullable Integer httpCode,
                                        @Nullable String errorBody) {
                        if (!isAdded()) return;

                        showLoading(false);
                        adapter.submitList(new ArrayList<>());
                        textEmpty.setVisibility(View.VISIBLE);

                        Toast.makeText(
                                requireContext(),
                                getString(R.string.patient_medications_load_error),
                                Toast.LENGTH_LONG
                        ).show();
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
        if (textEmpty != null && loading) {
            textEmpty.setVisibility(View.GONE);
        }
    }

    // ---------------------------------------------------------------------
    // Reminder toggle callback
    // ---------------------------------------------------------------------

    @Override
    public void onReminderToggled(@NonNull PrescriptionLine line, boolean enabled) {
        if (!isAdded()) return;

        Long lineId = line.getId();
        if (lineId == null) {
            Toast.makeText(
                    requireContext(),
                    getString(R.string.patient_medications_update_reminder_error),
                    Toast.LENGTH_LONG
            ).show();
            // Reload to reset UI
            loadMyMedications();
            return;
        }

        prescriptionRepository.updateMyLineReminder(
                lineId,
                enabled,
                new PrescriptionRepository.UpdateReminderCallback() {
                    @Override
                    public void onSuccess(@NonNull PrescriptionLine updatedLine) {
                        if (!isAdded()) return;

                        // Schedule/cancel local alarms based on new state
                        if (enabled) {
                            MedicationReminderScheduler.scheduleReminder(
                                    requireContext().getApplicationContext(),
                                    updatedLine
                            );
                        } else {
                            MedicationReminderScheduler.cancelReminder(
                                    requireContext().getApplicationContext(),
                                    updatedLine
                            );
                        }

                        Toast.makeText(
                                requireContext(),
                                getString(R.string.patient_medications_update_reminder_success),
                                Toast.LENGTH_SHORT
                        ).show();

                        // Simple approach: reload list to reflect new state
                        loadMyMedications();
                    }

                    @Override
                    public void onError(@Nullable Throwable throwable,
                                        @Nullable Integer httpCode,
                                        @Nullable String errorBody) {
                        if (!isAdded()) return;

                        Toast.makeText(
                                requireContext(),
                                getString(R.string.patient_medications_update_reminder_error),
                                Toast.LENGTH_LONG
                        ).show();

                        // Reload to revert UI
                        loadMyMedications();
                    }
                }
        );
    }
}
