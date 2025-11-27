package tn.esprit.presentation.appointment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import tn.esprit.R;
import tn.esprit.domain.appointment.Appointment;

/**
 * Patient "My appointments" screen.
 *
 * (Navigation wiring to bottom bar will be done separately.)
 */
public class PatientAppointmentsFragment extends Fragment {

    private PatientAppointmentsViewModel viewModel;

    private ProgressBar progressBar;
    private RecyclerView recyclerView;
    private TextView emptyText;

    private PatientAppointmentAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_patient_appointments, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        progressBar = view.findViewById(R.id.patient_appointments_progress);
        recyclerView = view.findViewById(R.id.recycler_patient_appointments);
        emptyText = view.findViewById(R.id.text_patient_appointments_empty);

        adapter = new PatientAppointmentAdapter(new PatientAppointmentAdapter.OnAppointmentClickListener() {
            @Override
            public void onAppointmentClicked(@NonNull Appointment appointment) {
                if (!isAdded()) return;

                // Confirm cancellation
                new AlertDialog.Builder(requireContext())
                        .setTitle("Cancel appointment")
                        .setMessage("Do you want to cancel this appointment?")
                        .setNegativeButton("Keep", null)
                        .setPositiveButton("Cancel appointment", (dialog, which) -> {
                            if (viewModel != null) {
                                viewModel.cancelAppointment(appointment);
                            }
                        })
                        .show();
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        viewModel = new ViewModelProvider(this)
                .get(PatientAppointmentsViewModel.class);

        observeViewModel();

        // Initial load
        viewModel.loadAppointments();
    }

    private void observeViewModel() {
        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            if (loading == null) return;
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        });

        viewModel.getAppointments().observe(getViewLifecycleOwner(), list -> {
            if (list == null || list.isEmpty()) {
                emptyText.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                emptyText.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }
            adapter.submitList(list);
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg == null || msg.trim().isEmpty()) return;
            if (isAdded()) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
            }
            viewModel.clearMessages();
        });

        viewModel.getActionMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg == null || msg.trim().isEmpty()) return;
            if (isAdded()) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
            }
            viewModel.clearMessages();
        });
    }
}
