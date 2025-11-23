package tn.esprit.data.remote.appointment;

import java.util.List;
import android.content.Context;

import retrofit2.Call;
import retrofit2.http.*;

import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface AppointmentApiService {

    @POST("api/appointments")
    Call<AppointmentDto> requestAppointment(@Body AppointmentCreateRequest request);

    @GET("api/appointments/me/patient")
    Call<List<AppointmentDto>> getMyAppointmentsAsPatient();

    @GET("api/appointments/me/doctor")
    Call<List<AppointmentDto>> getMyAppointmentsAsDoctor(
            @Query("status") String status,
            @Query("fromDate") String fromDate,
            @Query("toDate") String toDate
    );

    @PUT("api/appointments/{id}/accept")
    Call<AppointmentDto> accept(@Path("id") Long appointmentId);

    @PUT("api/appointments/{id}/reject")
    Call<AppointmentDto> reject(@Path("id") Long appointmentId);

    @PUT("api/appointments/{id}/complete")
    Call<AppointmentDto> complete(@Path("id") Long appointmentId);
}
