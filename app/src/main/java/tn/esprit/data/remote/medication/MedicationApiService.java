package tn.esprit.data.remote.medication;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;
import tn.esprit.data.remote.common.ListResponseDto;
import tn.esprit.domain.medication.Medication;

/**
 * Retrofit API for medication-related endpoints.
 *
 * Mirrors backend MedicationController:
 *  - GET /api/medications
 *  - GET /api/medications/{id}
 *
 * Public catalog-style endpoints: no Authorization header required.
 */
public interface MedicationApiService {

    /**
     * GET /api/medications?q=...
     *
     * Returns list of medications, optionally filtered by query.
     */
    @GET("/api/medications")
    Call<ListResponseDto<Medication>> getMedications(
            @Query("q") String query
    );

    /**
     * GET /api/medications/{id}
     *
     * Returns a single medication by id.
     */
    @GET("/api/medications/{id}")
    Call<Medication> getMedication(
            @Path("id") long id
    );
}
