package tn.esprit.data.remote.appointment;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;
import tn.esprit.data.remote.common.ListResponseDto;
import tn.esprit.domain.appointment.Appointment;
import tn.esprit.domain.appointment.AppointmentCreateRequest;
import tn.esprit.domain.appointment.AppointmentStatusUpdateRequest;
import tn.esprit.domain.appointment.WeeklyCalendarResponse;
import tn.esprit.domain.doctor.DoctorHomeStats;

public interface AppointmentApiService {

    @POST("api/appointments")
    Call<Appointment> createAppointment(
            @Header("Authorization") String authorization,
            @Body AppointmentCreateRequest request
    );

    @GET("api/appointments/me")
    Call<ListResponseDto<Appointment>> getMyAppointments(
            @Header("Authorization") String authorization,
            @Query("from") String fromIso,
            @Query("to") String toIso
    );

    @GET("api/doctors/me/appointments")
    Call<ListResponseDto<Appointment>> getDoctorAppointments(
            @Header("Authorization") String authorization,
            @Query("from") String fromIso,
            @Query("to") String toIso
    );

    @DELETE("api/appointments/{id}")
    Call<Void> cancelAppointment(
            @Header("Authorization") String authorization,
            @Path("id") long id
    );

    @PATCH("api/appointments/{id}/status")
    Call<Appointment> updateAppointmentStatus(
            @Header("Authorization") String authorization,
            @Path("id") long id,
            @Body AppointmentStatusUpdateRequest request
    );

    @PUT("api/appointments/{id}")
    Call<Appointment> rescheduleAppointment(
            @Header("Authorization") String authorization,
            @Path("id") long id,
            @Body AppointmentCreateRequest request
    );

    @GET("api/doctors/{doctorId}/weekly-calendar")
    Call<WeeklyCalendarResponse> getDoctorWeeklyCalendar(
            @Header("Authorization") String authorization,
            @Path("doctorId") long doctorId,
            @Query("weekStart") String weekStartIso
    );

    @GET("api/doctors/me/home-stats")
    Call<DoctorHomeStats> getDoctorHomeStats(
            @Header("Authorization") String authorization
    );

}
