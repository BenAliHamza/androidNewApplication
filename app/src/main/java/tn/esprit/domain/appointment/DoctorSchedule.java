package tn.esprit.domain.appointment;

/**
 * Mirrors backend dto.appointment.DoctorScheduleDto.
 *
 * Fields:
 *  - id
 *  - dayOfWeek: "MONDAY", "TUESDAY", ...
 *  - startTime: "HH:mm"
 *  - endTime: "HH:mm"
 *  - active: whether this slot is used.
 */
public class DoctorSchedule {

    private Long id;
    private String dayOfWeek;
    private String startTime;
    private String endTime;
    private Boolean active;

    public DoctorSchedule() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
