package tn.esprit.presentation.profile;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.math.BigDecimal;

import tn.esprit.data.profile.ProfileRepository;
import tn.esprit.domain.doctor.DoctorProfile;
import tn.esprit.domain.patient.PatientProfile;
import tn.esprit.domain.user.User;

/**
 * ViewModel that loads the current user's profile from the backend
 * using ProfileRepository.
 *
 * It exposes:
 *  - base User (from /me)
 *  - DoctorProfile (from /api/doctors/me) when role == DOCTOR
 *  - PatientProfile (from /api/patients/me) when role == PATIENT
 *
 * There is NO hard-coded profile data here – everything comes from the backend.
 */
public class ProfileViewModel extends AndroidViewModel {

    private final ProfileRepository profileRepository;

    private final MutableLiveData<User> user = new MutableLiveData<>();
    private final MutableLiveData<DoctorProfile> doctorProfile = new MutableLiveData<>();
    private final MutableLiveData<PatientProfile> patientProfile = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public ProfileViewModel(@NonNull Application application) {
        super(application);
        profileRepository = new ProfileRepository(application.getApplicationContext());
    }

    public LiveData<User> getUser() {
        return user;
    }

    public LiveData<DoctorProfile> getDoctorProfile() {
        return doctorProfile;
    }

    public LiveData<PatientProfile> getPatientProfile() {
        return patientProfile;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    /**
     * Loads the current user's profile from the backend.
     * Internally uses:
     *  - GET /me
     *  - GET /api/doctors/me         (if role == DOCTOR)
     *  - GET /api/patients/me        (if role == PATIENT)
     *
     * Behavior is 100% role-based and driven by real data.
     */
    public void loadProfile() {
        if (Boolean.TRUE.equals(loading.getValue())) {
            // avoid double calls
            return;
        }

        loading.setValue(true);
        errorMessage.setValue(null);

        profileRepository.loadProfile(new ProfileRepository.ProfileCallback() {
            @Override
            public void onSuccess(User u,
                                  DoctorProfile d,
                                  PatientProfile p) {
                loading.postValue(false);
                user.postValue(u);
                doctorProfile.postValue(d);
                patientProfile.postValue(p);
            }

            @Override
            public void onError(Throwable throwable,
                                Integer httpCode,
                                String errorBody) {
                loading.postValue(false);

                String msg;
                if (throwable != null) {
                    msg = "Network error while loading profile.";
                } else if (httpCode != null) {
                    msg = "Server error " + httpCode;
                } else {
                    msg = "Unknown error while loading profile.";
                }

                if (errorBody != null && !errorBody.isEmpty()) {
                    msg = msg + " " + errorBody;
                }

                errorMessage.postValue(msg);
            }
        });
    }

    /**
     * Allows updating the in-memory User (from /me) after:
     *  - PUT /me (base info update),
     *  - profile image upload, etc.
     */
    public void setUser(User updatedUser) {
        if (updatedUser == null) return;
        user.setValue(updatedUser);
    }

    /**
     * Updates the in-memory doctor profile after an edit.
     * Call this after a successful backend update for doctor profiles.
     */
    public void updateBasicInfo(
            String displayName,
            String phone,
            String clinic,
            String city,
            String country,
            String regNumber,
            BigDecimal fee,
            String bio,
            boolean acceptsNew,
            boolean teleconsultation
    ) {
        DoctorProfile current = doctorProfile.getValue();
        if (current == null) {
            current = new DoctorProfile();
        }

        // Split displayName into first / last name when possible
        if (displayName != null && !displayName.trim().isEmpty()) {
            String[] parts = displayName.trim().split(" ", 2);
            current.setFirstname(parts[0]);
            if (parts.length > 1) {
                current.setLastname(parts[1]);
            }
        }

        current.setPhone(phone);
        current.setClinicAddress(clinic);
        current.setCity(city);
        current.setCountry(country);
        current.setMedicalRegistrationNumber(regNumber);
        current.setConsultationFee(fee);
        current.setBio(bio);
        current.setAcceptsNewPatients(acceptsNew);
        current.setTeleconsultationEnabled(teleconsultation);

        doctorProfile.setValue(current);
    }
}
