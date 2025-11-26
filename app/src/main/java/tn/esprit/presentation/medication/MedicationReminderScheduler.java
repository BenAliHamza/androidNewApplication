package tn.esprit.presentation.medication;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import tn.esprit.domain.medication.PrescriptionLine;

/**
 * Helper to schedule and cancel local alarms for medication reminders.
 *
 * Current simple behavior:
 *  - For a line with reminderEnabled = true, we schedule a repeating alarm.
 *  - Repeat interval is 24h / timesPerDay (or 1x/day if missing).
 *  - First trigger is "now + interval".
 *
 * This is a starting point and can be refined later when we have exact times.
 */
public final class MedicationReminderScheduler {

    private MedicationReminderScheduler() {
        // no instances
    }

    public static void scheduleReminder(@NonNull Context context,
                                        @Nullable PrescriptionLine line) {
        if (line == null) return;
        Long id = line.getId();
        if (id == null || id <= 0L) return;

        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        PendingIntent pendingIntent = buildPendingIntent(context, line);
        if (pendingIntent == null) return;

        int timesPerDay = 1;
        if (line.getTimesPerDay() != null && line.getTimesPerDay() > 0) {
            timesPerDay = line.getTimesPerDay();
        }

        long intervalMillis = AlarmManager.INTERVAL_DAY / timesPerDay;
        long triggerAtMillis = System.currentTimeMillis() + intervalMillis;

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

    public static void cancelReminder(@NonNull Context context,
                                      @Nullable PrescriptionLine line) {
        if (line == null) return;
        Long id = line.getId();
        if (id == null || id <= 0L) return;

        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        PendingIntent pendingIntent = buildPendingIntent(context, line);
        if (pendingIntent == null) return;

        alarmManager.cancel(pendingIntent);
    }

    @Nullable
    private static PendingIntent buildPendingIntent(@NonNull Context context,
                                                    @NonNull PrescriptionLine line) {
        Long id = line.getId();
        if (id == null) return null;

        Intent intent = new Intent(context, MedicationReminderReceiver.class);
        intent.putExtra("lineId", id);
        intent.putExtra("medicationName", line.getMedicationName());
        intent.putExtra("dosage", line.getDosage());

        int requestCode;
        if (id > 0 && id <= Integer.MAX_VALUE) {
            requestCode = id.intValue();
        } else {
            // Fallback, still deterministic
            requestCode = (int) (id & 0x7FFFFFFF);
        }

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        return PendingIntent.getBroadcast(
                context.getApplicationContext(),
                requestCode,
                intent,
                flags
        );
    }
}
