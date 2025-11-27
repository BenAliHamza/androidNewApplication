package tn.esprit.domain.appointment;

import androidx.annotation.Nullable;

import java.util.List;

/**
 * Weekly calendar wrapper.
 * JSON: { "days": [ { "date": "...", "slots": [...] }, ... ] }
 */
public class WeeklyCalendarResponse {

    private List<DailySchedule> days;

    @Nullable
    public List<DailySchedule> getDays() {
        return days;
    }

    public void setDays(@Nullable List<DailySchedule> days) {
        this.days = days;
    }
}
