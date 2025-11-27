package tn.esprit.data.remote.doctor;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import tn.esprit.domain.doctor.DoctorHomeStats;

public interface DoctorStatsApiService {

    @GET("api/doctors/me/stats")
    Call<DoctorHomeStats> getDoctorStats(
            @Header("Authorization") String auth
    );
}
