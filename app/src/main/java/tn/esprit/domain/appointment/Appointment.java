package tn.esprit.domain.appointment;

/**
 * Mirrors backend dto.appointment.AppointmentDto.
 *
 * Date/time fields are represented as ISO-8601 strings on the mobile side.
 */
public class Appointment {

    private Long id;

    // Doctor info
    private Long doctorId;
    private Long doctorUserId;
    private String doctorFirstName;
    private String doctorLastName;

    // Patient info
    private Long patientId;
    private Long patientUserId;
    private String patientFirstName;
    private String patientLastName;

    // Time range (ISO-8601, e.g. "2025-03-17T08:30:00")
    private String startAt;
    private String endAt;

    // Status from backend enum (kept as raw string)
    private String status;

    private String reason;
    private Boolean teleconsultation;

    public Appointment() {
    }

    // --- getters & setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Long doctorId) {
        this.doctorId = doctorId;
    }

    public Long getDoctorUserId() {
        return doctorUserId;
    }

    public void setDoctorUserId(Long doctorUserId) {
        this.doctorUserId = doctorUserId;
    }

    public String getDoctorFirstName() {
        return doctorFirstName;
    }

    public void setDoctorFirstName(String doctorFirstName) {
        this.doctorFirstName = doctorFirstName;
    }

    public String getDoctorLastName() {
        return doctorLastName;
    }

    public void setDoctorLastName(String doctorLastName) {
        this.doctorLastName = doctorLastName;
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public Long getPatientUserId() {
        return patientUserId;
    }

    public void setPatientUserId(Long patientUserId) {
        this.patientUserId = patientUserId;
    }

    public String getPatientFirstName() {
        return patientFirstName;
    }

    public void setPatientFirstName(String patientFirstName) {
        this.patientFirstName = patientFirstName;
    }

    public String getPatientLastName() {
        return patientLastName;
    }

    public void setPatientLastName(String patientLastName) {
        this.patientLastName = patientLastName;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    // --- helpers ---

    public String getDoctorFullName() {
        String first = doctorFirstName != null ? doctorFirstName.trim() : "";
        String last = doctorLastName != null ? doctorLastName.trim() : "";
        String combined = (first + " " + last).trim();
        return combined.isEmpty() ? "" : combined;
    }

    public String getPatientFullName() {
        String first = patientFirstName != null ? patientFirstName.trim() : "";
        String last = patientLastName != null ? patientLastName.trim() : "";
        String combined = (first + " " + last).trim();
        return combined.isEmpty() ? "" : combined;
    }
}
