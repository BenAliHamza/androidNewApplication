package tn.esprit.presentation.medication;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import tn.esprit.R;
import tn.esprit.domain.medication.PrescriptionLine;

/**
 * Adapter to render a list of PrescriptionLine items in the
 * patient's "My Medications" screen, with a reminder toggle.
 */
public class PatientMedicationAdapter
        extends ListAdapter<PrescriptionLine, PatientMedicationAdapter.MedLineViewHolder> {

    public interface OnMedicationInteractionListener {
        void onReminderToggled(@NonNull PrescriptionLine line, boolean enabled);
    }

    @Nullable
    private final OnMedicationInteractionListener interactionListener;

    public PatientMedicationAdapter(@Nullable OnMedicationInteractionListener interactionListener) {
        super(DIFF_CALLBACK);
        this.interactionListener = interactionListener;
    }

    private static final DiffUtil.ItemCallback<PrescriptionLine> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<PrescriptionLine>() {
                @Override
                public boolean areItemsTheSame(@NonNull PrescriptionLine oldItem,
                                               @NonNull PrescriptionLine newItem) {
                    Long oldId = oldItem.getId();
                    Long newId = newItem.getId();
                    if (oldId == null || newId == null) return false;
                    return oldId.equals(newId);
                }

                @Override
                public boolean areContentsTheSame(@NonNull PrescriptionLine oldItem,
                                                  @NonNull PrescriptionLine newItem) {
                    Long oldId = oldItem.getId();
                    Long newId = newItem.getId();
                    if (oldId == null || newId == null) return false;
                    return oldId.equals(newId);
                }
            };

    @NonNull
    @Override
    public MedLineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_patient_medication_line, parent, false);
        return new MedLineViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MedLineViewHolder holder, int position) {
        PrescriptionLine line = getItem(position);
        holder.bind(line, interactionListener);
    }

    static class MedLineViewHolder extends RecyclerView.ViewHolder {

        private final TextView textMedicationName;
        private final TextView textDosageTimes;
        private final TextView textInstructions;
        private final TextView textDates;
        private final SwitchCompat switchReminder;

        MedLineViewHolder(@NonNull View itemView) {
            super(itemView);
            textMedicationName = itemView.findViewById(R.id.text_med_line_medication_name);
            textDosageTimes = itemView.findViewById(R.id.text_med_line_dosage_times);
            textInstructions = itemView.findViewById(R.id.text_med_line_instructions);
            textDates = itemView.findViewById(R.id.text_med_line_dates);
            switchReminder = itemView.findViewById(R.id.switch_med_line_reminder);
        }

        void bind(@Nullable PrescriptionLine line,
                  @Nullable OnMedicationInteractionListener interactionListener) {
            if (line == null) {
                textMedicationName.setText("");
                textDosageTimes.setText("");
                textInstructions.setText("");
                textDates.setText("");
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
}
