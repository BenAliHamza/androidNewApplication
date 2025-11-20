package tn.esprit.data.remote.doctor;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import tn.esprit.domain.doctor.DoctorProfile;

/**
 * Retrofit API for doctor-specific operations.
 *
 * We directly return the domain DoctorProfile model.
 */
public interface DoctorApiService {

    /**
     * GET /doctors/me
     * Authorization: Bearer <access_token>
     */
    @GET("doctors/me")
    Call<DoctorProfile> getMyProfile(@Header("Authorization") String authorization);
}
