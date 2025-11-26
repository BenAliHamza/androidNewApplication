package tn.esprit.presentation.medication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import tn.esprit.R;

/**
 * Receives alarm events and shows the actual notification.
 */
public class MedicationReminderReceiver extends BroadcastReceiver {

    private static final String TAG = "MedRemReceiver";

    public static final String EXTRA_LINE_ID = "extra_med_line_id";
    public static final String EXTRA_MEDICATION_NAME = "extra_med_name";

    // Action used by MedicationReminderScheduler (for safety checking)
    public static final String ACTION_SHOW_REMINDER =
            "tn.esprit.presentation.medication.ACTION_SHOW_REMINDER";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) return;

        android.util.Log.d(
                "MedRemReceiver",
                "onReceive: action=" + intent.getAction()
                        + ", lineId=" + intent.getLongExtra(EXTRA_LINE_ID, -1L)
                        + ", medName=" + intent.getStringExtra(EXTRA_MEDICATION_NAME)
        );

        // Defensive check: if action is set, make sure it's ours
        String action = intent.getAction();
        if (action != null && !ACTION_SHOW_REMINDER.equals(action)) {
            android.util.Log.d("MedRemReceiver", "Ignoring broadcast with unexpected action: " + action);
            return;
        }

        long lineId = intent.getLongExtra(EXTRA_LINE_ID, -1L);
        String medicationName = intent.getStringExtra(EXTRA_MEDICATION_NAME);
        if (medicationName == null || medicationName.trim().isEmpty()) {
            medicationName = context.getString(R.string.patient_medication_name_placeholder);
        }

        // Make sure we have channel + permission
        MedicationNotificationHelper.ensureNotificationChannel(context);
        if (!MedicationNotificationHelper.hasPostNotificationPermission(context)) {
            android.util.Log.w("MedRemReceiver", "Notification permission missing, not showing reminder");
            return;
        }

        android.util.Log.d("MedRemReceiver", "Showing notification for lineId=" + lineId);

        // Build notification
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, MedicationNotificationHelper.CHANNEL_ID_MEDICATION)
                        .setSmallIcon(R.drawable.logo)
                        .setContentTitle(
                                context.getString(
                                        R.string.medication_reminder_title,
                                        medicationName
                                )
                        )
                        .setContentText(
                                context.getString(R.string.medication_reminder_body)
                        )
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setAutoCancel(true);

        NotificationManagerCompat manager = NotificationManagerCompat.from(context);

        try {
            int notificationId = (int) (lineId > 0 ? lineId : System.currentTimeMillis());
            manager.notify(notificationId, builder.build());
        } catch (SecurityException ignored) {
            android.util.Log.e("MedRemReceiver", "SecurityException while showing notification", ignored);
        }
    }

}
