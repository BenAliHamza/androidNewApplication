package tn.esprit.data.remote.appointment;

import java.util.List;

public class AvailabilitySessionRequest {
    public String startDate;
    public String endDate;
    public String startTime;
    public String endTime;
    public int slotDurationMinutes;
    public String recurrenceType;   // "ONE_TIME" or "WEEKLY"
    public List<String> daysOfWeek; // e.g. ["MONDAY","WEDNESDAY"]
    public AvailabilitySessionRequest() {
    }
}
