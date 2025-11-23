package tn.esprit.presentation.appointments;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import tn.esprit.R;
import tn.esprit.data.remote.appointment.AppointmentDto;
public class DoctorAppointmentsAdapter
        extends RecyclerView.Adapter<DoctorAppointmentsAdapter.AppointmentViewHolder{
    public interface OnAppointmentActionListener {
        void onAcceptClick(AppointmentDto appointment);
        void onRejectClick(AppointmentDto appointment);
        void onCompleteClick(AppointmentDto appointment);
    }

    private final List<AppointmentDto> items = new ArrayList<>();
    private final OnAppointmentActionListener listener;

    public DoctorAppointmentsAdapter(OnAppointmentActionListener listener) {
        this.listener = listener;
    }

    public void setItems(List<AppointmentDto> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
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
        AppointmentDto item = items.get(position);

        holder.tvPatientName.setText(item.patientFullName);
        holder.tvStatus.setText(item.status);

        String dateTime = item.date + " • " + item.startTime + " - " + item.endTime;
        holder.tvDateTime.setText(dateTime);

        if (item.reason != null && !item.reason.isEmpty()) {
            holder.tvReason.setText(item.reason);
        } else {
            holder.tvReason.setText("Aucune raison fournie");
        }

        holder.btnAccept.setOnClickListener(v -> {
            if (listener != null) listener.onAcceptClick(item);
        });

        holder.btnReject.setOnClickListener(v -> {
            if (listener != null) listener.onRejectClick(item);
        });

        holder.btnComplete.setOnClickListener(v -> {
            if (listener != null) listener.onCompleteClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class AppointmentViewHolder extends RecyclerView.ViewHolder {

        TextView tvPatientName;
        TextView tvStatus;
        TextView tvDateTime;
        TextView tvReason;
        Button btnAccept;
        Button btnReject;
        Button btnComplete;

        public AppointmentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPatientName = itemView.findViewById(R.id.tvPatientName);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
            tvReason = itemView.findViewById(R.id.tvReason);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnReject = itemView.findViewById(R.id.btnReject);
            btnComplete = itemView.findViewById(R.id.btnComplete);
        }
    }

}
