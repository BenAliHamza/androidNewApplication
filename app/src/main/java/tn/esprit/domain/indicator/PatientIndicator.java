package tn.esprit.domain.indicator;

import java.math.BigDecimal;

/**
 * Domain model mirroring backend dto.indicator.PatientIndicatorDto.
 *
 * NOTE:
 *  - measuredAt is represented as a raw ISO-8601 String as returned by backend
 *    (e.g. "2025-02-03T10:15:30"), so we don't depend on Java time parsing on Android.
 */
public class PatientIndicator {

    private Long id;

    private Long indicatorTypeId;
    private String indicatorCode;
    private String indicatorName;
    private String unit;

    private BigDecimal numericValue;
    private String textValue;

    /**
     * ISO date-time string from the backend, e.g. "2025-02-03T10:15:30".
     */
    private String measuredAt;

    private String note;

    public PatientIndicator() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getIndicatorTypeId() {
        return indicatorTypeId;
    }

    public void setIndicatorTypeId(Long indicatorTypeId) {
        this.indicatorTypeId = indicatorTypeId;
    }

    public String getIndicatorCode() {
        return indicatorCode;
    }

    public void setIndicatorCode(String indicatorCode) {
        this.indicatorCode = indicatorCode;
    }

    public String getIndicatorName() {
        return indicatorName;
    }

    public void setIndicatorName(String indicatorName) {
        this.indicatorName = indicatorName;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public BigDecimal getNumericValue() {
        return numericValue;
    }

    public void setNumericValue(BigDecimal numericValue) {
        this.numericValue = numericValue;
    }

    public String getTextValue() {
        return textValue;
    }

    public void setTextValue(String textValue) {
        this.textValue = textValue;
    }

    public String getMeasuredAt() {
        return measuredAt;
    }

    public void setMeasuredAt(String measuredAt) {
        this.measuredAt = measuredAt;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
