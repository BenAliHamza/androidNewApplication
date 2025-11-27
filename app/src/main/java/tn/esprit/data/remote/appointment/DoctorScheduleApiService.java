package tn.esprit.data.remote.appointment;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;
import tn.esprit.data.remote.common.ListResponseDto;
import tn.esprit.domain.appointment.DoctorSchedule;

public interface DoctorScheduleApiService {

    @GET("api/doctors/me/schedule")
    Call<ListResponseDto<DoctorSchedule>> getMySchedule(
            @Header("Authorization") String authHeader
    );

    @PUT("api/doctors/me/schedule")
    Call<ListResponseDto<DoctorSchedule>> updateMySchedule(
            @Header("Authorization") String authHeader,
            @Body List<DoctorSchedule> entries
    );

    @GET("api/doctors/{doctorId}/schedule")
    Call<ListResponseDto<DoctorSchedule>> getDoctorSchedule(
            @Header("Authorization") String authHeader,
            @Path("doctorId") Long doctorId
    );

    @GET("api/doctors/{doctorId}/available-slots")
    Call<ListResponseDto<String>> getDoctorAvailableSlots(
            @Header("Authorization") String authHeader,
            @Path("doctorId") Long doctorId,
            @Query("from") String fromIso,
            @Query("to") String toIso
    );
}
