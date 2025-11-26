package tn.esprit.presentation.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import tn.esprit.R;
import tn.esprit.domain.patient.PatientProfile;

/**
 * Adapter for displaying the doctor's patients list.
 *
 * Uses a simple card layout (item_doctor_patient.xml) and exposes a click listener.
 */
public class DoctorPatientsAdapter extends RecyclerView.Adapter<DoctorPatientsAdapter.PatientViewHolder> {

    public interface OnPatientClickListener {
        void onPatientClick(@NonNull PatientProfile patient);
    }

    private final List<PatientProfile> patients = new ArrayList<>();
    private final OnPatientClickListener clickListener;

    public DoctorPatientsAdapter(@NonNull OnPatientClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public void setPatients(@NonNull List<PatientProfile> newPatients) {
        patients.clear();
        patients.addAll(newPatients);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PatientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_doctor_patient, parent, false);
        return new PatientViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PatientViewHolder holder, int position) {
        PatientProfile patient = patients.get(position);
        holder.bind(patient, clickListener);
    }

    @Override
    public int getItemCount() {
        return patients.size();
    }

    static class PatientViewHolder extends RecyclerView.ViewHolder {

        private final TextView textName;
        private final TextView textSubtitle;

        PatientViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.text_patient_name);
            textSubtitle = itemView.findViewById(R.id.text_patient_subtitle);
        }

        void bind(@NonNull PatientProfile patient,
                  @NonNull OnPatientClickListener clickListener) {
            // Name
            String fullName = patient.getFullName();
            if (fullName == null || fullName.trim().isEmpty()) {
                fullName = itemView.getContext().getString(R.string.profile_role_patient);
            }
            textName.setText(fullName);

            // Subtitle: "City, Country" or placeholder
            String subtitle = patient.getCityCountryLabel();
            if (subtitle == null || subtitle.trim().isEmpty()) {
                subtitle = "";
            }
            textSubtitle.setText(subtitle);

            itemView.setOnClickListener(v -> clickListener.onPatientClick(patient));
        }
    }
}
