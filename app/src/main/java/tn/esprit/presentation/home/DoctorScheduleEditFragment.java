package tn.esprit.presentation.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;
import java.util.List;

import tn.esprit.R;
import tn.esprit.domain.appointment.DoctorSchedule;

/**
 * Edit screen for weekly schedule.
 *
 * - Uses REAL backend via DoctorScheduleViewModel + DoctorScheduleRepository.
 * - NO dummy or static fake data: we read lastLoadedSchedule and send a real PUT.
 * - UI lets you pick per-day:
 *      OFF / MORNING (08:00-13:00) / AFTERNOON (14:00-19:00) / FULL DAY (08:00-19:00).
 *
 * OFF days are NOT sent to backend at all (no DoctorSchedule entry for that day),
 * so every element in the request body has dayOfWeek + startTime + endTime, as required.
 */
public class DoctorScheduleEditFragment extends Fragment {

    private static final int SHIFT_OFF = 0;
    private static final int SHIFT_MORNING = 1;
    private static final int SHIFT_AFTERNOON = 2;
    private static final int SHIFT_FULL_DAY = 3;

    private static final String TIME_MORNING_START = "08:00";
    private static final String TIME_MORNING_END = "13:00";
    private static final String TIME_AFTERNOON_START = "14:00";
    private static final String TIME_AFTERNOON_END = "19:00";
    private static final String TIME_FULL_START = "08:00";
    private static final String TIME_FULL_END = "19:00";

    private DoctorScheduleViewModel viewModel;

    private ProgressBar progressBar;
    private Button buttonCancel;
    private Button buttonSave;

    private Spinner spinnerMonday;
    private Spinner spinnerTuesday;
    private Spinner spinnerWednesday;
    private Spinner spinnerThursday;
    private Spinner spinnerFriday;
    private Spinner spinnerSaturday;
    private Spinner spinnerSunday;

    private boolean hasInitialisedFromSchedule = false;
    private boolean lastIsLoading = false;
    private boolean lastIsSaving = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_doctor_schedule_edit, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        progressBar = view.findViewById(R.id.doctor_schedule_edit_progress);
        buttonCancel = view.findViewById(R.id.button_cancel_schedule);
        buttonSave = view.findViewById(R.id.button_save_schedule);

        spinnerMonday = view.findViewById(R.id.spinner_monday_shift);
        spinnerTuesday = view.findViewById(R.id.spinner_tuesday_shift);
        spinnerWednesday = view.findViewById(R.id.spinner_wednesday_shift);
        spinnerThursday = view.findViewById(R.id.spinner_thursday_shift);
        spinnerFriday = view.findViewById(R.id.spinner_friday_shift);
        spinnerSaturday = view.findViewById(R.id.spinner_saturday_shift);
        spinnerSunday = view.findViewById(R.id.spinner_sunday_shift);

        ImageButton buttonBack = view.findViewById(R.id.button_back_schedule);

        viewModel = new ViewModelProvider(requireActivity())
                .get(DoctorScheduleViewModel.class);

        setupSpinners();
        observeViewModel();

        if (buttonBack != null) {
            buttonBack.setOnClickListener(v -> {
                requireActivity()
                        .getOnBackPressedDispatcher()
                        .onBackPressed();
            });
        }

        if (buttonCancel != null) {
            buttonCancel.setOnClickListener(v -> {
                requireActivity()
                        .getOnBackPressedDispatcher()
                        .onBackPressed();
            });
        }

