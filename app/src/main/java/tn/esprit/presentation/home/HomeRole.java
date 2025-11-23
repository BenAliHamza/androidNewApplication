package tn.esprit.presentation.home;

/**
 * Tiny helper to centralize role checks.
 */
public final class HomeRole {

    private HomeRole() {
        // no instances
    }

    public static boolean isDoctor(String role) {
        return role != null && "DOCTOR".equalsIgnoreCase(role);
    }

    public static boolean isPatient(String role) {
        return role != null && "PATIENT".equalsIgnoreCase(role);
    }
}
