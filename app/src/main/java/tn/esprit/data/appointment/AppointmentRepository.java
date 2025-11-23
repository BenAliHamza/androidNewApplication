package tn.esprit.data.appointment;

import java.util.List;

import retrofit2.Call;
import tn.esprit.data.remote.ApiClient;
import tn.esprit.data.remote.appointment.AppointmentApiService;
import tn.esprit.data.remote.appointment.AppointmentBookingRequest;
import tn.esprit.data.remote.appointment.AvailabilitySessionRequest;
import tn.esprit.domain.appointment.AvailabilitySessionResponse;
import tn.esprit.domain.appointment.Slot;

public class AppointmentRepository {

    private final AppointmentApiService apiService;

    public AppointmentRepository() {
        this.apiService = ApiClient.createService(AppointmentApiService.class);
    }

    public Call<AvailabilitySessionResponse> createAvailability(String bearerToken,
                                                                AvailabilitySessionRequest req) {
        return apiService.createAvailability(bearerToken, req);
    }

    public Call<List<Slot>> getDoctorSlots(String bearerToken,
                                           String from,
                                           String to,
                                           String status) {
        return apiService.getDoctorSlots(bearerToken, from, to, status);
    }

    public Call<List<Slot>> getAvailableSlots(String bearerToken,
                                              Long doctorId,
                                              String from,
                                              String to) {
        return apiService.getAvailableSlots(bearerToken, doctorId, from, to);
    }

    public Call<Slot> bookSlot(String bearerToken,
                               AppointmentBookingRequest req) {
        return apiService.bookSlot(bearerToken, req);
    }
}