        if (buttonSave != null) {
            buttonSave.setOnClickListener(v -> {
                List<DoctorSchedule> entries = buildEntriesFromUi();
                viewModel.saveSchedule(entries);
            });
        }
    }

    // ------------------------------------------------------------------------
    // UI Setup
    // ------------------------------------------------------------------------

    private void setupSpinners() {
        if (getContext() == null) return;

        String[] options = new String[]{
                getString(R.string.doctor_schedule_shift_off),
                getString(R.string.doctor_schedule_shift_morning),
                getString(R.string.doctor_schedule_shift_afternoon),
                getString(R.string.doctor_schedule_shift_full_day)
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                options
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinnerMonday.setAdapter(adapter);
        spinnerTuesday.setAdapter(adapter);
        spinnerWednesday.setAdapter(adapter);
        spinnerThursday.setAdapter(adapter);
        spinnerFriday.setAdapter(adapter);
        spinnerSaturday.setAdapter(adapter);
        spinnerSunday.setAdapter(adapter);
    }

    // ------------------------------------------------------------------------
    // ViewModel observation
    // ------------------------------------------------------------------------

    private void observeViewModel() {
        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (state == null) return;

            lastIsLoading = state.isLoading();
            updateLoadingState();

            // Initialize spinners from backend schedule only once, after first load
            if (!lastIsLoading && !hasInitialisedFromSchedule) {
                hasInitialisedFromSchedule = true;
                applyExistingSchedule(viewModel.getLastLoadedSchedule());
            }
        });

        viewModel.getIsSaving().observe(getViewLifecycleOwner(), isSaving -> {
            if (isSaving == null) return;
            lastIsSaving = isSaving;
            updateLoadingState();
        });

        viewModel.getSaveSuccessEvent().observe(getViewLifecycleOwner(), success -> {
            if (success == null || !success) return;
            viewModel.clearSaveSuccessEvent();

            // Go back to office screen after successful save
            requireActivity()
                    .getOnBackPressedDispatcher()
                    .onBackPressed();
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), msg -> {
            // Optional: Toast for errors
            // if (!TextUtils.isEmpty(msg)) {
            //     Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
            // }
        });
    }

    private void updateLoadingState() {
        boolean loading = lastIsLoading || lastIsSaving;

        if (progressBar != null) {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        if (buttonSave != null) {
            buttonSave.setEnabled(!loading);
        }
        if (buttonCancel != null) {
            buttonCancel.setEnabled(!loading);
        }

        if (spinnerMonday != null) spinnerMonday.setEnabled(!loading);
        if (spinnerTuesday != null) spinnerTuesday.setEnabled(!loading);
        if (spinnerWednesday != null) spinnerWednesday.setEnabled(!loading);
        if (spinnerThursday != null) spinnerThursday.setEnabled(!loading);
        if (spinnerFriday != null) spinnerFriday.setEnabled(!loading);
        if (spinnerSaturday != null) spinnerSaturday.setEnabled(!loading);
        if (spinnerSunday != null) spinnerSunday.setEnabled(!loading);
    }

    // ------------------------------------------------------------------------
    // Existing schedule -> spinner selection
    // ------------------------------------------------------------------------

    private void applyExistingSchedule(List<DoctorSchedule> schedule) {
        if (schedule == null) return;

        spinnerMonday.setSelection(inferShiftForDay("MONDAY", schedule));
        spinnerTuesday.setSelection(inferShiftForDay("TUESDAY", schedule));
        spinnerWednesday.setSelection(inferShiftForDay("WEDNESDAY", schedule));
        spinnerThursday.setSelection(inferShiftForDay("THURSDAY", schedule));
        spinnerFriday.setSelection(inferShiftForDay("FRIDAY", schedule));
        spinnerSaturday.setSelection(inferShiftForDay("SATURDAY", schedule));
        spinnerSunday.setSelection(inferShiftForDay("SUNDAY", schedule));
    }

    private int inferShiftForDay(String dayCode, List<DoctorSchedule> schedule) {
        if (schedule == null) return SHIFT_OFF;

        DoctorSchedule match = null;
        for (DoctorSchedule slot : schedule) {
            if (slot == null) continue;
            if (slot.getDayOfWeek() == null) continue;
            if (!dayCode.equalsIgnoreCase(slot.getDayOfWeek())) continue;

            match = slot;
            break;
        }

        if (match == null || match.getActive() == null || !match.getActive()) {
            return SHIFT_OFF;
        }

        String start = match.getStartTime();
        String end = match.getEndTime();

        if (TIME_MORNING_START.equals(start) && TIME_MORNING_END.equals(end)) {
            return SHIFT_MORNING;
        } else if (TIME_AFTERNOON_START.equals(start) && TIME_AFTERNOON_END.equals(end)) {
            return SHIFT_AFTERNOON;
        } else if (TIME_FULL_START.equals(start) && TIME_FULL_END.equals(end)) {
            return SHIFT_FULL_DAY;
        }

        // If backend has some other custom hours, treat as FULL DAY fallback.
        return SHIFT_FULL_DAY;
    }

    // ------------------------------------------------------------------------
    // Spinner selection -> DoctorSchedule list
    // ------------------------------------------------------------------------

    /**
     * Build the list to send to backend.
     *
     * IMPORTANT: OFF days are NOT added to the list, so every entry that is sent
     * has dayOfWeek + startTime + endTime, matching backend validation.
     */
    private List<DoctorSchedule> buildEntriesFromUi() {
        List<DoctorSchedule> list = new ArrayList<>();

        addIfNotOff(list, "MONDAY", spinnerMonday.getSelectedItemPosition());
        addIfNotOff(list, "TUESDAY", spinnerTuesday.getSelectedItemPosition());
        addIfNotOff(list, "WEDNESDAY", spinnerWednesday.getSelectedItemPosition());
        addIfNotOff(list, "THURSDAY", spinnerThursday.getSelectedItemPosition());
        addIfNotOff(list, "FRIDAY", spinnerFriday.getSelectedItemPosition());
        addIfNotOff(list, "SATURDAY", spinnerSaturday.getSelectedItemPosition());
        addIfNotOff(list, "SUNDAY", spinnerSunday.getSelectedItemPosition());

        return list;
    }

    private void addIfNotOff(List<DoctorSchedule> list,
                             String dayCode,
                             int shift) {
        DoctorSchedule entry = buildEntryForDay(dayCode, shift);
        if (entry != null) {
            list.add(entry);
        }
    }

    /**
     * Returns a DoctorSchedule for this day if shift != OFF.
     * Returns null if day is OFF (so it is not included in the request).
     */
    @Nullable
    private DoctorSchedule buildEntryForDay(String dayCode, int shift) {
        if (shift == SHIFT_OFF) {
            return null;
        }

        DoctorSchedule entry = new DoctorSchedule();
        entry.setDayOfWeek(dayCode);
        entry.setActive(true);

        switch (shift) {
            case SHIFT_MORNING:
                entry.setStartTime(TIME_MORNING_START);
                entry.setEndTime(TIME_MORNING_END);
                break;
            case SHIFT_AFTERNOON:
                entry.setStartTime(TIME_AFTERNOON_START);
                entry.setEndTime(TIME_AFTERNOON_END);
                break;
            case SHIFT_FULL_DAY:
            default:
                entry.setStartTime(TIME_FULL_START);
                entry.setEndTime(TIME_FULL_END);
                break;
        }

        return entry;
    }
}
