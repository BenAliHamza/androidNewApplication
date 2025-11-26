package tn.esprit.presentation.medication;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Calendar;
import java.util.Locale;
import java.util.Map;

/**
 * Helper class to schedule/cancel daily medication reminders for a given prescription line.
 *
 * All configuration is stored in SharedPreferences so that we can:
 *  - show the current time in the UI (adapter)
 *  - reschedule all alarms after reboot.
 */
public final class MedicationReminderScheduler {

    private static final String PREFS_NAME = "medication_reminders";

    private static final String KEY_PREFIX_LINE = "line_";
    private static final String KEY_SUFFIX_HOUR = "_hour";
    private static final String KEY_SUFFIX_MINUTE = "_minute";
    private static final String KEY_SUFFIX_NAME = "_name";

    private MedicationReminderScheduler() {
        // no instances
    }

    // ---------------------------------------------------------------------
    // Public API used from Fragment / Boot receiver
    // ---------------------------------------------------------------------

    /**
     * Schedule a daily alarm for a prescription line at the given time.
     * This also persists the config (time + medication name) in SharedPreferences.
     *
     * Uses an inexact repeating alarm so it works without SCHEDULE_EXACT_ALARM permission.
     */
    public static void scheduleDailyReminder(@NonNull Context context,
                                             @NonNull Long lineId,
                                             @NonNull String medicationName,
                                             int hourOfDay,
                                             int minute) {
        if (lineId == null || lineId <= 0L) return;
        if (hourOfDay < 0 || hourOfDay > 23 || minute < 0 || minute > 59) return;
        if (TextUtils.isEmpty(medicationName)) return;

        Context appCtx = context.getApplicationContext();

        // Persist config
        saveReminderConfig(appCtx, lineId, medicationName, hourOfDay, minute);

        AlarmManager alarmManager =
                (AlarmManager) appCtx.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            android.util.Log.w("MedScheduler", "AlarmManager is null, cannot schedule reminder");
            return;
        }

        PendingIntent pendingIntent =
                buildPendingIntent(appCtx, lineId, medicationName);

        // Compute first trigger time (today at hour:minute, or tomorrow if already passed)
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        long triggerAtMillis = calendar.getTimeInMillis();
        long now = System.currentTimeMillis();
        if (triggerAtMillis <= now) {
            // First trigger should be in the future -> add 1 day
            calendar.add(Calendar.DAY_OF_YEAR, 1);
            triggerAtMillis = calendar.getTimeInMillis();
        }

        long intervalMillis = AlarmManager.INTERVAL_DAY;

