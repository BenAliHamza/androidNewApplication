package tn.esprit.domain.appointment;

public class AvailabilitySessionResponse {

    private Long id;
    private int generatedSlotsCount;

    public AvailabilitySessionResponse() {
    }

    public Long getId() {
        return id;
    }

    public int getGeneratedSlotsCount() {
        return generatedSlotsCount;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setGeneratedSlotsCount(int generatedSlotsCount) {
        this.generatedSlotsCount = generatedSlotsCount;
    }
}
