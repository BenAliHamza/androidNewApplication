package tn.esprit.presentation.notification;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import tn.esprit.R;
import tn.esprit.domain.notification.NotificationItem;
import tn.esprit.presentation.appointment.AppointmentUiHelper;

/**
 * Adapter for displaying notifications in a RecyclerView.
 *
 * Uses NotificationItem as provided by backend:
 *  - id
 *  - type
 *  - title
 *  - message
 *  - appointmentId
 *  - createdAt (ISO-8601 string)
 *  - read
 */
public class NotificationAdapter extends ListAdapter<NotificationItem, NotificationAdapter.NotificationViewHolder> {

    public interface OnNotificationClickListener {
        void onNotificationClicked(@NonNull NotificationItem notification);
    }

    private final OnNotificationClickListener listener;

    private static final DiffUtil.ItemCallback<NotificationItem> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<NotificationItem>() {
                @Override
                public boolean areItemsTheSame(@NonNull NotificationItem oldItem,
                                               @NonNull NotificationItem newItem) {
                    if (oldItem.getId() == null || newItem.getId() == null) {
                        // Fall back to reference equality if id is missing
                        return oldItem == newItem;
                    }
                    return oldItem.getId().equals(newItem.getId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull NotificationItem oldItem,
                                                  @NonNull NotificationItem newItem) {
                    return safeEquals(oldItem.getTitle(), newItem.getTitle())
                            && safeEquals(oldItem.getMessage(), newItem.getMessage())
                            && safeEquals(oldItem.getCreatedAt(), newItem.getCreatedAt())
                            && oldItem.isRead() == newItem.isRead();
                }

                private boolean safeEquals(@Nullable Object a, @Nullable Object b) {
                    if (a == b) return true;
                    if (a == null || b == null) return false;
                    return a.equals(b);
                }
            };

    public NotificationAdapter(@NonNull OnNotificationClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        NotificationItem notification = getItem(position);
        holder.bind(notification, listener);
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {

        private final MaterialCardView cardRoot;
        private final View unreadDot;
        private final TextView textTitle;
        private final TextView textMessage;
        private final TextView textCreatedAt;

        NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.card_notification_root);
            unreadDot = itemView.findViewById(R.id.view_unread_dot);
            textTitle = itemView.findViewById(R.id.text_notification_title);
            textMessage = itemView.findViewById(R.id.text_notification_message);
            textCreatedAt = itemView.findViewById(R.id.text_notification_created_at);
        }

        void bind(@NonNull NotificationItem notification,
                  @NonNull OnNotificationClickListener listener) {

            // Title
            String title = notification.getTitle();
            if (title == null || title.trim().isEmpty()) {
                title = itemView.getContext().getString(R.string.notifications_default_title);
            }
            textTitle.setText(title);

            // Message
            String message = notification.getMessage();
            if (message == null || message.trim().isEmpty()) {
                textMessage.setVisibility(View.GONE);
            } else {
                textMessage.setVisibility(View.VISIBLE);
                textMessage.setText(message.trim());
            }

            // Created at – pretty format if possible, otherwise raw
            String createdAt = notification.getCreatedAt();
            if (createdAt == null || createdAt.trim().isEmpty()) {
                textCreatedAt.setVisibility(View.GONE);
            } else {
                textCreatedAt.setVisibility(View.VISIBLE);
                textCreatedAt.setText(formatCreatedAt(createdAt.trim()));
            }

            // Unread dot + alpha
            if (notification.isRead()) {
                unreadDot.setVisibility(View.INVISIBLE);
                cardRoot.setAlpha(0.8f);
            } else {
                unreadDot.setVisibility(View.VISIBLE);
                cardRoot.setAlpha(1.0f);
            }

            cardRoot.setOnClickListener(v -> listener.onNotificationClicked(notification));
        }

        /**
         * Formats ISO datetime into something like:
         *  - "Today • 09:30"  (if same date as today)
         *  - "Thu, 27 Nov • 09:30" (otherwise)
         *
         * Falls back to the original string if parsing fails.
         */
        @NonNull
        private String formatCreatedAt(@NonNull String createdAtIso) {
            Date date = AppointmentUiHelper.parseDate(createdAtIso);
            if (date == null) {
                // If parsing fails, just show the raw text
                return createdAtIso;
            }

            String todayPrefix = AppointmentUiHelper.getTodayDatePrefix();
            String datePrefix = AppointmentUiHelper.safeDatePrefix(createdAtIso);

            SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());
            SimpleDateFormat dateFmt = new SimpleDateFormat("EEE, d MMM", Locale.getDefault());

            String time = timeFmt.format(date);

            if (todayPrefix != null && todayPrefix.equals(datePrefix)) {
                // Reuse same pattern as home: "Today • 09:30"
                return itemView.getContext()
                        .getString(R.string.home_next_appointment_today, time);
            } else {
                // Example: "Thu, 27 Nov • 09:30"
                String dateLabel = dateFmt.format(date);
                return itemView.getContext()
                        .getString(R.string.home_next_appointment_date_time, dateLabel, time);
            }
        }
    }
}
