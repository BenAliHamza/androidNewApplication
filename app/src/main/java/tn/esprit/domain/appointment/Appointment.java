package tn.esprit.domain.appointment;

public class Appointment {

    private Long id;

    private Long doctorId;
    private Long doctorUserId;
    private String doctorFirstName;
    private String doctorLastName;

    private Long patientId;
    private Long patientUserId;
    private String patientFirstName;
    private String patientLastName;

    private String startAt;
    private String endAt;

    private String status;

    private String reason;
    private Boolean teleconsultation;

    public Appointment() {}

    public Long getId() {
        return id;
    }

    public Long getDoctorId() {
        return doctorId;
    }

    public Long getDoctorUserId() {
        return doctorUserId;
    }

    public String getDoctorFirstName() {
        return doctorFirstName;
    }

    public String getDoctorLastName() {
        return doctorLastName;
    }

    public Long getPatientId() {
        return patientId;
    }

    public Long getPatientUserId() {
        return patientUserId;
    }

    public String getPatientFirstName() {
        return patientFirstName;
    }

    public String getPatientLastName() {
        return patientLastName;
    }

    public String getStartAt() {
        return startAt;
    }

    public String getEndAt() {
        return endAt;
    }

    public String getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public Boolean getTeleconsultation() {
        return teleconsultation;
    }
}
