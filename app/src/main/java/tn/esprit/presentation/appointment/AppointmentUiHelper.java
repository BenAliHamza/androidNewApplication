package tn.esprit.presentation.appointment;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import tn.esprit.R;
import tn.esprit.domain.appointment.Appointment;

/**
 * Small UI helper for appointments (status labels, date helpers, etc.).
 */
public final class AppointmentUiHelper {

    private AppointmentUiHelper() {
        // no instances
    }

    // ------------------------------------------------------------------------
    // Status helpers
    // ------------------------------------------------------------------------

    /**
     * Maps backend status string (PENDING / ACCEPTED / REJECTED / COMPLETED)
     * to a human-readable localized label.
     */
    @NonNull
    public static String getStatusLabel(@NonNull Context context,
                                        @Nullable String statusRaw) {
        if (statusRaw == null) {
            return context.getString(R.string.appointment_status_unknown);
        }

        switch (statusRaw) {
            case "PENDING":
                return context.getString(R.string.appointment_status_pending);
            case "ACCEPTED":
                return context.getString(R.string.appointment_status_accepted);
            case "REJECTED":
                return context.getString(R.string.appointment_status_rejected);
            case "COMPLETED":
                return context.getString(R.string.appointment_status_completed);
            default:
                return context.getString(R.string.appointment_status_unknown);
        }
    }

    /**
     * Returns "Status: X" using appointment_status_prefix.
     */
    @NonNull
    public static String getStatusWithPrefix(@NonNull Context context,
                                             @Nullable String statusRaw) {
        String label = getStatusLabel(context, statusRaw);
        return context.getString(R.string.appointment_status_prefix, label);
    }

    // ------------------------------------------------------------------------
    // Date helpers
    // ------------------------------------------------------------------------

    /**
     * Returns today's date prefix in ISO-8601 date format (yyyy-MM-dd).
     *
     * Used by DoctorAppointmentsViewModel:
     *   startAt != null && startAt.startsWith(getTodayDatePrefix())
     */
    @NonNull
    public static String getTodayDatePrefix() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }

    /**
     * Safely extracts the "yyyy-MM-dd" prefix from an ISO-8601 date-time string.
     * Returns null if the input is null/too short.
     *
     * Used by DoctorAppointmentsViewModel to split appointments into
     * today / upcoming / past using lexicographic comparison.
     */
    @Nullable
    public static String safeDatePrefix(@Nullable String isoDateTime) {
        if (isoDateTime == null) return null;
        String trimmed = isoDateTime.trim();
        if (trimmed.length() < 10) return null;
        // Expect "yyyy-MM-dd..." -> first 10 chars
        return trimmed.substring(0, 10);
    }

    /**
     * Parse ISO datetime string into Date using common backend patterns.
     *
     * Patterns tried:
     *  - yyyy-MM-dd'T'HH:mm:ss
     *  - yyyy-MM-dd'T'HH:mm:ss.SSS
     */
    @Nullable
    public static Date parseDate(@Nullable String iso) {
        if (iso == null || iso.trim().isEmpty()) return null;

        String[] patterns = new String[]{
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss.SSS"
        };

        for (String pattern : patterns) {
            try {
                SimpleDateFormat parser = new SimpleDateFormat(pattern, Locale.getDefault());
                return parser.parse(iso);
            } catch (ParseException ignore) {
            }
        }
        return null;
    }

    /**
     * Sort a list of appointments by startAt (ISO string, lexicographically).
     *
     * Returns a new list (does not mutate the input). Null -> empty list.
     */
    @NonNull
    public static List<Appointment> sortByStart(@Nullable List<Appointment> list) {
        if (list == null) return Collections.emptyList();
        List<Appointment> copy = new ArrayList<>(list);

        // ISO-8601 date strings are lexicographically sortable.
        Collections.sort(copy, new Comparator<Appointment>() {
            @Override
            public int compare(Appointment o1, Appointment o2) {
                String d1 = (o1 != null && o1.getStartAt() != null) ? o1.getStartAt() : "";
                String d2 = (o2 != null && o2.getStartAt() != null) ? o2.getStartAt() : "";
                return d1.compareTo(d2);
            }
        });

        return copy;
    }

    /**
     * Builds a human-readable date/time range from start/end ISO-8601 strings.
     *
     * Format example:
     *   "Mon, 4 Mar  •  09:00 – 09:30"
     * or (if end is missing)
     *   "Mon, 4 Mar  •  09:00"
     *
     * Used by DoctorAppointmentAdapter.
     */
    @NonNull
    public static String buildDateTimeDisplay(@Nullable String startIso,
                                              @Nullable String endIso,
                                              @NonNull Context context) {
        if (startIso == null || startIso.trim().isEmpty()) {
            return "";
        }

        SimpleDateFormat parser =
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
        SimpleDateFormat formatterDate =
                new SimpleDateFormat("EEE, d MMM", Locale.getDefault());
        SimpleDateFormat formatterTime =
                new SimpleDateFormat("HH:mm", Locale.getDefault());

        try {
            Date start = parser.parse(startIso);
            if (start == null) {
                return startIso;
            }
            String datePart = formatterDate.format(start);
            String startTime = formatterTime.format(start);

            if (endIso != null && !endIso.trim().isEmpty()) {
                Date end = parser.parse(endIso);
                if (end != null) {
                    String endTime = formatterTime.format(end);
                    return datePart + "  •  " + startTime + " – " + endTime;
                }
            }

            return datePart + "  •  " + startTime;
        } catch (ParseException e) {
            // Fallback: just show the raw ISO string if parsing fails
            return startIso;
        }
    }
}
