package tn.esprit.domain.medication;

/**
 * Domain model mirroring backend dto.medication.PrescriptionLineDto.
 *
 * NOTE:
 *  - Dates are represented as raw ISO-8601 strings (e.g. "2025-02-03"),
 *    so we don't depend on java.time parsing on Android.
 */
public class PrescriptionLine {

    private Long id;

    private Long prescriptionId;
    private String prescriptionStartDate;
    private String prescriptionEndDate;

    private Long medicationId;
    private String medicationName;

    private String dosage;
    private Integer timesPerDay;
    private String instructions;

    private Boolean reminderEnabled;

    // Needed by Gson / Retrofit
    public PrescriptionLine() {
    }

    public PrescriptionLine(Long id,
                            Long prescriptionId,
                            String prescriptionStartDate,
                            String prescriptionEndDate,
                            Long medicationId,
                            String medicationName,
                            String dosage,
                            Integer timesPerDay,
                            String instructions,
                            Boolean reminderEnabled) {
        this.id = id;
        this.prescriptionId = prescriptionId;
        this.prescriptionStartDate = prescriptionStartDate;
        this.prescriptionEndDate = prescriptionEndDate;
        this.medicationId = medicationId;
        this.medicationName = medicationName;
        this.dosage = dosage;
        this.timesPerDay = timesPerDay;
        this.instructions = instructions;
        this.reminderEnabled = reminderEnabled;
    }

    public Long getId() {
        return id;
    }

    public Long getPrescriptionId() {
        return prescriptionId;
    }

    public String getPrescriptionStartDate() {
        return prescriptionStartDate;
    }

    public String getPrescriptionEndDate() {
        return prescriptionEndDate;
    }

    public Long getMedicationId() {
        return medicationId;
    }

    public String getMedicationName() {
        return medicationName;
    }

    public String getDosage() {
        return dosage;
    }

    public Integer getTimesPerDay() {
        return timesPerDay;
    }

    public String getInstructions() {
        return instructions;
    }

    public Boolean getReminderEnabled() {
        return reminderEnabled;
    }

    // UI helpers

    public String getMedicationTitle() {
        if (medicationName != null && !medicationName.trim().isEmpty()) {
            return medicationName.trim();
        }
        return "Medication";
    }

    public String getDosageLabel() {
        if (dosage == null || dosage.trim().isEmpty()) return "";
        return dosage.trim();
    }

    public String getTimesPerDayLabel() {
        if (timesPerDay == null || timesPerDay <= 0) return "";
        if (timesPerDay == 1) {
            return "1 time per day";
        }
        return timesPerDay + " times per day";
    }

    public boolean isReminderEnabledSafe() {
        return reminderEnabled != null && reminderEnabled;
    }
}
