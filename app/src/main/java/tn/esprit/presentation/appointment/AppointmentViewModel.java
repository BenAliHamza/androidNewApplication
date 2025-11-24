package tn.esprit.presentation.appointment;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import tn.esprit.data.remote.appointment.AppointmentApiService;
import tn.esprit.data.remote.appointment.AvailabilitySessionRequest;
import tn.esprit.domain.appointment.AvailabilitySessionResponse;
import tn.esprit.domain.appointment.Slot;
import tn.esprit.data.remote.RetrofitInstance;

public class AppointmentViewModel extends ViewModel {

    public MutableLiveData<AvailabilitySessionResponse> createdAvailability = new MutableLiveData<>();
    public MutableLiveData<Boolean> availabilityError = new MutableLiveData<>();
    public MutableLiveData<String> availabilityErrorMessage = new MutableLiveData<>();
    public MutableLiveData<List<Slot>> doctorSlots = new MutableLiveData<>();

    private final AppointmentApiService api =
            RetrofitInstance.getInstance().create(AppointmentApiService.class);

    public MutableLiveData<AvailabilitySessionResponse> getCreatedAvailability() {
        return createdAvailability;
    }

    public void createAvailability(String bearerToken, AvailabilitySessionRequest request) {
        api.createAvailability(bearerToken, request)
                .enqueue(new Callback<AvailabilitySessionResponse>() {
                    @Override
                    public void onResponse(Call<AvailabilitySessionResponse> call,
                                           Response<AvailabilitySessionResponse> response) {

                        if (response.isSuccessful() && response.body() != null) {
                            createdAvailability.postValue(response.body());
                        } else {
                            availabilityError.postValue(true);
                            availabilityErrorMessage.postValue("HTTP " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<AvailabilitySessionResponse> call, Throwable t) {
                        availabilityError.postValue(true);
                        availabilityErrorMessage.postValue(t.getMessage());
                    }
                });
    }

    public void loadDoctorSlots(String bearerToken, String from, String to, String status) {
        api.getDoctorSlots(bearerToken, from, to, status)
                .enqueue(new Callback<List<Slot>>() {
                    @Override
                    public void onResponse(Call<List<Slot>> call, Response<List<Slot>> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            doctorSlots.postValue(response.body());
                        }
                    }

                    @Override
                    public void onFailure(Call<List<Slot>> call, Throwable t) { }
                });
    }
}
