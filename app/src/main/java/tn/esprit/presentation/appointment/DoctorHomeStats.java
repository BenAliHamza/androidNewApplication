package tn.esprit.presentation.appointment;

import androidx.annotation.NonNull;

/**
 * Small value object for the doctor home stats card.
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

    public int getTodayAppointments() {
        return todayAppointments;
    }

    public int getWeekAppointments() {
        return weekAppointments;
    }

    public int getPatientsWithAppointments() {
        return patientsWithAppointments;
    }

    @NonNull
    public static DoctorHomeStats empty() {
        return new DoctorHomeStats(0, 0, 0);
    }
}
