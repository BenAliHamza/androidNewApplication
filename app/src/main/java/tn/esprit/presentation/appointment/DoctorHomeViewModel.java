package tn.esprit.presentation.appointment;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import tn.esprit.R;
import tn.esprit.data.appointment.AppointmentRepository;
import tn.esprit.domain.doctor.DoctorHomeStats;

public class DoctorHomeViewModel extends AndroidViewModel {

    private final AppointmentRepository repository;

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<DoctorHomeStats> stats = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public DoctorHomeViewModel(@NonNull Application app) {
        super(app);
        repository = new AppointmentRepository(app.getApplicationContext());
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<DoctorHomeStats> getStats() {
        return stats;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void clearError() {
        errorMessage.setValue(null);
    }

    public void loadStats() {
        loading.setValue(true);
        errorMessage.setValue(null);

        repository.getDoctorHomeStats(new AppointmentRepository.HomeStatsCallback() {
            @Override
            public void onSuccess(@NonNull DoctorHomeStats result) {
                stats.postValue(result);
                loading.postValue(false);
            }

            @Override
            public void onError(@Nullable Throwable throwable,
                                @Nullable Integer httpCode,
                                @Nullable String errorBody) {
                loading.postValue(false);
                String msg = getApplication().getString(
                        R.string.doctor_appointments_error_generic
                );
                errorMessage.postValue(msg);
            }
        });
    }
}
