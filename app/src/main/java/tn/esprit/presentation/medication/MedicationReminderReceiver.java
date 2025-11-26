package tn.esprit.presentation.medication;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import tn.esprit.R;

/**
 * Receives alarm broadcasts for medication reminders and shows a notification.
 */
public class MedicationReminderReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "medication_reminders_channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) return;

        long lineId = intent.getLongExtra("lineId", -1L);
        String medicationName = intent.getStringExtra("medicationName");
        String dosage = intent.getStringExtra("dosage");

        if (medicationName == null || medicationName.trim().isEmpty()) {
            medicationName = "Medication reminder";
        }

        String contentText;
        if (dosage != null && !dosage.trim().isEmpty()) {
            contentText = "Time to take: " + dosage;
        } else {
            contentText = "Don't forget to take your medication.";
        }

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        // Create channel on Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Medication reminders";
            String description = "Reminders to take your medications";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel =
                    new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.drawable.logo)
                        .setContentTitle(medicationName)
                        .setContentText(contentText)
                        .setAutoCancel(true)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        // Use lineId as notification ID if possible, otherwise fallback to 0
        int notificationId = (lineId > 0 && lineId <= Integer.MAX_VALUE)
                ? (int) lineId
                : 0;

        manager.notify(notificationId, builder.build());
    }
}
