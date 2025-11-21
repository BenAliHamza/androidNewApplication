package tn.esprit.data.remote.patient;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PUT;
import tn.esprit.domain.patient.PatientProfile;

/**
 * Retrofit API for patient-specific operations.
 *
 * We directly return the domain PatientProfile model.
 */
public interface PatientApiService {

    /**
     * GET /patients/me
     * Authorization: Bearer <access_token>
     */
    @GET("patients/me")
    Call<PatientProfile> getMyProfile(@Header("Authorization") String authorization);

    /**
     * PUT /patients/me
     * Authorization: Bearer <access_token>
     *
     * Body matches backend dto.patient.PatientProfileUpdateRequest
     * (with dates formatted as ISO-8601 strings: yyyy-MM-dd).
     */
    @PUT("patients/me")
    Call<PatientProfile> updateMyProfile(
            @Header("Authorization") String authorization,
            @Body PatientProfileUpdateRequestDto request
    );

    /**
     * DTO mirroring backend PatientProfileUpdateRequest.
     * Used directly as Retrofit request body (no extra mapping).
     */
    class PatientProfileUpdateRequestDto {

        private String dateOfBirth; // ISO date string yyyy-MM-dd
        private String gender;
        private String bloodType;
        private Integer heightCm;
        private Integer weightKg;
        private String address;
        private String city;
        private String country;
        private String maritalStatus;
        private Boolean smoker;
        private Boolean alcoholUse;
        private String notes;

        public PatientProfileUpdateRequestDto() {
        }

        // Getters & setters

        public String getDateOfBirth() {
            return dateOfBirth;
        }

        public void setDateOfBirth(String dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
        }

        public String getGender() {
            return gender;
        }

        public void setGender(String gender) {
            this.gender = gender;
        }

        public String getBloodType() {
            return bloodType;
        }

        public void setBloodType(String bloodType) {
            this.bloodType = bloodType;
        }

        public Integer getHeightCm() {
            return heightCm;
        }

        public void setHeightCm(Integer heightCm) {
            this.heightCm = heightCm;
        }

        public Integer getWeightKg() {
            return weightKg;
        }

        public void setWeightKg(Integer weightKg) {
            this.weightKg = weightKg;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
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

        public String getMaritalStatus() {
            return maritalStatus;
        }

        public void setMaritalStatus(String maritalStatus) {
            this.maritalStatus = maritalStatus;
        }

        public Boolean getSmoker() {
            return smoker;
        }

        public void setSmoker(Boolean smoker) {
            this.smoker = smoker;
        }

        public Boolean getAlcoholUse() {
            return alcoholUse;
        }

        public void setAlcoholUse(Boolean alcoholUse) {
            this.alcoholUse = alcoholUse;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }
    }
}
