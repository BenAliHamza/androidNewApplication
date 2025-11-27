package tn.esprit.domain.appointment;

/**
 * Request body to update an appointment status.
 * Matches backend AppointmentStatusUpdateRequest (status: ACCEPTED / REJECTED / COMPLETED).
 */
public class AppointmentStatusUpdateRequest {

    private String status;

    public AppointmentStatusUpdateRequest() {
    }

    public AppointmentStatusUpdateRequest(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
