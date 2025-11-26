package tn.esprit.presentation.home;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import tn.esprit.R;
import tn.esprit.domain.medication.Prescription;
import tn.esprit.domain.medication.PrescriptionLine;

/**
 * Adapter to show prescriptions for a patient (doctor view).
 * Supports long-press callbacks for delete / actions.
 */
public class PatientPrescriptionsAdapter
        extends ListAdapter<Prescription, PatientPrescriptionsAdapter.PrescriptionViewHolder> {

    public interface OnPrescriptionInteractionListener {
        void onPrescriptionLongClick(@NonNull Prescription prescription);
    }

    private final OnPrescriptionInteractionListener interactionListener;

    public PatientPrescriptionsAdapter(
            @Nullable OnPrescriptionInteractionListener interactionListener
    ) {
        super(DIFF_CALLBACK);
        this.interactionListener = interactionListener;
    }

    private static final DiffUtil.ItemCallback<Prescription> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Prescription>() {
                @Override
                public boolean areItemsTheSame(@NonNull Prescription oldItem,
                                               @NonNull Prescription newItem) {
                    Long oldId = oldItem.getId();
                    Long newId = newItem.getId();
                    return oldId != null && oldId.equals(newId);
                }

                @Override
                public boolean areContentsTheSame(@NonNull Prescription oldItem,
                                                  @NonNull Prescription newItem) {
                    Long oldId = oldItem.getId();
                    Long newId = newItem.getId();
                    return oldId != null && oldId.equals(newId);
                }
            };

    @NonNull
    @Override
    public PrescriptionViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                                     int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_patient_prescription, parent, false);
        return new PrescriptionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PrescriptionViewHolder holder,
                                 int position) {
        Prescription item = getItem(position);
        holder.bind(item, interactionListener);
    }

    static class PrescriptionViewHolder extends RecyclerView.ViewHolder {

        private final TextView textDates;
        private final TextView textNote;
        private final TextView textMedications;
        private final TextView textMeta;

        PrescriptionViewHolder(@NonNull View itemView) {
            super(itemView);
            textDates = itemView.findViewById(R.id.text_prescription_dates);
            textNote = itemView.findViewById(R.id.text_prescription_note);
            textMedications = itemView.findViewById(R.id.text_prescription_medications);
            textMeta = itemView.findViewById(R.id.text_prescription_meta);
        }

        void bind(@Nullable Prescription prescription,
                  @Nullable OnPrescriptionInteractionListener interactionListener) {
            if (prescription == null) {
                textDates.setText("");
                textNote.setText("");
                textMedications.setText("");
                textMeta.setText("");
                itemView.setOnLongClickListener(null);
                return;
            }

            // Dates (raw ISO yyyy-MM-dd for now)
            String start = safe(prescription.getStartDate());
            String end = safe(prescription.getEndDate());
            if (!start.isEmpty() && !end.isEmpty()) {
                textDates.setText(start + " - " + end);
            } else if (!start.isEmpty()) {
                textDates.setText(start);
            } else if (!end.isEmpty()) {
                textDates.setText(end);
            } else {
                textDates.setText("");
            }

            // Note
            String note = safe(prescription.getNote());
            if (note.isEmpty()) {
                textNote.setVisibility(View.GONE);
            } else {
                textNote.setVisibility(View.VISIBLE);
                textNote.setText(note);
            }

            // Medications summary: join up to 3 medication names
            List<PrescriptionLine> lines = prescription.getLines();
            if (lines == null || lines.isEmpty()) {
                textMedications.setText("");
            } else {
                StringBuilder sb = new StringBuilder();
                int count = 0;
                for (PrescriptionLine line : lines) {
                    if (line == null) continue;
                    String medName = safe(line.getMedicationName());
                    String dosage = safe(line.getDosage());

                    if (medName.isEmpty() && dosage.isEmpty()) continue;

                    if (sb.length() > 0) {
                        sb.append(" · ");
                    }
                    if (!medName.isEmpty() && !dosage.isEmpty()) {
                        sb.append(medName).append(" ").append(dosage);
                    } else if (!medName.isEmpty()) {
                        sb.append(medName);
                    } else {
                        sb.append(dosage);
                    }

                    count++;
                    if (count >= 3) break;
                }
                textMedications.setText(sb.toString());
            }

            // Meta: "<N> medications · until <date>" or just count
            int totalLines = lines != null ? lines.size() : 0;
            StringBuilder metaBuilder = new StringBuilder();
            if (totalLines > 0) {
                String label = itemView.getContext()
                        .getResources()
                        .getQuantityString(
                                R.plurals.patient_prescription_medications_count,
                                totalLines,
                                totalLines
                        );
                metaBuilder.append(totalLines)
                        .append(" ")
                        .append(label);
            }

            String endDate = safe(prescription.getEndDate());
            if (!endDate.isEmpty()) {
                if (metaBuilder.length() > 0) {
                    metaBuilder.append(" · ");
                }
                metaBuilder.append(
                        itemView.getContext().getString(
                                R.string.patient_prescription_status_until,
                                endDate
                        )
                );
            }

            textMeta.setText(metaBuilder.toString());

            // Long-press interaction (delete, etc.)
            if (interactionListener != null) {
                itemView.setOnLongClickListener(v -> {
                    interactionListener.onPrescriptionLongClick(prescription);
                    return true;
                });
            } else {
                itemView.setOnLongClickListener(null);
            }
        }

        private String safe(@Nullable String s) {
            if (s == null) return "";
            String trimmed = s.trim();
            return TextUtils.isEmpty(trimmed) ? "" : trimmed;
        }
    }
}
