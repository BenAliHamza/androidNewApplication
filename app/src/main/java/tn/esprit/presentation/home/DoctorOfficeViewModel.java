package tn.esprit.presentation.home;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import tn.esprit.data.appointment.DoctorScheduleRepository;
import tn.esprit.domain.appointment.DoctorSchedule;

/**
 * ViewModel backing the doctor office (schedule) screen.
 */
public class DoctorOfficeViewModel extends ViewModel {

    private static final String[] DAY_ORDER = new String[]{
            "MONDAY",
            "TUESDAY",
            "WEDNESDAY",
            "THURSDAY",
            "FRIDAY",
            "SATURDAY",
            "SUNDAY"
    };

    private final DoctorScheduleRepository repository;

    private final MutableLiveData<DoctorScheduleUiState> uiState =
            new MutableLiveData<>(DoctorScheduleUiState.createInitial());

    // Simple flag event for load error (observed as Boolean)
    private final MutableLiveData<Boolean> loadErrorEvents = new MutableLiveData<>();

    private List<DoctorSchedule> currentSchedule = new ArrayList<>();

    public DoctorOfficeViewModel(@NonNull DoctorScheduleRepository repository) {
        this.repository = repository;
    }

    public LiveData<DoctorScheduleUiState> getUiState() {
        return uiState;
    }

    public LiveData<Boolean> getLoadErrorEvents() {
        return loadErrorEvents;
    }

    // ------------------------------------------------------------------------
    // Public actions
    // ------------------------------------------------------------------------

    public void loadSchedule() {
        uiState.setValue(new DoctorScheduleUiState(
                true,
                false,
                Collections.emptyList()
        ));

        repository.getMySchedule(new DoctorScheduleRepository.LoadScheduleCallback() {
            @Override
            public void onSuccess(List<DoctorSchedule> schedule) {
                currentSchedule = (schedule != null)
                        ? new ArrayList<>(schedule)
                        : new ArrayList<>();

                DoctorScheduleUiState newState = buildUiStateFromSchedule(currentSchedule);
                uiState.setValue(newState);
            }

            @Override
            public void onError(Throwable throwable, Integer httpCode, String errorBody) {
                // Keep it simple: show empty + error toast via fragment
                uiState.setValue(DoctorScheduleUiState.createInitial());
                loadErrorEvents.setValue(Boolean.TRUE);
            }
        });
    }

    // Will be used later when implementing editing
    public List<DoctorSchedule> getCurrentSchedule() {
        return new ArrayList<>(currentSchedule);
    }

    // ------------------------------------------------------------------------
    // Mapping
    // ------------------------------------------------------------------------

    private DoctorScheduleUiState buildUiStateFromSchedule(List<DoctorSchedule> schedule) {
        List<DoctorScheduleUiState.DayScheduleSummary> daySummaries = new ArrayList<>();

        if (schedule == null) {
            schedule = Collections.emptyList();
        }

        boolean anyActive = false;

        for (String dayCode : DAY_ORDER) {
            List<DoctorSchedule> dayEntries = new ArrayList<>();
            for (DoctorSchedule slot : schedule) {
                if (slot == null) continue;
                String code = slot.getDayOfWeek();
                if (code == null) continue;
                if (code.equalsIgnoreCase(dayCode)) {
                    dayEntries.add(slot);
                }
            }

            String summaryText = null;
            boolean active = false;

            if (!dayEntries.isEmpty()) {
                List<String> parts = new ArrayList<>();
                for (DoctorSchedule slot : dayEntries) {
                    Boolean slotActive = slot.getActive();
                    if (slotActive != null && slotActive) {
                        String start = slot.getStartTime();
                        String end = slot.getEndTime();
                        if (start != null && end != null) {
                            parts.add(start + " – " + end);
                        } else if (start != null || end != null) {
                            // Partial info, still show something
                            String partial = (start != null ? start : "?")
                                    + " – "
                                    + (end != null ? end : "?");
                            parts.add(partial);
                        }
                    }
                }

                if (!parts.isEmpty()) {
                    StringBuilder builder = new StringBuilder();
                    for (int i = 0; i < parts.size(); i++) {
                        if (i > 0) builder.append(" · ");
                        builder.append(parts.get(i));
                    }
                    summaryText = builder.toString();
                    active = true;
                    anyActive = true;
                }
            }

            daySummaries.add(
                    new DoctorScheduleUiState.DayScheduleSummary(
                            dayCode,
                            summaryText,
                            active
                    )
            );
        }

        return new DoctorScheduleUiState(
                false,
                anyActive,
                daySummaries
        );
    }

    // ------------------------------------------------------------------------
    // Factory for injecting repository
    // ------------------------------------------------------------------------

    public static class Factory implements ViewModelProvider.Factory {

        private final DoctorScheduleRepository repository;

        public Factory(@NonNull DoctorScheduleRepository repository) {
            this.repository = repository;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (modelClass.isAssignableFrom(DoctorOfficeViewModel.class)) {
                return (T) new DoctorOfficeViewModel(repository);
            }
            throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
        }
    }
}
