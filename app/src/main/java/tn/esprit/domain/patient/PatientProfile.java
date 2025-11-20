package tn.esprit.domain.patient;

/**
 * Domain model for the patient's profile, matches backend dto.patient.PatientProfileDto.
 *
 * Also used directly as Retrofit response type (no extra mapping).
 */
public class PatientProfile {

    private Long id;
    private Long userId;
    private String firstname;
    private String lastname;
    private String email;
    private String phone;
    private String dateOfBirth; // ISO date string from backend
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

    // Needed by Gson / Retrofit
    public PatientProfile() {
    }

    public PatientProfile(Long id,
                          Long userId,
                          String firstname,
                          String lastname,
                          String email,
                          String phone,
                          String dateOfBirth,
                          String gender,
                          String bloodType,
                          Integer heightCm,
                          Integer weightKg,
                          String address,
                          String city,
                          String country,
                          String maritalStatus,
                          Boolean smoker,
                          Boolean alcoholUse,
                          String notes) {
        this.id = id;
        this.userId = userId;
        this.firstname = firstname;
        this.lastname = lastname;
        this.email = email;
        this.phone = phone;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.bloodType = bloodType;
        this.heightCm = heightCm;
        this.weightKg = weightKg;
        this.address = address;
        this.city = city;
        this.country = country;
        this.maritalStatus = maritalStatus;
        this.smoker = smoker;
        this.alcoholUse = alcoholUse;
        this.notes = notes;
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

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public String getGender() {
        return gender;
    }

    public String getBloodType() {
        return bloodType;
    }

    public Integer getHeightCm() {
        return heightCm;
    }

    public Integer getWeightKg() {
        return weightKg;
    }

    public String getAddress() {
        return address;
    }

    public String getCity() {
        return city;
    }

    public String getCountry() {
        return country;
    }

    public String getMaritalStatus() {
        return maritalStatus;
    }

    public Boolean getSmoker() {
        return smoker;
    }

    public Boolean getAlcoholUse() {
        return alcoholUse;
    }

    public String getNotes() {
        return notes;
    }
}
