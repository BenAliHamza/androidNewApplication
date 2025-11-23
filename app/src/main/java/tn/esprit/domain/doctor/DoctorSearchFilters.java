package tn.esprit.domain.doctor;

public class DoctorSearchFilters {

    private String query;
    private Long specialtyId;
    private String city;
    private String country;
    private Boolean teleconsultationEnabled;
    private Boolean acceptingNewPatients;

    public DoctorSearchFilters() {
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
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

    public static DoctorSearchFilters fromQuery(String query) {
        DoctorSearchFilters filters = new DoctorSearchFilters();
        filters.setQuery(query);
        return filters;
    }
}