        android.util.Log.d(
                "MedScheduler",
                "scheduleDailyReminder: lineId=" + lineId +
                        " name=" + medicationName +
                        " time=" + String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute) +
                        " firstAt=" + new java.util.Date(triggerAtMillis) +
                        " (" + triggerAtMillis + ")"
        );

        // Inexact repeating alarm – no exact-alarm permission needed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmManager.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    intervalMillis,
                    pendingIntent
            );
        } else {
            alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    intervalMillis,
                    pendingIntent
            );
        }
    }

    /**
     * Cancel an existing reminder (if any) and clear persisted config.
     */
    public static void cancelReminder(@NonNull Context context,
                                      @NonNull Long lineId,
                                      @NonNull String medicationName) {
        if (lineId == null || lineId <= 0L) return;

        Context appCtx = context.getApplicationContext();

        AlarmManager alarmManager =
                (AlarmManager) appCtx.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            PendingIntent pendingIntent =
                    buildPendingIntent(appCtx, lineId, medicationName);
            try {
                alarmManager.cancel(pendingIntent);
                android.util.Log.d("MedScheduler", "cancelReminder: lineId=" + lineId);
            } catch (Exception e) {
                android.util.Log.w("MedScheduler", "cancelReminder: failed", e);
            }
        }

        clearReminderConfig(appCtx, lineId);
    }

    /**
     * Returns [hour, minute] for this line's reminder, or null if none stored.
     */
    @Nullable
    public static int[] getReminderTime(@NonNull Context context,
                                        long lineId) {
        Context appCtx = context.getApplicationContext();
        SharedPreferences prefs =
                appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        String baseKey = KEY_PREFIX_LINE + lineId;
        int hour = prefs.getInt(baseKey + KEY_SUFFIX_HOUR, -1);
        int minute = prefs.getInt(baseKey + KEY_SUFFIX_MINUTE, -1);

        if (hour < 0 || minute < 0) {
            return null;
        }
        return new int[]{hour, minute};
    }

    /**
     * Called from BOOT_COMPLETED receiver to reschedule all known reminders
     * after device reboot.
     */
    public static void rescheduleAllReminders(@NonNull Context context) {
        Context appCtx = context.getApplicationContext();
        SharedPreferences prefs =
                appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        Map<String, ?> all = prefs.getAll();
        if (all == null || all.isEmpty()) {
            android.util.Log.d("MedScheduler", "rescheduleAllReminders: no stored reminders");
            return;
        }

        android.util.Log.d("MedScheduler", "rescheduleAllReminders: entries=" + all.size());

        for (Map.Entry<String, ?> entry : all.entrySet()) {
            String key = entry.getKey();
            if (!key.startsWith(KEY_PREFIX_LINE) || !key.endsWith(KEY_SUFFIX_HOUR)) {
                continue;
            }

            // Extract lineId from "line_<id>_hour"
            String idPart = key.substring(
                    KEY_PREFIX_LINE.length(),
                    key.length() - KEY_SUFFIX_HOUR.length()
            );
            long lineId;
            try {
                lineId = Long.parseLong(idPart);
            } catch (NumberFormatException e) {
                continue;
            }

            int hour = prefs.getInt(key, -1);
            int minute = prefs.getInt(
                    KEY_PREFIX_LINE + idPart + KEY_SUFFIX_MINUTE,
                    -1
            );
            String medName = prefs.getString(
                    KEY_PREFIX_LINE + idPart + KEY_SUFFIX_NAME,
                    null
            );

            if (hour < 0 || minute < 0 || TextUtils.isEmpty(medName)) {
                continue;
            }

            android.util.Log.d(
                    "MedScheduler",
                    "rescheduleAllReminders: lineId=" + lineId +
                            " name=" + medName +
                            " time=" + String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
            );

            // Re-schedule alarm (repeating)
            scheduleDailyReminder(appCtx, lineId, medName, hour, minute);
        }
    }

    // ---------------------------------------------------------------------
    // Internal helpers for prefs + PendingIntent
    // ---------------------------------------------------------------------

    private static void saveReminderConfig(@NonNull Context context,
                                           long lineId,
                                           @NonNull String medicationName,
                                           int hour,
                                           int minute) {
        SharedPreferences prefs =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String baseKey = KEY_PREFIX_LINE + lineId;

        prefs.edit()
                .putInt(baseKey + KEY_SUFFIX_HOUR, hour)
                .putInt(baseKey + KEY_SUFFIX_MINUTE, minute)
                .putString(baseKey + KEY_SUFFIX_NAME, medicationName)
                .apply();
    }

    private static void clearReminderConfig(@NonNull Context context,
                                            long lineId) {
        SharedPreferences prefs =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String baseKey = KEY_PREFIX_LINE + lineId;

        prefs.edit()
                .remove(baseKey + KEY_SUFFIX_HOUR)
                .remove(baseKey + KEY_SUFFIX_MINUTE)
                .remove(baseKey + KEY_SUFFIX_NAME)
                .apply();
    }

    private static PendingIntent buildPendingIntent(@NonNull Context context,
                                                    long lineId,
                                                    @NonNull String medicationName) {
        Intent intent = new Intent(context, MedicationReminderReceiver.class);
        intent.setAction(MedicationReminderReceiver.ACTION_SHOW_REMINDER);
        intent.putExtra(MedicationReminderReceiver.EXTRA_LINE_ID, lineId);
        intent.putExtra(MedicationReminderReceiver.EXTRA_MEDICATION_NAME, medicationName);

        int requestCode = (int) (lineId & 0x7FFFFFFF); // keep it in int range

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        android.util.Log.d(
                "MedScheduler",
                "buildPendingIntent: lineId=" + lineId + " requestCode=" + requestCode
        );

        return PendingIntent.getBroadcast(
                context.getApplicationContext(),
                requestCode,
                intent,
                flags
        );
    }

    // Returns a formatted "HH:mm" string for this line's reminder time, or null if none.
    @Nullable
    public static String getFormattedReminderTimeForLine(@NonNull Context context,
                                                         long lineId) {
        int[] time = getReminderTime(context, lineId);
        if (time == null || time.length != 2) {
            return null;
        }
        return String.format(Locale.getDefault(), "%02d:%02d", time[0], time[1]);
    }
}
