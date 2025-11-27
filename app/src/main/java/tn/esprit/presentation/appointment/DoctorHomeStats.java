package tn.esprit.domain.appointment;

/**
 * Simple stats model for the doctor home highlight card.
 *
 * - todayAppointments: number of appointments scheduled for today.
 * - weekAppointments: number of appointments in the current week.
 * - patientsWithAppointments: distinct patients with at least one appointment this week.
 */
public class DoctorHomeStats {

    private final int todayAppointments;
    private final int weekAppointments;
    private final int patientsWithAppointments;

    public DoctorHomeStats(int todayAppointments,
                           int weekAppointments,
                           int patientsWithAppointments) {
        this.todayAppointments = todayAppointments;
        this.weekAppointments = weekAppointments;
        this.patientsWithAppointments = patientsWithAppointments;
    }

    public static DoctorHomeStats empty() {
        return new DoctorHomeStats(0, 0, 0);
    }

    public int getTodayAppointments() {
        return todayAppointments;
    }

    public int getWeekAppointments() {
        return weekAppointments;
    }

    public int getPatientsWithAppointments() {
        return patientsWithAppointments;
    }
}
