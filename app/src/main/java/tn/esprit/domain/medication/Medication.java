package tn.esprit.domain.medication;

/**
 * Domain model mirroring backend dto.medication.MedicationDto.
 *
 * Fields:
 *  - id
 *  - code
 *  - name
 *  - description
 *  - active
 */
public class Medication {

    private Long id;
    private String code;
    private String name;
    private String description;
    private Boolean active;

    // Needed by Gson / Retrofit
    public Medication() {
    }

    public Medication(Long id,
                      String code,
                      String name,
                      String description,
                      Boolean active) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.description = description;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Boolean getActive() {
        return active;
    }

    // Simple UI helpers

    /**
     * Returns a user-friendly display name, falling back to code if needed.
     */
    public String getDisplayName() {
        if (name != null && !name.trim().isEmpty()) {
            return name.trim();
        }
        if (code != null && !code.trim().isEmpty()) {
            return code.trim();
        }
        return "Medication";
    }

    /**
     * Returns true if medication is flagged active (null => false).
     */
    public boolean isActiveSafe() {
        return active != null && active;
    }

    @Override
    public String toString() {
        // Used by adapters / debugging
        return getDisplayName();
    }
}
