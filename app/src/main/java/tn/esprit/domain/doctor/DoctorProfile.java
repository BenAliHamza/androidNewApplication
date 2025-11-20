package tn.esprit.domain.doctor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Domain model for the doctor's profile, matches backend dto.doctor.DoctorProfileDto.
 *
 * Used directly as Retrofit response type (no extra mapping).
 */
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
    private List<Specialty> specialties;
    private List<Act> acts;

    // Needed by Gson / Retrofit
    public DoctorProfile() {
    }

    public DoctorProfile(Long id,
                         Long userId,
                         String firstname,
                         String lastname,
                         String email,
                         String phone,
                         String bio,
                         Integer yearsOfExperience,
                         String clinicAddress,
                         String city,
                         String country,
                         String medicalRegistrationNumber,
                         BigDecimal consultationFee,
                         Boolean acceptsNewPatients,
                         Boolean teleconsultationEnabled,
                         Integer maxDailyAppointments,
                         Integer averageConsultationDurationMinutes,
                         List<Specialty> specialties,
                         List<Act> acts) {
        this.id = id;
        this.userId = userId;
        this.firstname = firstname;
        this.lastname = lastname;
        this.email = email;
        this.phone = phone;
        this.bio = bio;
        this.yearsOfExperience = yearsOfExperience;
        this.clinicAddress = clinicAddress;
        this.city = city;
        this.country = country;
        this.medicalRegistrationNumber = medicalRegistrationNumber;
        this.consultationFee = consultationFee;
        this.acceptsNewPatients = acceptsNewPatients;
        this.teleconsultationEnabled = teleconsultationEnabled;
        this.maxDailyAppointments = maxDailyAppointments;
        this.averageConsultationDurationMinutes = averageConsultationDurationMinutes;
        this.specialties = specialties;
        this.acts = acts;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getFirstname() {
        return firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getBio() {
        return bio;
    }

    public Integer getYearsOfExperience() {
        return yearsOfExperience;
    }

    public String getClinicAddress() {
        return clinicAddress;
    }

    public String getCity() {
        return city;
    }

    public String getCountry() {
        return country;
    }

    public String getMedicalRegistrationNumber() {
        return medicalRegistrationNumber;
    }

    public BigDecimal getConsultationFee() {
        return consultationFee;
    }

    public Boolean getAcceptsNewPatients() {
        return acceptsNewPatients;
    }

    public Boolean getTeleconsultationEnabled() {
        return teleconsultationEnabled;
    }

    public Integer getMaxDailyAppointments() {
        return maxDailyAppointments;
    }

    public Integer getAverageConsultationDurationMinutes() {
        return averageConsultationDurationMinutes;
    }

    public List<Specialty> getSpecialties() {
        return specialties;
    }

    public List<Act> getActs() {
        return acts;
    }

    // --- Nested DTO-like models matching backend SpecialtyDto & ActDto ---

    public static class Specialty {
        private Long id;
        private String code;
        private String name;
        private String description;
        private Boolean active;

        public Specialty() {
        }

        public Specialty(Long id,
                         String code,
                         String name,
                         String description,
                         Boolean active) {
            this.id = id;
            this.code = code;
            this.name = name;
            this.description = description;
            this.active = active;
        }

        public Long getId() {
            return id;
        }

        public String getCode() {
            return code;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public Boolean getActive() {
            return active;
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

        public Act() {
        }

        public Act(Long id,
                   String code,
                   String name,
                   String description,
                   BigDecimal basePrice,
                   Integer defaultDurationMinutes,
                   Boolean teleconsultationAvailable) {
            this.id = id;
            this.code = code;
            this.name = name;
            this.description = description;
            this.basePrice = basePrice;
            this.defaultDurationMinutes = defaultDurationMinutes;
            this.teleconsultationAvailable = teleconsultationAvailable;
        }

        public Long getId() {
            return id;
        }

        public String getCode() {
            return code;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public BigDecimal getBasePrice() {
            return basePrice;
        }

        public Integer getDefaultDurationMinutes() {
            return defaultDurationMinutes;
        }

        public Boolean getTeleconsultationAvailable() {
            return teleconsultationAvailable;
        }
    }
}
