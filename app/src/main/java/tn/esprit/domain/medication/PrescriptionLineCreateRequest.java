package tn.esprit.domain.medication;

/**
 * Domain model mirroring backend dto.medication.PrescriptionLineCreateRequest.
 *
 * Fields:
 *  - medicationId
 *  - dosage
 *  - timesPerDay
 *  - instructions
 */
public class PrescriptionLineCreateRequest {

    private Long medicationId;
    private String dosage;
    private Integer timesPerDay;
    private String instructions;

    public PrescriptionLineCreateRequest() {
    }

    public PrescriptionLineCreateRequest(Long medicationId,
                                         String dosage,
                                         Integer timesPerDay,
                                         String instructions) {
        this.medicationId = medicationId;
        this.dosage = dosage;
        this.timesPerDay = timesPerDay;
        this.instructions = instructions;
    }

    public Long getMedicationId() {
        return medicationId;
    }

    public void setMedicationId(Long medicationId) {
        this.medicationId = medicationId;
    }

    public String getDosage() {
        return dosage;
    }

    public void setDosage(String dosage) {
        this.dosage = dosage;
    }

    public Integer getTimesPerDay() {
        return timesPerDay;
    }

    public void setTimesPerDay(Integer timesPerDay) {
        this.timesPerDay = timesPerDay;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }
}
