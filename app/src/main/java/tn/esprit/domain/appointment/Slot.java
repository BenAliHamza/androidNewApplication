package tn.esprit.domain.appointment;

import androidx.annotation.Nullable;

public class Slot {

    // Must match JSON: { "time": "08:30", "available": true }
    private String time;
    private boolean available;

    @Nullable
    public String getTime() {
        return time;
    }

    public void setTime(@Nullable String time) {
        this.time = time;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
}
