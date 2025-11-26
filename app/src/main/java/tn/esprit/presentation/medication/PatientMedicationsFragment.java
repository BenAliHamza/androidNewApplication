package tn.esprit.presentation.medication;

import android.app.TimePickerDialog;
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

import java.util.Calendar;
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

        // Patient endpoint: GET /api/prescriptions/me?activeOnly=<null for ALL>
        prescriptionRepository.getMyPrescriptions(
                null,   // <-- IMPORTANT: null = both active and past prescriptions
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
        if (lineId == null || lineId <= 0L) {
            Toast.makeText(
                    requireContext(),
                    getString(R.string.patient_medications_update_reminder_error),
                    Toast.LENGTH_LONG
            ).show();
            // Reload to reset UI
            loadMyMedications();
            return;
        }

        if (enabled) {
            // If we already have a stored time, use it directly
            int[] existingTime = MedicationReminderScheduler.getReminderTime(
                    requireContext(),
                    lineId
            );

            if (existingTime != null) {
                updateReminderOnServerAndSchedule(line, true, existingTime[0], existingTime[1]);
            } else {
                // Ask user for time first
                showTimePickerForLine(line);
            }
        } else {
            // Turning OFF: cancel local alarm + clear stored time + update server
            String medName = safeMedicationName(line);
            MedicationReminderScheduler.cancelReminder(
                    requireContext(),
                    lineId,
                    medName
            );
            updateReminderOnServerAndSchedule(line, false, -1, -1);
        }
    }

    private void showTimePickerForLine(@NonNull PrescriptionLine line) {
        if (!isAdded()) return;

        Calendar now = Calendar.getInstance();
        int defaultHour = now.get(Calendar.HOUR_OF_DAY);
        int defaultMinute = now.get(Calendar.MINUTE);

        TimePickerDialog dialog = new TimePickerDialog(
                requireContext(),
                (view, hourOfDay, minute) -> {
                    // User picked a time -> update backend + schedule alarm
                    updateReminderOnServerAndSchedule(line, true, hourOfDay, minute);
                },
                defaultHour,
                defaultMinute,
                android.text.format.DateFormat.is24HourFormat(requireContext())
        );

        dialog.setOnCancelListener(d -> {
            // User cancelled selecting a time -> revert UI by reloading list
            loadMyMedications();
        });

        dialog.show();
    }

    private void updateReminderOnServerAndSchedule(@NonNull PrescriptionLine line,
                                                   boolean enabled,
                                                   int hourOfDay,
                                                   int minute) {
        Long lineId = line.getId();
        if (lineId == null) return;

        prescriptionRepository.updateMyLineReminder(
                lineId,
                enabled,
                new PrescriptionRepository.UpdateReminderCallback() {
                    @Override
                    public void onSuccess(@NonNull PrescriptionLine updatedLine) {
                        if (!isAdded()) return;

                        if (enabled) {
                            String medName = safeMedicationName(updatedLine);
                            MedicationReminderScheduler.scheduleDailyReminder(
                                    requireContext(),
                                    updatedLine.getId(),
                                    medName,
                                    hourOfDay,
                                    minute
                            );
                        }

                        Toast.makeText(
                                requireContext(),
                                getString(R.string.patient_medications_update_reminder_success),
                                Toast.LENGTH_SHORT
                        ).show();

                        // Simple approach: reload list to reflect new state & reset switches
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

                        // Reload to revert UI (switch state)
                        loadMyMedications();
                    }
                }
        );
    }

    private String safeMedicationName(@NonNull PrescriptionLine line) {
        String name = line.getMedicationName();
        if (name == null || name.trim().isEmpty()) {
            return getString(R.string.patient_medication_name_placeholder);
        }
        return name.trim();
    }
}
