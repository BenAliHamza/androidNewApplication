package tn.esprit.data.remote.patient;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import tn.esprit.domain.patient.PatientProfile;

/**
 * Retrofit API for patient-specific operations.
 *
 * We directly return the domain PatientProfile model.
 */
public interface PatientApiService {

    /**
     * GET /patients/me
     * Authorization: Bearer <access_token>
     */
    @GET("patients/me")
    Call<PatientProfile> getMyProfile(@Header("Authorization") String authorization);
}
