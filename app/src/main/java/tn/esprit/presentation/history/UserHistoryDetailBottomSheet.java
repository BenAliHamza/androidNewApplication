package tn.esprit.presentation.history;

import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import tn.esprit.R;
import tn.esprit.domain.history.UserHistoryEntry;

/**
 * Bottom sheet to show more details about a history entry
 * (message, time, and pretty-printed detailsJson if available).
 */
public class UserHistoryDetailBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_MESSAGE = "arg_message";
    private static final String ARG_EVENT_TYPE = "arg_event_type";
    private static final String ARG_CREATED_AT = "arg_created_at";
    private static final String ARG_DETAILS_JSON = "arg_details_json";

    public static UserHistoryDetailBottomSheet newInstance(@NonNull UserHistoryEntry entry) {
        UserHistoryDetailBottomSheet sheet = new UserHistoryDetailBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_MESSAGE, entry.getSafeMessage());
        args.putString(ARG_EVENT_TYPE, entry.getEventType());
        args.putString(ARG_CREATED_AT, entry.getCreatedAt());
        args.putString(ARG_DETAILS_JSON, entry.getDetailsJson());
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.bottom_sheet_user_history_details, container, false);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        TextView textTitle = view.findViewById(R.id.text_history_detail_title);
        TextView textEventType = view.findViewById(R.id.text_history_detail_event_type);
        TextView textTime = view.findViewById(R.id.text_history_detail_time);
        TextView textDetails = view.findViewById(R.id.text_history_detail_details);
        MaterialButton buttonClose = view.findViewById(R.id.button_history_detail_close);

        Bundle args = getArguments();
        String message = args != null ? args.getString(ARG_MESSAGE) : null;
        String eventType = args != null ? args.getString(ARG_EVENT_TYPE) : null;
        String createdAt = args != null ? args.getString(ARG_CREATED_AT) : null;
        String detailsJson = args != null ? args.getString(ARG_DETAILS_JSON) : null;

        // Title = human message
        if (!TextUtils.isEmpty(message)) {
            textTitle.setText(message);
        } else {
            textTitle.setText(getString(R.string.history_detail_title_fallback));
        }

        // Event type label
        if (!TextUtils.isEmpty(eventType)) {
            textEventType.setVisibility(View.VISIBLE);
            textEventType.setText(eventType.replace('_', ' '));
        } else {
            textEventType.setVisibility(View.GONE);
        }

        // Time label similar to list cells
        String timeLabel = formatTime(view, createdAt);
        if (!TextUtils.isEmpty(timeLabel)) {
            textTime.setText(timeLabel);
        } else {
            textTime.setText("");
        }

        // Details JSON pretty-print
        String prettyDetails = buildPrettyDetails(detailsJson);
        if (TextUtils.isEmpty(prettyDetails)) {
            textDetails.setText(getString(R.string.history_details_none));
        } else {
            textDetails.setText(prettyDetails);
        }

        if (buttonClose != null) {
            buttonClose.setOnClickListener(v -> dismiss());
        }
    }

    private String formatTime(@NonNull View view, @Nullable String createdAt) {
        if (createdAt == null || createdAt.trim().isEmpty()) {
            return "";
        }

        Date parsed = parseIsoDateTime(createdAt);
        if (parsed == null) {
            return createdAt;
        }

        long then = parsed.getTime();

        if (DateUtils.isToday(then)) {
            java.text.DateFormat timeFormat =
                    android.text.format.DateFormat.getTimeFormat(view.getContext());
            String time = timeFormat.format(parsed);
            return view.getContext().getString(R.string.history_time_today, time);
        }

        long oneDayMillis = 24L * 60L * 60L * 1000L;
        if (DateUtils.isToday(then + oneDayMillis)) {
            java.text.DateFormat timeFormat =
                    android.text.format.DateFormat.getTimeFormat(view.getContext());
            String time = timeFormat.format(parsed);
            return view.getContext().getString(R.string.history_time_yesterday, time);
        }

        java.text.DateFormat dateFormat =
                android.text.format.DateFormat.getMediumDateFormat(view.getContext());
        java.text.DateFormat timeFormat =
                android.text.format.DateFormat.getTimeFormat(view.getContext());

        String datePart = dateFormat.format(parsed);
        String timePart = timeFormat.format(parsed);
        return view.getContext()
                .getString(R.string.history_time_date_time, datePart, timePart);
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

    @Nullable
    private String buildPrettyDetails(@Nullable String detailsJson) {
        if (detailsJson == null) return null;
        String trimmed = detailsJson.trim();
        if (trimmed.isEmpty()) return null;

        try {
            JsonElement element = JsonParser.parseString(trimmed);
            Gson gson = new GsonBuilder()
                    .setPrettyPrinting()
                    .create();
            return gson.toJson(element);
        } catch (Exception e) {
            // Not valid JSON → just show raw
            return trimmed;
        }
    }
}
