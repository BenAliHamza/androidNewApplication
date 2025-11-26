package tn.esprit.data.remote.medication;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;
import tn.esprit.data.remote.common.ListResponseDto;
import tn.esprit.domain.medication.Prescription;
import tn.esprit.domain.medication.PrescriptionCreateRequest;
import tn.esprit.domain.medication.PrescriptionLine;

/**
 * Retrofit API for prescription-related endpoints.
 *
 * Mirrors backend controllers:
 *  - DoctorPrescriptionController   (/api/doctors/me/...)
 *  - PatientPrescriptionController  (/api/prescriptions/me/...)
 *
 * NOTE:
 *  - Uses domain models directly as DTOs (Prescription, PrescriptionLine, request DTOs).
 */
public interface PrescriptionApiService {

    // ---------------------------------------------------------------------
    // Doctor: prescriptions for a patient
    // ---------------------------------------------------------------------

    /**
     * GET /api/doctors/me/patients/{patientUserId}/prescriptions?activeOnly=true
     *
     * Lists prescriptions of a given patient (by patient User.id)
     * for the current doctor.
     */
    @GET("/api/doctors/me/patients/{patientUserId}/prescriptions")
    Call<ListResponseDto<Prescription>> getPrescriptionsForPatientAsDoctor(
            @Header("Authorization") String authHeader,
            @Path("patientUserId") long patientUserId,
            @Query("activeOnly") Boolean activeOnly
    );

    /**
     * POST /api/doctors/me/patients/{patientUserId}/prescriptions
     *
     * Creates a prescription for a given patient (current doctor).
     * Body mirrors backend dto.medication.PrescriptionCreateRequest.
     */
    @POST("/api/doctors/me/patients/{patientUserId}/prescriptions")
    Call<Prescription> createPrescriptionForPatientAsDoctor(
            @Header("Authorization") String authHeader,
            @Path("patientUserId") long patientUserId,
            @Body PrescriptionCreateRequest body
    );

    /**
     * GET /api/doctors/me/prescriptions/{prescriptionId}
     *
     * Returns a single prescription owned by the current doctor.
     */
    @GET("/api/doctors/me/prescriptions/{prescriptionId}")
    Call<Prescription> getPrescriptionAsDoctor(
            @Header("Authorization") String authHeader,
            @Path("prescriptionId") long prescriptionId
    );

    /**
     * DELETE /api/doctors/me/prescriptions/{prescriptionId}
     *
     * Deletes a prescription (current doctor only).
     */
    @DELETE("/api/doctors/me/prescriptions/{prescriptionId}")
    Call<Void> deletePrescriptionAsDoctor(
            @Header("Authorization") String authHeader,
            @Path("prescriptionId") long prescriptionId
    );

    // ---------------------------------------------------------------------
    // Patient: own prescriptions & lines
    // ---------------------------------------------------------------------

    /**
     * GET /api/prescriptions/me?activeOnly=true
     *
     * Lists prescriptions of the current patient.
     */
    @GET("/api/prescriptions/me")
    Call<ListResponseDto<Prescription>> getMyPrescriptions(
            @Header("Authorization") String authHeader,
            @Query("activeOnly") Boolean activeOnly
    );

    /**
     * GET /api/prescriptions/me/{prescriptionId}
     *
     * Returns a single prescription of the current patient.
     */
    @GET("/api/prescriptions/me/{prescriptionId}")
    Call<Prescription> getMyPrescription(
            @Header("Authorization") String authHeader,
            @Path("prescriptionId") long prescriptionId
    );

    /**
     * GET /api/prescriptions/me/lines/active
     *
     * Returns active prescription lines for the current patient.
     */
    @GET("/api/prescriptions/me/lines/active")
    Call<ListResponseDto<PrescriptionLine>> getMyActiveLines(
            @Header("Authorization") String authHeader
    );

    /**
     * PATCH /api/prescriptions/me/lines/{lineId}/reminder
     *
     * Updates reminderEnabled flag for a specific line of the current patient.
     *
     * Body matches backend PrescriptionLineReminderUpdateRequest (just reminderEnabled).
     */
    @PATCH("/api/prescriptions/me/lines/{lineId}/reminder")
    Call<PrescriptionLine> updateMyLineReminder(
            @Header("Authorization") String authHeader,
            @Path("lineId") long lineId,
            @Body ReminderUpdateRequestDto body
    );

    // ---------------------------------------------------------------------
    // DTOs for request bodies
    // ---------------------------------------------------------------------

    /**
     * Mirrors backend dto.medication.PrescriptionLineReminderUpdateRequest.
     */
    class ReminderUpdateRequestDto {
        private Boolean reminderEnabled;

        public ReminderUpdateRequestDto() {
        }

        public ReminderUpdateRequestDto(Boolean reminderEnabled) {
            this.reminderEnabled = reminderEnabled;
        }

        public Boolean getReminderEnabled() {
            return reminderEnabled;
        }

        public void setReminderEnabled(Boolean reminderEnabled) {
            this.reminderEnabled = reminderEnabled;
        }
    }
}
