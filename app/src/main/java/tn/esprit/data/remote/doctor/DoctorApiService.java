package tn.esprit.data.remote.doctor;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;
import retrofit2.http.PUT;
import retrofit2.http.Query;
import tn.esprit.domain.doctor.DoctorProfile;
import tn.esprit.domain.doctor.DoctorPublicProfile;
import tn.esprit.domain.doctor.DoctorSearchResult;

public interface DoctorApiService {

    /**
     * GET /api/doctors/me
     * Returns the current doctor's profile.
     */
    @GET("/api/doctors/me")
    Call<DoctorProfile> getMyProfile(
            @Header("Authorization") String authHeader
    );

    /**
     * PUT /api/doctors/me
     * Updates the current doctor's profile (bio, clinicAddress, etc.).
     */
    @PUT("/api/doctors/me")
    Call<DoctorProfile> updateMyProfile(
            @Header("Authorization") String authHeader,
            @Body DoctorProfileUpdateRequestDto request
    );

    /**
     * PUT /api/doctors/me/practice-setup
     * Onboarding step: choose one specialty and the acts performed.
     */
    @PUT("/api/doctors/me/practice-setup")
    Call<DoctorProfile> setupPracticeForCurrentDoctor(
            @Header("Authorization") String authHeader,
            @Body DoctorPracticeSetupRequestDto request
    );

    // ----------- NEW: public/search endpoints -----------

    /**
     * Public search endpoint for doctors.
     *
     * GET /api/doctors/search
     *
     * All query parameters are optional. Passing null for a parameter
     * simply omits it from the request.
     */
    @GET("/api/doctors/search")
    Call<List<DoctorSearchResult>> searchDoctors(
            @Header("Authorization") String authHeader,
            @Query("q") String query,
            @Query("specialtyId") Long specialtyId,
            @Query("city") String city,
            @Query("country") String country,
            @Query("teleconsultationEnabled") Boolean teleconsultationEnabled,
            @Query("acceptingNewPatients") Boolean acceptingNewPatients
    );

    /**
     * Public doctor profile endpoint.
     *
     * GET /api/doctors/{doctorId}/public
     */
    @GET("/api/doctors/{doctorId}/public")
    Call<DoctorPublicProfile> getDoctorPublicProfile(
            @Header("Authorization") String authHeader,
            @Path("doctorId") Long doctorId
    );

    // ----------- DTOs used as request bodies -----------

    /**
     * Mirrors backend DoctorProfileUpdateRequest.
     */
    class DoctorProfileUpdateRequestDto {

        private String bio;
        private Integer yearsOfExperience;
        private String clinicAddress;
        private String city;
        private String country;
        private String medicalRegistrationNumber;
        private java.math.BigDecimal consultationFee;
        private Boolean acceptsNewPatients;
        private Boolean teleconsultationEnabled;
        private Integer maxDailyAppointments;
        private Integer averageConsultationDurationMinutes;

        // --- getters & setters ---

        public String getBio() {
            return bio;
        }

        public void setBio(String bio) {
            this.bio = bio;
        }

        public Integer getYearsOfExperience() {
            return yearsOfExperience;
        }

        public void setYearsOfExperience(Integer yearsOfExperience) {
            this.yearsOfExperience = yearsOfExperience;
        }

        public String getClinicAddress() {
            return clinicAddress;
        }

        public void setClinicAddress(String clinicAddress) {
            this.clinicAddress = clinicAddress;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public String getCountry() {
            return country;
        }

        public void setCountry(String country) {
            this.country = country;
        }

        public String getMedicalRegistrationNumber() {
            return medicalRegistrationNumber;
        }

        public void setMedicalRegistrationNumber(String medicalRegistrationNumber) {
            this.medicalRegistrationNumber = medicalRegistrationNumber;
        }

        public java.math.BigDecimal getConsultationFee() {
            return consultationFee;
        }

        public void setConsultationFee(java.math.BigDecimal consultationFee) {
            this.consultationFee = consultationFee;
        }

        public Boolean getAcceptsNewPatients() {
            return acceptsNewPatients;
        }

        public void setAcceptsNewPatients(Boolean acceptsNewPatients) {
            this.acceptsNewPatients = acceptsNewPatients;
        }

        public Boolean getTeleconsultationEnabled() {
            return teleconsultationEnabled;
        }

        public void setTeleconsultationEnabled(Boolean teleconsultationEnabled) {
            this.teleconsultationEnabled = teleconsultationEnabled;
        }

        public Integer getMaxDailyAppointments() {
            return maxDailyAppointments;
        }

        public void setMaxDailyAppointments(Integer maxDailyAppointments) {
            this.maxDailyAppointments = maxDailyAppointments;
        }

        public Integer getAverageConsultationDurationMinutes() {
            return averageConsultationDurationMinutes;
        }

        public void setAverageConsultationDurationMinutes(Integer averageConsultationDurationMinutes) {
            this.averageConsultationDurationMinutes = averageConsultationDurationMinutes;
        }
    }

    /**
     * Mirrors backend DoctorPracticeSetupRequest.
     */
    class DoctorPracticeSetupRequestDto {
        public Long specialtyId;
        public java.util.List<Long> actIds;
    }
}
