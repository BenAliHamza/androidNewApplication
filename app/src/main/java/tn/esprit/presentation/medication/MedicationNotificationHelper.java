package tn.esprit.presentation.medication;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * Helper for:
 *  - Creating the notification channel (Android 8+)
 *  - Requesting POST_NOTIFICATIONS permission (Android 13+)
 *  - Checking if notifications are allowed
 *
 * All alarm scheduling is handled by MedicationReminderScheduler.
 */
public final class MedicationNotificationHelper {

    // Channel id used by receiver + notifications
    public static final String CHANNEL_ID_MEDICATION = "medication_reminders_channel";

    private MedicationNotificationHelper() {
        // no instances
    }

    // ---------------------------------------------------------------------
    // Channel
    // ---------------------------------------------------------------------

    public static void ensureNotificationChannel(@NonNull Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        NotificationChannel existing = manager.getNotificationChannel(CHANNEL_ID_MEDICATION);
        if (existing != null) {
            return; // already created
        }

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID_MEDICATION,
                "Medication reminders",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        channel.setDescription("Reminders to take your medications");
        manager.createNotificationChannel(channel);
    }

    // ---------------------------------------------------------------------
    // Permission (Android 13+)
    // ---------------------------------------------------------------------

    /**
     * Android 13+ requires POST_NOTIFICATIONS runtime permission.
     * Call this from an Activity (e.g. MainActivity.onCreate()).
     */
    public static void requestNotificationPermissionIfNeeded(@NonNull Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return; // not needed before Android 13
        }

        if (ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED) {
            return; // already granted
        }

        ActivityCompat.requestPermissions(
                activity,
                new String[]{Manifest.permission.POST_NOTIFICATIONS},
                1001
        );
    }

    /**
     * Helper used inside the receiver to check if notifications are allowed.
     */
    public static boolean hasPostNotificationPermission(@NonNull Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true;
        }
        return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED;
    }
}
