package tn.esprit.presentation.medication;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import tn.esprit.R;
import tn.esprit.domain.medication.PrescriptionLine;

/**
 * Adapter to render a list of PrescriptionLine items in the
 * patient's "My Medications" screen, with:
 *  - Section headers: "Active medications" / "Past medications"
 *  - Reminder toggle per line.
 *
 * The fragment still calls adapter.submitList(List<PrescriptionLine>) –
 * internally we group items into sections.
 */
public class PatientMedicationAdapter
        extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface OnMedicationInteractionListener {
        void onReminderToggled(@NonNull PrescriptionLine line, boolean enabled);
    }

    private static final int VIEW_TYPE_HEADER_ACTIVE = 0;
    private static final int VIEW_TYPE_HEADER_PAST = 1;
    private static final int VIEW_TYPE_LINE = 2;

    @Nullable
    private final OnMedicationInteractionListener interactionListener;

    // Raw lines coming from the backend / fragment
    @NonNull
    private final List<PrescriptionLine> sourceLines = new ArrayList<>();

    // Flattened list used for the RecyclerView (headers + lines)
    @NonNull
    private final List<Row> displayRows = new ArrayList<>();

    public PatientMedicationAdapter(@Nullable OnMedicationInteractionListener interactionListener) {
        this.interactionListener = interactionListener;
    }

    // ---------------------------------------------------------------------
    // Public API (called from Fragment)
    // ---------------------------------------------------------------------

    public void submitList(@Nullable List<PrescriptionLine> lines) {
        sourceLines.clear();
        if (lines != null) {
            sourceLines.addAll(lines);
        }
        rebuildDisplayRows();
        notifyDataSetChanged();
    }

    // ---------------------------------------------------------------------
    // Internal model for rows
    // ---------------------------------------------------------------------

    private static class Row {
        final int viewType;
        @Nullable
        final PrescriptionLine line; // only for VIEW_TYPE_LINE

        Row(int viewType, @Nullable PrescriptionLine line) {
            this.viewType = viewType;
            this.line = line;
        }
    }

    private void rebuildDisplayRows() {
        displayRows.clear();

        List<PrescriptionLine> active = new ArrayList<>();
        List<PrescriptionLine> past = new ArrayList<>();

        for (PrescriptionLine line : sourceLines) {
            if (line == null) continue;
            if (isLineActive(line)) {
                active.add(line);
            } else {
                past.add(line);
            }
        }

        // Active section
        if (!active.isEmpty()) {
            displayRows.add(new Row(VIEW_TYPE_HEADER_ACTIVE, null));
            for (PrescriptionLine line : active) {
                displayRows.add(new Row(VIEW_TYPE_LINE, line));
            }
        }

        // Past section
        if (!past.isEmpty()) {
            displayRows.add(new Row(VIEW_TYPE_HEADER_PAST, null));
            for (PrescriptionLine line : past) {
                displayRows.add(new Row(VIEW_TYPE_LINE, line));
            }
        }
    }

    /**
     * Very small heuristic to decide whether a line is "active" or "past":
     *  - If end date is empty -> active
     *  - Else: parse yyyy-MM-dd, active if endDate >= today.
     */
    private boolean isLineActive(@NonNull PrescriptionLine line) {
        String end = safeTrim(line.getPrescriptionEndDate());
        if (TextUtils.isEmpty(end)) {
            return true; // open-ended prescription
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date endDate = sdf.parse(end);
            if (endDate == null) return true;

            Calendar todayCal = Calendar.getInstance();
            todayCal.set(Calendar.HOUR_OF_DAY, 0);
            todayCal.set(Calendar.MINUTE, 0);
            todayCal.set(Calendar.SECOND, 0);
            todayCal.set(Calendar.MILLISECOND, 0);
            Date today = todayCal.getTime();

            // Active if endDate is today or in the future
            return !endDate.before(today);
        } catch (Exception e) {
            // If parsing fails, treat as active to avoid hiding things.
            return true;
        }
    }

    // ---------------------------------------------------------------------
    // RecyclerView.Adapter overrides
    // ---------------------------------------------------------------------

    @Override
    public int getItemViewType(int position) {
        return displayRows.get(position).viewType;
    }

    @Override
    public int getItemCount() {
        return displayRows.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                                      int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == VIEW_TYPE_HEADER_ACTIVE || viewType == VIEW_TYPE_HEADER_PAST) {
            View view = inflater.inflate(
                    R.layout.item_patient_medication_section_header,
                    parent,
                    false
            );
            return new SectionHeaderViewHolder(view);
        } else {
            View view = inflater.inflate(
                    R.layout.item_patient_medication_line,
                    parent,
                    false
            );
            return new MedLineViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder,
                                 int position) {
        Row row = displayRows.get(position);

        if (row.viewType == VIEW_TYPE_HEADER_ACTIVE) {
            ((SectionHeaderViewHolder) holder).bind(true);
        } else if (row.viewType == VIEW_TYPE_HEADER_PAST) {
            ((SectionHeaderViewHolder) holder).bind(false);
        } else if (row.viewType == VIEW_TYPE_LINE && row.line != null) {
            ((MedLineViewHolder) holder).bind(row.line, interactionListener);
        }
    }

    // ---------------------------------------------------------------------
    // ViewHolders
    // ---------------------------------------------------------------------

    static class SectionHeaderViewHolder extends RecyclerView.ViewHolder {
        private final TextView textHeader;

        SectionHeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            textHeader = itemView.findViewById(R.id.text_med_section_header);
        }

        void bind(boolean isActiveSection) {
            if (isActiveSection) {
                textHeader.setText(
                        itemView.getContext().getString(
                                R.string.patient_medications_section_active
                        )
                );
            } else {
                textHeader.setText(
                        itemView.getContext().getString(
                                R.string.patient_medications_section_past
                        )
                );
            }
        }
    }

    static class MedLineViewHolder extends RecyclerView.ViewHolder {

        private final TextView textMedicationName;
        private final TextView textDosageTimes;
        private final TextView textInstructions;
        private final TextView textDates;
        private final TextView textReminderTime;
        private final SwitchCompat switchReminder;

        MedLineViewHolder(@NonNull View itemView) {
            super(itemView);
            textMedicationName = itemView.findViewById(R.id.text_med_line_medication_name);
            textDosageTimes = itemView.findViewById(R.id.text_med_line_dosage_times);
            textInstructions = itemView.findViewById(R.id.text_med_line_instructions);
            textDates = itemView.findViewById(R.id.text_med_line_dates);
            textReminderTime = itemView.findViewById(R.id.text_med_line_reminder_time);
            switchReminder = itemView.findViewById(R.id.switch_med_line_reminder);
        }

        void bind(@Nullable PrescriptionLine line,
                  @Nullable OnMedicationInteractionListener interactionListener) {
            if (line == null) {
                textMedicationName.setText("");
                textDosageTimes.setText("");
                textInstructions.setText("");
                textDates.setText("");
                if (textReminderTime != null) {
                    textReminderTime.setText("");
                    textReminderTime.setVisibility(View.GONE);
                }
                switchReminder.setOnCheckedChangeListener(null);
                switchReminder.setChecked(false);
                return;
            }

            // Medication name
            String medName = line.getMedicationName();
            if (TextUtils.isEmpty(medName)) {
                medName = itemView.getContext()
                        .getString(R.string.patient_medication_name_placeholder);
            }
            textMedicationName.setText(medName);

            // Dosage + times per day
            String dosage = safeTrim(line.getDosage());
            Integer timesPerDay = line.getTimesPerDay();
            String timesLabel = null;
            if (timesPerDay != null && timesPerDay > 0) {
                timesLabel = itemView.getContext().getString(
                        R.string.patient_medication_times_per_day_format,
                        timesPerDay
                );
            }

            if (!TextUtils.isEmpty(dosage) && !TextUtils.isEmpty(timesLabel)) {
                textDosageTimes.setText(dosage + " · " + timesLabel);
            } else if (!TextUtils.isEmpty(dosage)) {
                textDosageTimes.setText(dosage);
            } else if (!TextUtils.isEmpty(timesLabel)) {
                textDosageTimes.setText(timesLabel);
            } else {
                textDosageTimes.setText("");
            }

            // Instructions
            String instructions = safeTrim(line.getInstructions());
            if (TextUtils.isEmpty(instructions)) {
                textInstructions.setVisibility(View.GONE);
            } else {
                textInstructions.setVisibility(View.VISIBLE);
                textInstructions.setText(instructions);
            }

            // Date range: uses raw ISO date strings from backend
            String start = safeTrim(line.getPrescriptionStartDate());
            String end = safeTrim(line.getPrescriptionEndDate());

            if (!TextUtils.isEmpty(start) && !TextUtils.isEmpty(end)) {
                textDates.setText(
                        itemView.getContext().getString(
                                R.string.patient_medication_dates_range_format,
                                start,
                                end
                        )
                );
            } else if (!TextUtils.isEmpty(start)) {
                textDates.setText(
                        itemView.getContext().getString(
                                R.string.patient_medication_dates_from_format,
                                start
                        )
                );
            } else if (!TextUtils.isEmpty(end)) {
                textDates.setText(
                        itemView.getContext().getString(
                                R.string.patient_medication_dates_until_format,
                                end
                        )
                );
            } else {
                textDates.setText("");
            }

            // Reminder time (from SharedPreferences, via scheduler)
            if (textReminderTime != null) {
                Long lineId = line.getId();
                if (lineId != null) {
                    String timeText = MedicationReminderScheduler
                            .getFormattedReminderTimeForLine(itemView.getContext(), lineId);
                    if (!TextUtils.isEmpty(timeText)) {
                        textReminderTime.setVisibility(View.VISIBLE);
                        textReminderTime.setText(
                                itemView.getContext().getString(
                                        R.string.patient_medication_reminder_time_format,
                                        timeText
                                )
                        );
                    } else {
                        textReminderTime.setVisibility(View.GONE);
                    }
                } else {
                    textReminderTime.setVisibility(View.GONE);
                }
            }

            // Reminder switch
            switchReminder.setOnCheckedChangeListener(null);
            switchReminder.setChecked(line.isReminderEnabledSafe());

            if (interactionListener != null) {
                switchReminder.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    interactionListener.onReminderToggled(line, isChecked);
                });
            }
        }

        private String safeTrim(@Nullable String s) {
            return s == null ? "" : s.trim();
        }
    }

    // ---------------------------------------------------------------------
    // Small helper
    // ---------------------------------------------------------------------

    private static String safeTrim(@Nullable String value) {
        if (value == null) return "";
        String trimmed = value.trim();
        return trimmed.isEmpty() ? "" : trimmed;
    }
}
