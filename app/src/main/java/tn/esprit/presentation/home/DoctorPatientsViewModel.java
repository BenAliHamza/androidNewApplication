package tn.esprit.presentation.home;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.List;

import tn.esprit.R;
import tn.esprit.data.doctor.DoctorPatientsRepository;
import tn.esprit.domain.patient.PatientProfile;

/**
 * ViewModel for DoctorPatientsFragment.
 *
 * Responsibilities:
 *  - load patients of current doctor
 *  - expose loading & error state
 *  - later: handle remove patient action.
 */
public class DoctorPatientsViewModel extends AndroidViewModel {

    private final DoctorPatientsRepository repository;

    private final MutableLiveData<List<PatientProfile>> patients =
            new MutableLiveData<>(new ArrayList<>());

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>(null);

    public DoctorPatientsViewModel(@NonNull Application application) {
        super(application);
        repository = new DoctorPatientsRepository(application.getApplicationContext());
    }

    public LiveData<List<PatientProfile>> getPatients() {
        return patients;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    /**
     * Triggers loading of current doctor's patients.
     */
    public void loadPatients() {
        loading.setValue(true);
        errorMessage.setValue(null);

        repository.getMyPatients(new DoctorPatientsRepository.LoadPatientsCallback() {
            @Override
            public void onSuccess(List<PatientProfile> list) {
                loading.postValue(false);
                if (list == null) {
                    patients.postValue(new ArrayList<>());
                } else {
                    patients.postValue(new ArrayList<>(list));
                }
            }

            @Override
            public void onError(Throwable throwable,
                                Integer httpCode,
                                String errorBody) {
                loading.postValue(false);

                String msg = getApplication().getString(R.string.doctor_patients_error_generic);
                // You could specialize on httpCode / errorBody later if needed.

                errorMessage.postValue(msg);
            }
        });
    }
}
