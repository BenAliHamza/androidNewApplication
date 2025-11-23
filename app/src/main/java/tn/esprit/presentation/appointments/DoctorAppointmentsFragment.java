package tn.esprit.presentation.appointments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import tn.esprit.R;
import tn.esprit.data.remote.appointment.AppointmentDto;
public class DoctorAppointmentsFragment extends Fragment {
    private AppointmentViewModel viewModel;
    private DoctorAppointmentsAdapter adapter;
    private RecyclerView rvAppointments;
    private ProgressBar progressBar;
    private TextView tvEmptyState;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_doctor_appointments, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvAppointments = view.findViewById(R.id.rvDoctorAppointments);
        progressBar = view.findViewById(R.id.progressLoading);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);

        rvAppointments.setLayoutManager(new LinearLayoutManager(requireContext()));

        viewModel = new ViewModelProvider(this).get(AppointmentViewModel.class);
        viewModel.init(requireContext());

        adapter = new DoctorAppointmentsAdapter(new DoctorAppointmentsAdapter.OnAppointmentActionListener() {
            @Override
            public void onAcceptClick(AppointmentDto appointment) {
                viewModel.accept(appointment.id);
                Toast.makeText(requireContext(),
                        "Acceptation en cours...", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onRejectClick(AppointmentDto appointment) {
                viewModel.reject(appointment.id);
                Toast.makeText(requireContext(),
                        "Refus en cours...", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCompleteClick(AppointmentDto appointment) {
                viewModel.complete(appointment.id);
                Toast.makeText(requireContext(),
                        "Marquage comme terminé...", Toast.LENGTH_SHORT).show();
            }
        });

        rvAppointments.setAdapter(adapter);

        // Observers
        viewModel.getAppointments().observe(getViewLifecycleOwner(), this::updateAppointmentsUI);
        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Erreur : " + error, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.getAppointmentActionResult().observe(getViewLifecycleOwner(), updated -> {
            if (updated != null) {
                Toast.makeText(requireContext(), "Statut mis à jour : " + updated.status,
                        Toast.LENGTH_SHORT).show();
                // Recharger la liste après une action
                loadAppointments();
            }
        });

        // Charger les rendez-vous au démarrage
        loadAppointments();
    }

    private void loadAppointments() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmptyState.setVisibility(View.GONE);
        viewModel.loadDoctorAppointments(null, null, null);
    }

    private void updateAppointmentsUI(List<AppointmentDto> list) {
        progressBar.setVisibility(View.GONE);
        if (list == null || list.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            adapter.setItems(null);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            adapter.setItems(list);
        }
    }
}
