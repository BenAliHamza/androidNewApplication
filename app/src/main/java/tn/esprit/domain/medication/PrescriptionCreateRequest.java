package tn.esprit.domain.medication;

import java.util.List;

/**
 * Domain model mirroring backend dto.medication.PrescriptionCreateRequest.
 *
 * Fields:
 *  - startDate (ISO yyyy-MM-dd)
 *  - endDate   (ISO yyyy-MM-dd)
 *  - note
 *  - lines: list of line create requests
 */
public class PrescriptionCreateRequest {

    private String startDate;
    private String endDate;
    private String note;
    private List<PrescriptionLineCreateRequest> lines;

    public PrescriptionCreateRequest() {
    }

    public PrescriptionCreateRequest(String startDate,
                                     String endDate,
                                     String note,
                                     List<PrescriptionLineCreateRequest> lines) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.note = note;
        this.lines = lines;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public List<PrescriptionLineCreateRequest> getLines() {
        return lines;
    }

    public void setLines(List<PrescriptionLineCreateRequest> lines) {
        this.lines = lines;
    }
}
