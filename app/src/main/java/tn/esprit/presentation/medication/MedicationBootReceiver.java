package tn.esprit.presentation.medication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

/**
 * Receives BOOT_COMPLETED and re-schedules all medication reminders
 * from SharedPreferences.
 */
public class MedicationBootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(@NonNull Context context, Intent intent) {
        if (intent == null) return;

        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            // Re-schedule all reminders we know about
            MedicationReminderScheduler.rescheduleAllReminders(context);
        }
    }
}
