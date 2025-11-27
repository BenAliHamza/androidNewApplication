package tn.esprit.domain.appointment;

import androidx.annotation.Nullable;

import java.util.List;

/**
 * One day in the weekly calendar.
 * Matches backend JSON:
 * { "date": "2025-11-27", "slots": [ { "time": "08:00", "available": true }, ... ] }
 */
public class DailySchedule {

    private String date;            // "yyyy-MM-dd"
    private List<Slot> slots;       // may be null/empty

    @Nullable
    public String getDate() {
        return date;
    }

    public void setDate(@Nullable String date) {
        this.date = date;
    }

    @Nullable
    public List<Slot> getSlots() {
        return slots;
    }

    public void setSlots(@Nullable List<Slot> slots) {
        this.slots = slots;
    }
}
