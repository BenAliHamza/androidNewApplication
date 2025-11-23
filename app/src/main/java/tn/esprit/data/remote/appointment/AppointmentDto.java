package tn.esprit.data.remote.appointment;

public class AppointmentDto {
    public Long id;
    public Long doctorUserId;
    public String doctorFullName;
    public Long patientUserId;
    public String patientFullName;

    public String date;       // "2025-01-17"
    public String startTime;  // "09:00"
    public String endTime;    // "09:30"

    public String status;     // "PENDING", "ACCEPTED"...
    public String reason;
}
