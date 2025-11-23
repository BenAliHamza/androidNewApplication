package tn.esprit.data.remote.appointment;

public class AppointmentBookingRequest {
    public Long slotId;
    public String reason;

    public AppointmentBookingRequest(Long slotId, String reason) {
        this.slotId = slotId;
        this.reason = reason;
    }
}
