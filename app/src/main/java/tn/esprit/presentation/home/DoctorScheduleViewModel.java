package tn.esprit.presentation.home;

import android.app.Application;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import tn.esprit.R;
import tn.esprit.data.appointment.DoctorScheduleRepository;
import tn.esprit.domain.appointment.DoctorSchedule;

/**
 * ViewModel for the doctor office / schedule screen.
 *
 * Responsibilities:
 *  - Load weekly schedule for current doctor from backend.
 *  - Map raw slots to a friendly weekly summary for the UI.
 *  - Expose a simple "loading + days + hasSchedule" state and an error message.
 *  - Expose saving state + save success event for the edit screen.
 */
public class DoctorScheduleViewModel extends AndroidViewModel {

    private final DoctorScheduleRepository repository;

    private final MutableLiveData<DoctorScheduleUiState> uiState =
            new MutableLiveData<>(DoctorScheduleUiState.createInitial());

    private final MutableLiveData<String> errorMessage =
            new MutableLiveData<>();

    // Saving state (used by edit screen)
    private final MutableLiveData<Boolean> isSaving =
            new MutableLiveData<>(false);

    // One-shot event to notify edit screen that save succeeded
    private final MutableLiveData<Boolean> saveSuccessEvent =
            new MutableLiveData<>(false);

    // Keep last loaded schedule so edit screen can pre-fill choices
    private List<DoctorSchedule> lastLoadedSchedule = Collections.emptyList();

    public DoctorScheduleViewModel(@NonNull Application application) {
        super(application);
        repository = new DoctorScheduleRepository(application.getApplicationContext());
        loadSchedule();
    }

    public LiveData<DoctorScheduleUiState> getUiState() {
        return uiState;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Boolean> getIsSaving() {
        return isSaving;
    }

    public LiveData<Boolean> getSaveSuccessEvent() {
        return saveSuccessEvent;
    }

    public void clearSaveSuccessEvent() {
        saveSuccessEvent.setValue(false);
    }

    /**
     * Raw schedule from last successful load / save.
     */
    public List<DoctorSchedule> getLastLoadedSchedule() {
        return lastLoadedSchedule != null ? new ArrayList<>(lastLoadedSchedule) : Collections.emptyList();
    }

    /**
     * Reload schedule from backend.
     */
    public void refresh() {
        loadSchedule();
    }

    /**
     * Save the schedule to backend (real PUT call).
     */
    public void saveSchedule(List<DoctorSchedule> entries) {
        // start saving
        isSaving.setValue(true);
        saveSuccessEvent.setValue(false);

        repository.updateMySchedule(entries, new DoctorScheduleRepository.ScheduleCallback() {
            @Override
            public void onSuccess(List<DoctorSchedule> schedule) {
                lastLoadedSchedule = schedule != null
                        ? new ArrayList<>(schedule)
                        : Collections.emptyList();

                DoctorScheduleUiState newState = buildStateFromSchedule(schedule);
                uiState.postValue(newState);

                isSaving.postValue(false);
                // fire "save succeeded" event
                saveSuccessEvent.postValue(true);
            }

            @Override
            public void onError(Throwable throwable, Integer httpCode, String errorBody) {
                String msg = getApplication().getString(R.string.doctor_office_schedule_load_error);
                errorMessage.postValue(msg);
                isSaving.postValue(false);
            }
        });
    }

    // ------------------------------------------------------------------------
    // Internal
    // ------------------------------------------------------------------------

    private void loadSchedule() {
        DoctorScheduleUiState current = uiState.getValue();
        if (current == null) {
            current = DoctorScheduleUiState.createInitial();
        }

        // Set loading flag, keep existing days (for smoother UX)
        uiState.setValue(new DoctorScheduleUiState(
                true,
                current.hasSchedule(),
                current.getDays()
        ));

        repository.getMySchedule(new DoctorScheduleRepository.LoadScheduleCallback() {
            @Override
            public void onSuccess(List<DoctorSchedule> schedule) {
                lastLoadedSchedule = schedule != null
                        ? new ArrayList<>(schedule)
                        : Collections.emptyList();

                DoctorScheduleUiState newState = buildStateFromSchedule(schedule);
                uiState.postValue(newState);
            }

            @Override
            public void onError(Throwable throwable, Integer httpCode, String errorBody) {
                String msg = getApplication().getString(R.string.doctor_office_schedule_load_error);
                errorMessage.postValue(msg);

                DoctorScheduleUiState prev = uiState.getValue();
                if (prev == null) {
                    prev = DoctorScheduleUiState.createInitial();
                }

                uiState.postValue(new DoctorScheduleUiState(
                        false,
                        prev.hasSchedule(),
                        prev.getDays()
                ));
            }
        });
    }

    private DoctorScheduleUiState buildStateFromSchedule(List<DoctorSchedule> schedule) {
        List<DoctorScheduleUiState.DayScheduleSummary> days = new ArrayList<>();

        days.add(buildDaySummary("MONDAY", schedule));
        days.add(buildDaySummary("TUESDAY", schedule));
        days.add(buildDaySummary("WEDNESDAY", schedule));
        days.add(buildDaySummary("THURSDAY", schedule));
        days.add(buildDaySummary("FRIDAY", schedule));
        days.add(buildDaySummary("SATURDAY", schedule));
        days.add(buildDaySummary("SUNDAY", schedule));

        boolean hasAnyActive = false;
        for (DoctorScheduleUiState.DayScheduleSummary d : days) {
            if (d.isActive()) {
                hasAnyActive = true;
                break;
            }
        }

        return new DoctorScheduleUiState(
                false,
                hasAnyActive,
                days
        );
    }

    private DoctorScheduleUiState.DayScheduleSummary buildDaySummary(
            String dayCode,
            List<DoctorSchedule> schedule
    ) {
        List<String> segments = new ArrayList<>();
        boolean anyActive = false;

        if (schedule != null) {
            for (DoctorSchedule slot : schedule) {
                if (slot == null) continue;
                if (slot.getDayOfWeek() == null) continue;
                if (!dayCode.equalsIgnoreCase(slot.getDayOfWeek())) continue;

                Boolean active = slot.getActive();
                if (active != null && active) {
                    anyActive = true;

                    String start = slot.getStartTime();
                    String end = slot.getEndTime();

                    if (TextUtils.isEmpty(start) || TextUtils.isEmpty(end)) {
                        continue;
                    }

                    segments.add(start + " – " + end);
                }
            }
        }

        String summary;
        if (anyActive && !segments.isEmpty()) {
            summary = TextUtils.join("  ·  ", segments);
        } else {
            summary = getApplication().getString(R.string.doctor_office_day_off);
        }

        return new DoctorScheduleUiState.DayScheduleSummary(dayCode, summary, anyActive);
    }
}
