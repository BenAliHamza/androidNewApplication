package tn.esprit.presentation.history;

import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import tn.esprit.R;
import tn.esprit.domain.history.UserHistoryEntry;

/**
 * Simple ListAdapter for user history entries.
 */
public class UserHistoryAdapter
        extends ListAdapter<UserHistoryEntry, UserHistoryAdapter.HistoryViewHolder> {

    public interface OnHistoryClickListener {
        void onHistoryClicked(@NonNull UserHistoryEntry entry);
    }

    private final OnHistoryClickListener clickListener;

    private static final DiffUtil.ItemCallback<UserHistoryEntry> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<UserHistoryEntry>() {
                @Override
                public boolean areItemsTheSame(@NonNull UserHistoryEntry oldItem,
                                               @NonNull UserHistoryEntry newItem) {
                    Long oldId = oldItem.getId();
                    Long newId = newItem.getId();
                    if (oldId == null || newId == null) {
                        return oldItem == newItem;
                    }
                    return oldId.equals(newId);
                }

                @Override
                public boolean areContentsTheSame(@NonNull UserHistoryEntry oldItem,
                                                  @NonNull UserHistoryEntry newItem) {
                    if (!safeEquals(oldItem.getSafeMessage(), newItem.getSafeMessage())) return false;
                    if (!safeEquals(oldItem.getEventType(), newItem.getEventType())) return false;
                    if (!safeEquals(oldItem.getCreatedAt(), newItem.getCreatedAt())) return false;
                    if (!safeEquals(oldItem.getDetailsJson(), newItem.getDetailsJson())) return false;
                    return true;
                }

                private boolean safeEquals(Object a, Object b) {
                    if (a == b) return true;
                    if (a == null || b == null) return false;
                    return a.equals(b);
                }
            };

    public UserHistoryAdapter(@NonNull OnHistoryClickListener clickListener) {
        super(DIFF_CALLBACK);
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_history, parent, false);
        return new HistoryViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        UserHistoryEntry item = getItem(position);
        holder.bind(item);
    }

    class HistoryViewHolder extends RecyclerView.ViewHolder {

        private final TextView textMessage;
        private final TextView textTime;
        private final TextView textEventType;

        HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.text_history_message);
            textTime = itemView.findViewById(R.id.text_history_time);
            textEventType = itemView.findViewById(R.id.text_history_event_type);

            itemView.setOnClickListener(v -> {
                int pos = getBindingAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;
                UserHistoryEntry entry = getItem(pos);
                if (entry != null && clickListener != null) {
                    clickListener.onHistoryClicked(entry);
                }
            });
        }

        void bind(@NonNull UserHistoryEntry entry) {
            String message = entry.getSafeMessage();
            textMessage.setText(message);

            // Event type is optional and a bit technical; show as small label if present.
            String eventType = entry.getEventType();
            if (!TextUtils.isEmpty(eventType)) {
                textEventType.setVisibility(View.VISIBLE);
                textEventType.setText(eventType.replace('_', ' '));
            } else {
                textEventType.setVisibility(View.GONE);
            }

            // Human-ish time label from createdAt
            String createdAt = entry.getCreatedAt();
            String timeLabel = formatTime(itemView, createdAt);
            textTime.setText(timeLabel);

            // Accessibility: announce message + time
            StringBuilder desc = new StringBuilder();
            if (!TextUtils.isEmpty(message)) {
                desc.append(message);
            }
            if (!TextUtils.isEmpty(timeLabel)) {
                if (desc.length() > 0) desc.append(", ");
                desc.append(timeLabel);
            }
            itemView.setContentDescription(desc.toString());
        }

        private String formatTime(@NonNull View view, @Nullable String createdAt) {
            if (createdAt == null || createdAt.trim().isEmpty()) {
                return "";
            }

            // Backend uses LocalDateTime like 2025-01-28T09:15:22
            Date parsed = parseIsoDateTime(createdAt);
            if (parsed == null) {
                // Fallback: show raw date
                return createdAt;
            }

            long then = parsed.getTime();

            if (DateUtils.isToday(then)) {
                // Today • HH:mm
                java.text.DateFormat timeFormat =
                        android.text.format.DateFormat.getTimeFormat(view.getContext());
                String time = timeFormat.format(parsed);
                return view.getContext().getString(R.string.history_time_today, time);
            }

            // "Yesterday" check: DateUtils has no direct helper, so shift by +1 day
            long oneDayMillis = 24L * 60L * 60L * 1000L;
            if (DateUtils.isToday(then + oneDayMillis)) {
                java.text.DateFormat timeFormat =
                        android.text.format.DateFormat.getTimeFormat(view.getContext());
                String time = timeFormat.format(parsed);
                return view.getContext().getString(R.string.history_time_yesterday, time);
            }

            // Generic date • time
            java.text.DateFormat dateFormat =
                    android.text.format.DateFormat.getMediumDateFormat(view.getContext());
            java.text.DateFormat timeFormat =
                    android.text.format.DateFormat.getTimeFormat(view.getContext());

            String datePart = dateFormat.format(parsed);
            String timePart = timeFormat.format(parsed);
            return view.getContext().getString(R.string.history_time_date_time, datePart, timePart);
        }

        @Nullable
        private Date parseIsoDateTime(@NonNull String iso) {
            String trimmed = iso.trim();
            String[] patterns = new String[]{
                    "yyyy-MM-dd'T'HH:mm:ss",
                    "yyyy-MM-dd'T'HH:mm:ss.SSS",
                    "yyyy-MM-dd'T'HH:mm"
            };

            for (String pattern : patterns) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.US);
                    sdf.setLenient(true);
                    return sdf.parse(trimmed);
                } catch (ParseException ignored) {
                }
            }

            return null;
        }
    }
}
