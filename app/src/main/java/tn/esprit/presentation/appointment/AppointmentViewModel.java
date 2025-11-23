package tn.esprit.presentation.appointment;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import tn.esprit.data.appointment.AppointmentRepository;
import tn.esprit.data.remote.appointment.AppointmentBookingRequest;
import tn.esprit.data.remote.appointment.AvailabilitySessionRequest;
import tn.esprit.domain.appointment.AvailabilitySessionResponse;
import tn.esprit.domain.appointment.Slot;

public class AppointmentViewModel extends ViewModel {

    private final AppointmentRepository repo = new AppointmentRepository();

    public MutableLiveData<List<Slot>> doctorSlots = new MutableLiveData<>();
    public MutableLiveData<List<Slot>> availableSlots = new MutableLiveData<>();
    public MutableLiveData<Slot> bookedSlot = new MutableLiveData<>();
    public MutableLiveData<AvailabilitySessionResponse> createdAvailability = new MutableLiveData<>();

    public void createAvailability(String token, AvailabilitySessionRequest req) {
        repo.createAvailability(token, req).enqueue(new Callback<AvailabilitySessionResponse>() {
            @Override public void onResponse(Call<AvailabilitySessionResponse> call, Response<AvailabilitySessionResponse> r) {
                if (r.isSuccessful()) createdAvailability.setValue(r.body());
            }
            @Override public void onFailure(Call<AvailabilitySessionResponse> call, Throwable t) {}
        });
    }

    public void loadDoctorSlots(String token, String from, String to, String status) {
        repo.getDoctorSlots(token, from, to, status).enqueue(new Callback<List<Slot>>() {
            @Override public void onResponse(Call<List<Slot>> call, Response<List<Slot>> r) {
                if (r.isSuccessful()) doctorSlots.setValue(r.body());
            }
            @Override public void onFailure(Call<List<Slot>> call, Throwable t) {}
        });
    }

    public void loadAvailableSlots(String token, Long doctorId, String from, String to) {
        repo.getAvailableSlots(token, doctorId, from, to).enqueue(new Callback<List<Slot>>() {
            @Override public void onResponse(Call<List<Slot>> call, Response<List<Slot>> r) {
                if (r.isSuccessful()) availableSlots.setValue(r.body());
            }
            @Override public void onFailure(Call<List<Slot>> call, Throwable t) {}
        });
    }

    public void bookSlot(String token, AppointmentBookingRequest req) {
        repo.bookSlot(token, req).enqueue(new Callback<Slot>() {
            @Override public void onResponse(Call<Slot> call, Response<Slot> r) {
                if (r.isSuccessful()) bookedSlot.setValue(r.body());
            }
            @Override public void onFailure(Call<Slot> call, Throwable t) {}
        });
    }
}
