package tn.esprit.domain.medication;

import java.util.List;

/**
 * Domain model mirroring backend dto.medication.PrescriptionDto.
 *
 * Dates are kept as ISO date strings ("yyyy-MM-dd") as returned by backend.
 */
public class Prescription {

    private Long id;

    private Long doctorId;
    private Long doctorUserId;
    private String doctorFirstName;
    private String doctorLastName;

    private Long patientId;
    private Long patientUserId;
    private String patientFirstName;
    private String patientLastName;

    private String startDate;
    private String endDate;

    private String note;

    private List<PrescriptionLine> lines;

    // Needed by Gson / Retrofit
    public Prescription() {
    }

    public Prescription(Long id,
                        Long doctorId,
                        Long doctorUserId,
                        String doctorFirstName,
                        String doctorLastName,
                        Long patientId,
                        Long patientUserId,
                        String patientFirstName,
                        String patientLastName,
                        String startDate,
                        String endDate,
                        String note,
                        List<PrescriptionLine> lines) {
        this.id = id;
        this.doctorId = doctorId;
        this.doctorUserId = doctorUserId;
        this.doctorFirstName = doctorFirstName;
        this.doctorLastName = doctorLastName;
        this.patientId = patientId;
        this.patientUserId = patientUserId;
        this.patientFirstName = patientFirstName;
        this.patientLastName = patientLastName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.note = note;
        this.lines = lines;
    }

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

    public String getStartDate() {
        return startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public String getNote() {
        return note;
    }

    public List<PrescriptionLine> getLines() {
        return lines;
    }

    // UI helpers

    public String getDoctorFullName() {
        String first = doctorFirstName != null ? doctorFirstName.trim() : "";
        String last = doctorLastName != null ? doctorLastName.trim() : "";

        if (!first.isEmpty() && !last.isEmpty()) {
            return first + " " + last;
        }
        if (!last.isEmpty()) return last;
        if (!first.isEmpty()) return first;
        return "Doctor";
    }

    public String getPatientFullName() {
        String first = patientFirstName != null ? patientFirstName.trim() : "";
        String last = patientLastName != null ? patientLastName.trim() : "";

        if (!first.isEmpty() && !last.isEmpty()) {
            return first + " " + last;
        }
        if (!last.isEmpty()) return last;
        if (!first.isEmpty()) return first;
        return "Patient";
    }
}
