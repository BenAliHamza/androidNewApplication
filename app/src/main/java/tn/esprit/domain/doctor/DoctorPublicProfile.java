package tn.esprit.domain.doctor;

import java.math.BigDecimal;
import java.util.List;

public class DoctorPublicProfile {

    private Long doctorId;
    private Long userId;

    // Basic identity
    private String firstName;
    private String lastName;
    private String profileImageUrl;

    // Professional info
    private String specialtyName;
    private Long specialtyId;
    private String city;
    private String country;
    private String clinicAddress;
    private String bio;
    private Integer yearsOfExperience;
    private BigDecimal consultationFee;
    private Boolean acceptingNewPatients;
    private Boolean teleconsultationEnabled;

    // Acts
    private List<Act> acts;

    public DoctorPublicProfile() {
    }

    public Long getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Long doctorId) {
        this.doctorId = doctorId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public String getSpecialtyName() {
        return specialtyName;
    }

    public void setSpecialtyName(String specialtyName) {
        this.specialtyName = specialtyName;
    }

    public Long getSpecialtyId() {
        return specialtyId;
    }

    public void setSpecialtyId(Long specialtyId) {
        this.specialtyId = specialtyId;
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

    public String getClinicAddress() {
        return clinicAddress;
    }

    public void setClinicAddress(String clinicAddress) {
        this.clinicAddress = clinicAddress;
    }

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

    public BigDecimal getConsultationFee() {
        return consultationFee;
    }

    public void setConsultationFee(BigDecimal consultationFee) {
        this.consultationFee = consultationFee;
    }

    public Boolean getAcceptingNewPatients() {
        return acceptingNewPatients;
    }

    public void setAcceptingNewPatients(Boolean acceptingNewPatients) {
        this.acceptingNewPatients = acceptingNewPatients;
    }

    public Boolean getTeleconsultationEnabled() {
        return teleconsultationEnabled;
    }

    public void setTeleconsultationEnabled(Boolean teleconsultationEnabled) {
        this.teleconsultationEnabled = teleconsultationEnabled;
    }

    public List<Act> getActs() {
        return acts;
    }

    public void setActs(List<Act> acts) {
        this.acts = acts;
    }

    public String getFullName() {
        String first = firstName != null ? firstName.trim() : "";
        String last = lastName != null ? lastName.trim() : "";
        String combined = (first + " " + last).trim();
        return combined.isEmpty() ? "" : combined;
    }

    // Nested Act model mirrors ActDto in backend
    public static class Act {
        private Long id;
        private String code;
        private String name;
        private String description;
        private BigDecimal basePrice;
        private Integer defaultDurationMinutes;
        private Boolean teleconsultationAvailable;

        public Act() {
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public BigDecimal getBasePrice() {
            return basePrice;
        }

        public void setBasePrice(BigDecimal basePrice) {
            this.basePrice = basePrice;
        }

        public Integer getDefaultDurationMinutes() {
            return defaultDurationMinutes;
        }

        public void setDefaultDurationMinutes(Integer defaultDurationMinutes) {
            this.defaultDurationMinutes = defaultDurationMinutes;
        }

        public Boolean getTeleconsultationAvailable() {
            return teleconsultationAvailable;
        }

        public void setTeleconsultationAvailable(Boolean teleconsultationAvailable) {
            this.teleconsultationAvailable = teleconsultationAvailable;
        }
    }
}
