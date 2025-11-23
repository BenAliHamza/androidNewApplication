package tn.esprit.presentation.home;

import java.util.Calendar;

import tn.esprit.R;

/**
 * Small UI helper for home header: greeting and role labels.
 */
public final class HomeUiHelper {

    private HomeUiHelper() {
        // no instances
    }

    /**
     * Returns the greeting string resource id based on current hour.
     */
    public static int resolveGreetingResId() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);

        if (hour < 12) {
            return R.string.home_greeting_morning;
        } else if (hour < 18) {
            return R.string.home_greeting_afternoon;
        } else {
            return R.string.home_greeting_evening;
        }
    }

    /**
     * Returns the correct role label resource id for a given backend role string.
     */
    public static int resolveRoleLabelResId(String role) {
        if (HomeRole.isDoctor(role)) {
            return R.string.profile_role_doctor;
        } else if (HomeRole.isPatient(role)) {
            return R.string.profile_role_patient;
        } else {
            return R.string.profile_role_unknown;
        }
    }
}
