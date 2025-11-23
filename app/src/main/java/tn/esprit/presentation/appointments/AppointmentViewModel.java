package tn.esprit.presentation.appointments;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import tn.esprit.data.appointment.AppointmentRepository;
import tn.esprit.data.remote.appointment.AppointmentCreateRequest;
import tn.esprit.data.remote.appointment.AppointmentDto;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AppointmentViewModel extends ViewModel {
    private AppointmentRepository repo;

    private final MutableLiveData<List<AppointmentDto>> appointments = new MutableLiveData<>();
    public LiveData<List<AppointmentDto>> getAppointments() { return appointments; }

    private final MutableLiveData<AppointmentDto> appointmentActionResult = new MutableLiveData<>();
    public LiveData<AppointmentDto> getAppointmentActionResult() { return appointmentActionResult; }

    private final MutableLiveData<String> error = new MutableLiveData<>();
    public LiveData<String> getError() { return error; }

    public void init(Context context) {
        repo = new AppointmentRepository(context);
    }

    // PATIENT : créer un RDV
    public void requestAppointment(AppointmentCreateRequest req) {
        repo.requestAppointment(req).enqueue(new Callback<AppointmentDto>() {
            @Override
            public void onResponse(Call<AppointmentDto> call, Response<AppointmentDto> response) {
                if (response.isSuccessful()) {
                    appointmentActionResult.postValue(response.body());
                } else {
                    error.postValue("Erreur demande RDV");
                }
            }
            @Override
            public void onFailure(Call<AppointmentDto> call, Throwable t) {
                error.postValue(t.getMessage());
            }
        });
    }

    // PATIENT : liste de ses RDV
    public void loadPatientAppointments() { /* idem avec repo.getMyAppointmentsAsPatient() */ }

    // DOCTOR : liste de ses RDV
    public void loadDoctorAppointments(String status, String fromDate, String toDate) { /* idem */ }

    public void accept(Long id) { actionHelper(repo.accept(id)); }
    public void reject(Long id) { actionHelper(repo.reject(id)); }
    public void complete(Long id) { actionHelper(repo.complete(id)); }

    private void actionHelper(Call<AppointmentDto> call) { /* gestion réponse */ }

}
