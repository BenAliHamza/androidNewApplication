package tn.esprit.data.appointment;

import java.util.List;

import retrofit2.Call;
import tn.esprit.data.remote.ApiClient;
import tn.esprit.data.remote.appointment.*;
import tn.esprit.domain.appointment.*;

public class AppointmentRepository {

    private final AppointmentApiService api =
            ApiClient.createService(AppointmentApiService.class);

    public Call<AvailabilitySessionResponse> createAvailability(
            String token, AvailabilitySessionRequest req) {
        return api.createAvailability("Bearer " + token, req);
    }

    public Call<List<Slot>> getDoctorSlots(
            String token, String from, String to, String status) {
        return api.getDoctorSlots("Bearer " + token, from, to, status);
    }

    public Call<List<Slot>> getAvailableSlots(
            String token, Long doctorId, String from, String to) {
        return api.getAvailableSlots("Bearer " + token, doctorId, from, to);
    }

    public Call<Slot> bookSlot(String token, AppointmentBookingRequest req) {
        return api.bookSlot("Bearer " + token, req);
    }
}
