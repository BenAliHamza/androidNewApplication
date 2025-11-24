package tn.esprit.presentation.indicator;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.math.BigDecimal;

import tn.esprit.R;
import tn.esprit.domain.indicator.PatientIndicator;

/**
 * Simple adapter to render a list of PatientIndicator items.
 */
public class PatientIndicatorAdapter
        extends ListAdapter<PatientIndicator, PatientIndicatorAdapter.IndicatorViewHolder> {

    public PatientIndicatorAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<PatientIndicator> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<PatientIndicator>() {
                @Override
                public boolean areItemsTheSame(@NonNull PatientIndicator oldItem,
                                               @NonNull PatientIndicator newItem) {
                    Long oldId = oldItem.getId();
                    Long newId = newItem.getId();
                    if (oldId == null || newId == null) return false;
                    return oldId.equals(newId);
                }

                @Override
                public boolean areContentsTheSame(@NonNull PatientIndicator oldItem,
                                                  @NonNull PatientIndicator newItem) {
                    // For now, simple id-based check is enough for small lists.
                    Long oldId = oldItem.getId();
                    Long newId = newItem.getId();
                    if (oldId == null || newId == null) return false;
                    return oldId.equals(newId);
                }
            };

    @NonNull
    @Override
    public IndicatorViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                                  int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_patient_indicator, parent, false);
        return new IndicatorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull IndicatorViewHolder holder,
                                 int position) {
        PatientIndicator item = getItem(position);
        holder.bind(item);
    }

    static class IndicatorViewHolder extends RecyclerView.ViewHolder {

        private final TextView textNameValue;
        private final TextView textMeasuredAt;
        private final TextView textNote;

        IndicatorViewHolder(@NonNull View itemView) {
            super(itemView);
            textNameValue = itemView.findViewById(R.id.text_indicator_name_value);
            textMeasuredAt = itemView.findViewById(R.id.text_indicator_measured_at);
            textNote = itemView.findViewById(R.id.text_indicator_note);
        }

        void bind(@Nullable PatientIndicator indicator) {
            if (indicator == null) {
                textNameValue.setText("");
                textMeasuredAt.setText("");
                textNote.setText("");
                textNote.setVisibility(View.GONE);
                return;
            }

            // Name + value
            String name = indicator.getIndicatorName();
            if (name == null) name = "";

            String valuePart = buildValueString(
                    indicator.getNumericValue(),
                    indicator.getUnit(),
                    indicator.getTextValue()
            );

            if (!name.isEmpty() && !valuePart.isEmpty()) {
                textNameValue.setText(name + " • " + valuePart);
            } else if (!name.isEmpty()) {
                textNameValue.setText(name);
            } else {
                textNameValue.setText(valuePart);
            }

            // Measured at
            String measuredAt = indicator.getMeasuredAt();
            if (measuredAt == null) measuredAt = "";
            textMeasuredAt.setText(measuredAt);

            // Note
            String note = indicator.getNote();
            if (note == null || note.trim().isEmpty()) {
                textNote.setVisibility(View.GONE);
            } else {
                textNote.setVisibility(View.VISIBLE);
                textNote.setText(note.trim());
            }
        }

        private String buildValueString(@Nullable BigDecimal numericValue,
                                        @Nullable String unit,
                                        @Nullable String textValue) {
            String numeric = null;
            if (numericValue != null) {
                numeric = numericValue.stripTrailingZeros().toPlainString();
            }

            String u = unit != null ? unit.trim() : "";
            String tv = textValue != null ? textValue.trim() : "";

            if (numeric != null && !numeric.isEmpty()) {
                if (!u.isEmpty()) {
                    return numeric + " " + u;
                }
                return numeric;
            }

            return tv;
        }
    }
}
