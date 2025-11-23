package tn.esprit.presentation.appointment;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.io.IOException;
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

    private static final String TAG = "AppointmentVM";

    private final AppointmentRepository repo = new AppointmentRepository();

    public MutableLiveData<List<Slot>> doctorSlots = new MutableLiveData<>();
    public MutableLiveData<List<Slot>> availableSlots = new MutableLiveData<>();
    public MutableLiveData<Slot> bookedSlot = new MutableLiveData<>();

    public MutableLiveData<AvailabilitySessionResponse> createdAvailability = new MutableLiveData<>();
    public MutableLiveData<Boolean> availabilityError = new MutableLiveData<>();
    public MutableLiveData<String> availabilityErrorMessage = new MutableLiveData<>();

    public void createAvailability(String token, AvailabilitySessionRequest req) {

        if (token == null || token.trim().isEmpty()) {
            availabilityError.setValue(true);
            availabilityErrorMessage.setValue("Missing access token (user not logged in?)");
            return;
        }

        repo.createAvailability(token, req).enqueue(new Callback<AvailabilitySessionResponse>() {
            @Override
            public void onResponse(Call<AvailabilitySessionResponse> call,
                                   Response<AvailabilitySessionResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    createdAvailability.setValue(response.body());
                    availabilityError.setValue(false);
                    availabilityErrorMessage.setValue(null);
                } else {
                    availabilityError.setValue(true);
                    StringBuilder sb = new StringBuilder();
                    sb.append("HTTP ").append(response.code());
                    try {
                        if (response.errorBody() != null) {
                            sb.append(" - ").append(response.errorBody().string());
                        }
                    } catch (IOException e) {
                        // ignore
                    }
                    String msg = sb.toString();
                    availabilityErrorMessage.setValue(msg);
                    Log.e(TAG, "createAvailability error: " + msg);
                }
            }

            @Override
            public void onFailure(Call<AvailabilitySessionResponse> call, Throwable t) {
                availabilityError.setValue(true);
                String msg = "Request failed: " + t.getMessage();
                availabilityErrorMessage.setValue(msg);
                Log.e(TAG, "createAvailability failure", t);
            }
        });
    }

    public void loadDoctorSlots(String token, String from, String to, String status) {
        repo.getDoctorSlots(token, from, to, status).enqueue(new Callback<List<Slot>>() {
            @Override
            public void onResponse(Call<List<Slot>> call, Response<List<Slot>> response) {
                if (response.isSuccessful()) {
                    doctorSlots.setValue(response.body());
                } else {
                    Log.e(TAG, "loadDoctorSlots HTTP " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<Slot>> call, Throwable t) {
                Log.e(TAG, "loadDoctorSlots failure", t);
            }
        });
    }

    public void loadAvailableSlots(String token, Long doctorId, String from, String to) {
        repo.getAvailableSlots(token, doctorId, from, to).enqueue(new Callback<List<Slot>>() {
            @Override
            public void onResponse(Call<List<Slot>> call, Response<List<Slot>> response) {
                if (response.isSuccessful()) {
                    availableSlots.setValue(response.body());
                } else {
                    Log.e(TAG, "loadAvailableSlots HTTP " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<Slot>> call, Throwable t) {
                Log.e(TAG, "loadAvailableSlots failure", t);
            }
        });
    }

    public void bookSlot(String token, AppointmentBookingRequest req) {
        repo.bookSlot(token, req).enqueue(new Callback<Slot>() {
            @Override
            public void onResponse(Call<Slot> call, Response<Slot> response) {
                if (response.isSuccessful()) {
                    bookedSlot.setValue(response.body());
                } else {
                    Log.e(TAG, "bookSlot HTTP " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Slot> call, Throwable t) {
                Log.e(TAG, "bookSlot failure", t);
            }
        });
    }
}
