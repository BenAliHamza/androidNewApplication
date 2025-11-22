package tn.esprit.data.remote.specialty;

import java.math.BigDecimal;
import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;

public interface SpecialtyApiService {

    @GET("/api/specialties")
    Call<List<SpecialtyDto>> getAllSpecialties(
            @Header("Authorization") String authHeader
    );

    @GET("/api/specialties/{specialtyId}/acts")
    Call<List<ActDto>> getActsBySpecialty(
            @Header("Authorization") String authHeader,
            @Path("specialtyId") Long specialtyId
    );

    // --- DTOs mirroring your backend ---

    public static class SpecialtyDto {
        public Long id;
        public String code;
        public String name;
        public String description;
        public Boolean active;
    }

    public static class ActDto {
        public Long id;
        public String code;
        public String name;
        public String description;
        public BigDecimal basePrice;
        public Integer defaultDurationMinutes;
        public Boolean teleconsultationAvailable;
    }
}
