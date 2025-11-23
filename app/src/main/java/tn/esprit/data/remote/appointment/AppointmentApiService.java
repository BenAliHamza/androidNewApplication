package tn.esprit.data.remote.appointment;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.*;
import tn.esprit.domain.appointment.Slot;
import tn.esprit.domain.appointment.AvailabilitySessionResponse;

public interface AppointmentApiService {

    @POST("/api/doctors/me/availability")
    Call<AvailabilitySessionResponse> createAvailability(
            @Header("Authorization") String auth,
            @Body AvailabilitySessionRequest request
    );

    @GET("/api/doctors/me/slots")
    Call<List<Slot>> getDoctorSlots(
            @Header("Authorization") String auth,
            @Query("from") String from,
            @Query("to") String to,
            @Query("status") String status
    );

    @GET("/api/doctors/{doctorId}/slots")
    Call<List<Slot>> getAvailableSlots(
            @Header("Authorization") String auth,
            @Path("doctorId") Long doctorId,
            @Query("from") String from,
            @Query("to") String to
    );

    @POST("/api/patients/me/appointments")
    Call<Slot> bookSlot(
            @Header("Authorization") String auth,
            @Body AppointmentBookingRequest request
    );
}
