package tn.esprit.presentation.appointment;

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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import tn.esprit.R;
import tn.esprit.domain.appointment.Appointment;

/**
 * Adapter for displaying patient's appointments.
 */
public class PatientAppointmentAdapter extends ListAdapter<Appointment, PatientAppointmentAdapter.AppointmentViewHolder> {

    public interface OnAppointmentClickListener {
        void onAppointmentClicked(@NonNull Appointment appointment);
    }

    private final OnAppointmentClickListener listener;

    private static final DiffUtil.ItemCallback<Appointment> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Appointment>() {
                @Override
                public boolean areItemsTheSame(@NonNull Appointment oldItem, @NonNull Appointment newItem) {
                    if (oldItem.getId() == null || newItem.getId() == null) {
                        return oldItem == newItem;
                    }
                    return oldItem.getId().equals(newItem.getId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull Appointment oldItem, @NonNull Appointment newItem) {
                    String s1 = oldItem.getStartAt();
                    String s2 = newItem.getStartAt();
                    String e1 = oldItem.getEndAt();
                    String e2 = newItem.getEndAt();
                    String st1 = oldItem.getStatus();
                    String st2 = newItem.getStatus();
                    return safeEquals(s1, s2)
                            && safeEquals(e1, e2)
                            && safeEquals(st1, st2);
                }

                private boolean safeEquals(@Nullable Object a, @Nullable Object b) {
                    if (a == b) return true;
                    if (a == null || b == null) return false;
                    return a.equals(b);
                }
            };

    public PatientAppointmentAdapter(@NonNull OnAppointmentClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public AppointmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_patient_appointment, parent, false);
        return new AppointmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppointmentViewHolder holder, int position) {
        Appointment appointment = getItem(position);
        holder.bind(appointment, listener);
    }

    static class AppointmentViewHolder extends RecyclerView.ViewHolder {

        private final MaterialCardView cardRoot;
        private final TextView textDoctorName;
        private final TextView textDateTime;
        private final TextView textStatusChip;
        private final TextView textTele;
        private final TextView textReason;

        private final SimpleDateFormat parser =
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
        private final SimpleDateFormat formatterDate =
                new SimpleDateFormat("EEE, d MMM", Locale.getDefault());
        private final SimpleDateFormat formatterTime =
                new SimpleDateFormat("HH:mm", Locale.getDefault());

        AppointmentViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.card_appointment_root);
            textDoctorName = itemView.findViewById(R.id.text_doctor_name);
            textDateTime = itemView.findViewById(R.id.text_date_time);
            textStatusChip = itemView.findViewById(R.id.text_status_chip);
            textTele = itemView.findViewById(R.id.text_teleconsultation);
            textReason = itemView.findViewById(R.id.text_reason);
        }

        void bind(@NonNull Appointment appointment,
                  @NonNull OnAppointmentClickListener listener) {

            String first = appointment.getDoctorFirstName();
            String last = appointment.getDoctorLastName();
            String name;
            if (first == null && last == null) {
                name = itemView.getContext().getString(R.string.profile_role_doctor);
            } else {
                StringBuilder b = new StringBuilder();
                if (first != null) b.append(first.trim());
                if (last != null) {
                    if (b.length() > 0) b.append(" ");
                    b.append(last.trim());
                }
                name = b.toString();
            }
            textDoctorName.setText(name);

            String dateTime = buildDateTimeDisplay(appointment.getStartAt(), appointment.getEndAt());
            textDateTime.setText(dateTime);

            // Status chip
            String status = appointment.getStatus();
            bindStatus(status);

            // Teleconsultation label
            Boolean tele = appointment.getTeleconsultation();
            if (tele != null && tele) {
                textTele.setVisibility(View.VISIBLE);
                textTele.setText(R.string.appointment_teleconsultation_label);
            } else {
                textTele.setVisibility(View.GONE);
            }

            // Reason
            String reason = appointment.getReason();
            if (reason != null && !reason.trim().isEmpty()) {
                textReason.setVisibility(View.VISIBLE);
                textReason.setText(reason.trim());
            } else {
                textReason.setVisibility(View.GONE);
            }

            cardRoot.setOnClickListener(v -> listener.onAppointmentClicked(appointment));
        }

        private String buildDateTimeDisplay(@Nullable String startIso, @Nullable String endIso) {
            if (startIso == null || startIso.trim().isEmpty()) {
                return "";
            }
            try {
                Date start = parser.parse(startIso);
                if (start == null) {
                    return startIso;
                }
                String datePart = formatterDate.format(start);
                String startTime = formatterTime.format(start);

                if (endIso != null && !endIso.trim().isEmpty()) {
                    Date end = parser.parse(endIso);
                    if (end != null) {
                        String endTime = formatterTime.format(end);
                        return datePart + "  •  " + startTime + " – " + endTime;
                    }
                }

                return datePart + "  •  " + startTime;
            } catch (ParseException e) {
                return startIso;
            }
        }

        private void bindStatus(@Nullable String statusRaw) {
            String label;
            if (statusRaw == null) {
                label = itemView.getContext().getString(R.string.appointment_status_unknown);
            } else {
                String s = statusRaw.toUpperCase(Locale.getDefault());
                switch (s) {
                    case "PENDING":
                        label = itemView.getContext().getString(R.string.appointment_status_pending);
                        break;
                    case "ACCEPTED":
                        label = itemView.getContext().getString(R.string.appointment_status_accepted);
                        break;
                    case "REJECTED":
                        label = itemView.getContext().getString(R.string.appointment_status_rejected);
                        break;
                    case "COMPLETED":
                        label = itemView.getContext().getString(R.string.appointment_status_completed);
                        break;
                    default:
                        label = itemView.getContext().getString(R.string.appointment_status_unknown);
                        break;
                }
            }
            textStatusChip.setText(label);
        }
    }
}
