package tn.esprit.domain.indicator;

/**
 * Domain model mirroring backend dto.indicator.IndicatorTypeDto.
 *
 * Fields:
 *  - id
 *  - code (e.g. "BP_SYS", "HR", "GLUCOSE")
 *  - name (human friendly)
 *  - unit (e.g. "mmHg", "bpm", "mg/dL")
 *  - description
 *  - active (if this type is currently usable)
 */
public class IndicatorType {

    private Long id;
    private String code;
    private String name;
    private String unit;
    private String description;
    private Boolean active;

    public IndicatorType() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
