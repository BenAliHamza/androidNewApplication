package tn.esprit.data.remote.appointment;

public class AppointmentCreateRequest {
    public Long doctorUserId;
    public String date;       // YYYY-MM-DD
    public String startTime;  // HH:mm
    public String endTime;    // HH:mm
    public String reason;
}
