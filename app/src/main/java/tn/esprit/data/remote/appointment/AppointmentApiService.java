package tn.esprit.data.remote.appointment;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;
import tn.esprit.domain.appointment.AvailabilitySessionResponse;
import tn.esprit.domain.appointment.Slot;

public interface AppointmentApiService {

    @POST("api/doctors/me/availability")
    Call<AvailabilitySessionResponse> createAvailability(
            @Header("Authorization") String bearerToken,
            @Body AvailabilitySessionRequest request
    );

    @GET("api/doctors/me/slots")
    Call<List<Slot>> getDoctorSlots(
            @Header("Authorization") String bearerToken,
            @Query("from") String from,
            @Query("to") String to,
            @Query("status") String status
    );

    @GET("api/slots/available")
    Call<List<Slot>> getAvailableSlots(
            @Header("Authorization") String bearerToken,
            @Query("doctorId") Long doctorId,
            @Query("from") String from,
            @Query("to") String to
    );

    @POST("api/appointments/book")
    Call<Slot> bookSlot(
            @Header("Authorization") String bearerToken,
            @Body AppointmentBookingRequest request
    );
}
