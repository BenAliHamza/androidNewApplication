package tn.esprit.domain.doctor;

import java.math.BigDecimal;
import java.util.List;

public class DoctorProfile {

    private Long id;
    private Long userId;

    private String firstname;
    private String lastname;
    private String email;
    private String phone;

    private String bio;
    private Integer yearsOfExperience;
    private String clinicAddress;
    private String city;
    private String country;
    private String medicalRegistrationNumber;
    private BigDecimal consultationFee;
    private Boolean acceptsNewPatients;
    private Boolean teleconsultationEnabled;
    private Integer maxDailyAppointments;
    private Integer averageConsultationDurationMinutes;

    private Specialty specialty;
    private List<Act> acts;

    // --------- Getters & setters ---------

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
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

    public BigDecimal getConsultationFee() {
        return consultationFee;
    }

    public void setConsultationFee(BigDecimal consultationFee) {
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

    public Specialty getSpecialty() {
        return specialty;
    }

    public void setSpecialty(Specialty specialty) {
        this.specialty = specialty;
    }

    public List<Act> getActs() {
        return acts;
    }

    public void setActs(List<Act> acts) {
        this.acts = acts;
    }

    // --------- Nested types (match backend DTO) ---------

    public static class Specialty {
        private Long id;
        private String code;
        private String name;
        private String description;
        private Boolean active;

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

        public Boolean getActive() {
            return active;
        }

        public void setActive(Boolean active) {
            this.active = active;
        }
    }

    public static class Act {
        private Long id;
        private String code;
        private String name;
        private String description;
        private BigDecimal basePrice;
        private Integer defaultDurationMinutes;
        private Boolean teleconsultationAvailable;

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
