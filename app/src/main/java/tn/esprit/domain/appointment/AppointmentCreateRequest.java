package tn.esprit.domain.appointment;

/**
 * Mirrors backend dto.appointment.AppointmentCreateRequest.
 *
 * Date/time fields are ISO-8601 strings on the mobile side.
 */
public class AppointmentCreateRequest {

    private Long doctorId;
    private String startAt;          // "yyyy-MM-dd'T'HH:mm:ss"
    private String endAt;            // "yyyy-MM-dd'T'HH:mm:ss"
    private String reason;
    private Boolean teleconsultation;

    public AppointmentCreateRequest() {
    }

    public Long getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Long doctorId) {
        this.doctorId = doctorId;
    }

    public String getStartAt() {
        return startAt;
    }

    public void setStartAt(String startAt) {
        this.startAt = startAt;
    }

    public String getEndAt() {
        return endAt;
    }

    public void setEndAt(String endAt) {
        this.endAt = endAt;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Boolean getTeleconsultation() {
        return teleconsultation;
    }

    public void setTeleconsultation(Boolean teleconsultation) {
        this.teleconsultation = teleconsultation;
    }
}
