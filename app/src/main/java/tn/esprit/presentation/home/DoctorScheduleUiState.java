package tn.esprit.presentation.home;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Simple state holder for DoctorOfficeFragment.
 *
 * Contains:
 *  - loading flag
 *  - whether we have any active schedule
 *  - per-day summaries.
 */
public class DoctorScheduleUiState {

    private final boolean loading;
    private final boolean hasSchedule;
    private final List<DayScheduleSummary> days;

    public DoctorScheduleUiState(boolean loading,
                                 boolean hasSchedule,
                                 List<DayScheduleSummary> days) {
        this.loading = loading;
        this.hasSchedule = hasSchedule;
        if (days == null) {
            this.days = Collections.emptyList();
        } else {
            this.days = Collections.unmodifiableList(new ArrayList<>(days));
        }
    }

    public static DoctorScheduleUiState createInitial() {
        return new DoctorScheduleUiState(false, false, Collections.emptyList());
    }

    public boolean isLoading() {
        return loading;
    }

    public boolean hasSchedule() {
        return hasSchedule;
    }

    public List<DayScheduleSummary> getDays() {
        return days;
    }

    // ------------------------------------------------------------------------
    // Nested type: one row per day
    // ------------------------------------------------------------------------

    public static class DayScheduleSummary {

        private final String dayCode;      // "MONDAY", ...
        private final String summaryText;  // "08:00 – 12:00 · 14:00 – 18:00"
        private final boolean active;

        public DayScheduleSummary(String dayCode,
                                  String summaryText,
                                  boolean active) {
            this.dayCode = dayCode;
            this.summaryText = summaryText;
            this.active = active;
        }

        public String getDayCode() {
            return dayCode;
        }

        public String getSummaryText() {
            return summaryText;
        }

        public boolean isActive() {
            return active;
        }
    }
}
