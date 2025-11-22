package tn.esprit.domain.doctor;

import java.math.BigDecimal;

public class Act {

    private Long id;
    private String code;
    private String name;
    private String description;
    private BigDecimal basePrice;
    private Integer defaultDurationMinutes;
    private Boolean teleconsultationAvailable;

    public Act() {
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public Integer getDefaultDurationMinutes() {
        return defaultDurationMinutes;
    }

    public void setDefaultDurationMinutes(Integer defaultDurationMinutes) {
        this.defaultDurationMinutes = defaultDurationMinutes;
    }

    public Boolean getTeleconsultationAvailable() {
        return teleconsultationAvailable;
    }

    public void setTeleconsultationAvailable(Boolean teleconsultationAvailable) {
        this.teleconsultationAvailable = teleconsultationAvailable;
    }
}
