package tn.esprit.presentation.appointment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.Locale;

import tn.esprit.R;
import tn.esprit.domain.appointment.Appointment;

/**
 * Adapter for displaying DOCTOR's appointments.
 *
 * - Shows patient name, time range, status, tele flag, reason.
 * - If status == PENDING and appointment is in the future -> shows Accept / Reject buttons.
 * - Card click opens patient profile.
 */
public class DoctorAppointmentAdapter extends ListAdapter<Appointment, DoctorAppointmentAdapter.AppointmentViewHolder> {

    private static final String TAG = "DoctorApptAdapter";

    public interface OnAppointmentActionListener {
        void onAccept(@NonNull Appointment appointment);

        void onReject(@NonNull Appointment appointment);
    }

    public interface OnAppointmentClickListener {
        void onAppointmentClick(@NonNull Appointment appointment);
    }

    private final OnAppointmentActionListener actionListener;
    private final OnAppointmentClickListener clickListener;

    private static final DiffUtil.ItemCallback<Appointment> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Appointment>() {
                @Override
                public boolean areItemsTheSame(@NonNull Appointment oldItem,
                                               @NonNull Appointment newItem) {
                    if (oldItem.getId() == null || newItem.getId() == null) {
                        return oldItem == newItem;
                    }
                    return oldItem.getId().equals(newItem.getId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull Appointment oldItem,
                                                  @NonNull Appointment newItem) {
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

    public DoctorAppointmentAdapter(@NonNull OnAppointmentActionListener actionListener,
                                    @NonNull OnAppointmentClickListener clickListener) {
        super(DIFF_CALLBACK);
        this.actionListener = actionListener;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public AppointmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_doctor_appointment, parent, false);
        return new AppointmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppointmentViewHolder holder, int position) {
        Appointment appointment = getItem(position);
        holder.bind(appointment, actionListener, clickListener);
    }

    static class AppointmentViewHolder extends RecyclerView.ViewHolder {

        private final MaterialCardView cardRoot;
        private final TextView textPatientName;
        private final TextView textDateTime;
        private final TextView textStatusChip;
        private final TextView textTele;
        private final TextView textReason;
        private final MaterialButton buttonAccept;
        private final MaterialButton buttonReject;
        private final View layoutActions; // parent layout for buttons

        AppointmentViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.card_appointment_root);
            textPatientName = itemView.findViewById(R.id.text_patient_name);
            textDateTime = itemView.findViewById(R.id.text_date_time);
            textStatusChip = itemView.findViewById(R.id.text_status_chip);
            textTele = itemView.findViewById(R.id.text_teleconsultation);
            textReason = itemView.findViewById(R.id.text_reason);
            buttonAccept = itemView.findViewById(R.id.button_accept);
            buttonReject = itemView.findViewById(R.id.button_reject);
            layoutActions = itemView.findViewById(R.id.layout_actions);
        }

        void bind(@NonNull Appointment appointment,
                  @NonNull OnAppointmentActionListener actionListener,
                  @NonNull OnAppointmentClickListener clickListener) {

            // Patient name
            String first = appointment.getPatientFirstName();
            String last = appointment.getPatientLastName();
            String name;
            if ((first == null || first.trim().isEmpty())
                    && (last == null || last.trim().isEmpty())) {
                name = itemView.getContext().getString(R.string.profile_role_patient);
            } else {
                StringBuilder b = new StringBuilder();
                if (first != null && !first.trim().isEmpty()) {
                    b.append(first.trim());
                }
                if (last != null && !last.trim().isEmpty()) {
                    if (b.length() > 0) b.append(" ");
                    b.append(last.trim());
                }
                name = b.toString();
            }
            textPatientName.setText(name);

            // Date/time – use shared helper for consistent formatting
            String dateTime = AppointmentUiHelper.buildDateTimeDisplay(
                    appointment.getStartAt(),
                    appointment.getEndAt(),
                    itemView.getContext()
            );
            textDateTime.setText(dateTime);

            // Status chip
            bindStatus(appointment.getStatus());

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

            // Determine status + whether appointment is in the past
            String statusRaw = appointment.getStatus();
            String status = statusRaw != null
                    ? statusRaw.toUpperCase(Locale.getDefault())
                    : "";
            boolean isPending = "PENDING".equals(status);

            // ✅ FIX: pass the ISO string, not the Appointment object
            boolean isPast = AppointmentUiHelper.isInPast(appointment.getStartAt());

            Log.d(TAG, "bind: id=" + appointment.getId()
                    + " statusRaw=" + statusRaw
                    + " status=" + status
                    + " isPending=" + isPending
                    + " isPast=" + isPast);

            // Slightly dim past appointments visually
            if (isPast) {
                cardRoot.setAlpha(0.7f);
            } else {
                cardRoot.setAlpha(1f);
            }

            // Accept / Reject visibility & click:
            // only for PENDING + not in past
            if (isPending && !isPast) {
                if (layoutActions != null) {
                    layoutActions.setVisibility(View.VISIBLE);
                }
                buttonAccept.setVisibility(View.VISIBLE);
                buttonReject.setVisibility(View.VISIBLE);

                buttonAccept.setOnClickListener(v -> {
                    Log.d(TAG, "Accept clicked for id=" + appointment.getId());
                    actionListener.onAccept(appointment);
                });
                buttonReject.setOnClickListener(v -> {
                    Log.d(TAG, "Reject clicked for id=" + appointment.getId());
                    actionListener.onReject(appointment);
                });
            } else {
                if (layoutActions != null) {
                    layoutActions.setVisibility(View.GONE);
                }
                buttonAccept.setVisibility(View.GONE);
                buttonReject.setVisibility(View.GONE);
                buttonAccept.setOnClickListener(null);
                buttonReject.setOnClickListener(null);
            }

            // Card click -> open patient profile
            cardRoot.setOnClickListener(v -> clickListener.onAppointmentClick(appointment));
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
