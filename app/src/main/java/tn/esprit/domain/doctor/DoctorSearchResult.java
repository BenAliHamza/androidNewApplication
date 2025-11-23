package tn.esprit.domain.doctor;

public class DoctorSearchResult {

    private Long doctorId;
    private Long userId;

    private String firstName;
    private String lastName;

    private String specialtyName;
    private Long specialtyId;

    private String city;
    private String country;

    private String profileImageUrl;

    private Boolean teleconsultationEnabled;
    private Boolean acceptingNewPatients;

    public DoctorSearchResult() {
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

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public Boolean getTeleconsultationEnabled() {
        return teleconsultationEnabled;
    }

    public void setTeleconsultationEnabled(Boolean teleconsultationEnabled) {
        this.teleconsultationEnabled = teleconsultationEnabled;
    }

    public Boolean getAcceptingNewPatients() {
        return acceptingNewPatients;
    }

    public void setAcceptingNewPatients(Boolean acceptingNewPatients) {
        this.acceptingNewPatients = acceptingNewPatients;
    }

    // Convenience for UI formatting (Lastname F.)
    public String getDisplayNameCompact() {
        String last = lastName != null ? lastName.trim() : "";
        String firstInitial = (firstName != null && !firstName.trim().isEmpty())
                ? firstName.trim().substring(0, 1).toUpperCase()
                : "";
        if (!last.isEmpty() && !firstInitial.isEmpty()) {
            return last + " " + firstInitial + ".";
        }
        if (!last.isEmpty()) return last;
        if (firstName != null) return firstName;
        return "";
    }
}
