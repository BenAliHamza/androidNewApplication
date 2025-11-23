package tn.esprit.data.appointment;

import android.content.Context;
import retrofit2.Call;

import java.util.List;

import tn.esprit.data.remote.ApiClient;
import tn.esprit.data.remote.appointment.AppointmentApiService;
import tn.esprit.data.remote.appointment.AppointmentCreateRequest;
import tn.esprit.data.remote.appointment.AppointmentDto;
public class AppointmentRepository {
    private final AppointmentApiService api;

    public AppointmentRepository(Context context) {
        this.api = ApiClient.getAppointmentApiService(context);
    }

    public Call<AppointmentDto> requestAppointment(AppointmentCreateRequest req) {
        return api.requestAppointment(req);
    }

    public Call<List<AppointmentDto>> getMyAppointmentsAsPatient() {
        return api.getMyAppointmentsAsPatient();
    }

    public Call<List<AppointmentDto>> getMyAppointmentsAsDoctor(
            String status, String fromDate, String toDate
    ) {
        return api.getMyAppointmentsAsDoctor(status, fromDate, toDate);
    }

    public Call<AppointmentDto> accept(Long id) { return api.accept(id); }

    public Call<AppointmentDto> reject(Long id) { return api.reject(id); }

    public Call<AppointmentDto> complete(Long id) { return api.complete(id); }

}
