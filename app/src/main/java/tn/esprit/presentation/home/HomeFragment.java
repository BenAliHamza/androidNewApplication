package tn.esprit.presentation.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import tn.esprit.R;
import tn.esprit.presentation.appointment.DoctorHomeStats;

/**
 * Role-aware Home container for doctor home UI.
 */
public class HomeFragment extends Fragment {

    private ProgressBar progressBar;
    private FrameLayout doctorContainer;
    private FrameLayout patientContainer;

    private TextView textStatToday;
    private TextView textStatWeek;
    private TextView textStatPatients;

    private TextView chipQuickBook;
    private TextView chipQuickMessages;
    private TextView chipQuickReports;

    private DoctorHomeViewModel doctorHomeViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        progressBar = view.findViewById(R.id.home_progress);
        doctorContainer = view.findViewById(R.id.home_doctor_container);
        patientContainer = view.findViewById(R.id.home_patient_container);

        showDoctorHome();
    }

    private void showDoctorHome() {
        doctorContainer.setVisibility(View.VISIBLE);
        if (patientContainer != null) patientContainer.setVisibility(View.GONE);

        if (doctorContainer.getChildCount() == 0) {
            LayoutInflater.from(requireContext())
                    .inflate(R.layout.view_home_doctor_content, doctorContainer, true);
        }

        textStatToday = doctorContainer.findViewById(R.id.text_stat_today_count);
        textStatWeek = doctorContainer.findViewById(R.id.text_stat_week_count);
        textStatPatients = doctorContainer.findViewById(R.id.text_stat_patients_count);

        chipQuickBook = doctorContainer.findViewById(R.id.chip_quick_book);
        chipQuickMessages = doctorContainer.findViewById(R.id.chip_quick_messages);
        chipQuickReports = doctorContainer.findViewById(R.id.chip_quick_reports);

        chipQuickBook.setOnClickListener(v -> openAppointments());
        chipQuickMessages.setOnClickListener(v -> showComingSoon());
        chipQuickReports.setOnClickListener(v -> showComingSoon());

        doctorContainer.findViewById(R.id.button_doctor_home_appointments)
                .setOnClickListener(v -> openAppointments());

        doctorContainer.findViewById(R.id.text_view_all_patients)
                .setOnClickListener(v -> openPatients());

        doctorHomeViewModel = new ViewModelProvider(this).get(DoctorHomeViewModel.class);

        observe();
        doctorHomeViewModel.loadStats();
    }

    private void observe() {
        doctorHomeViewModel.getStats().observe(getViewLifecycleOwner(), stats -> {
            if (stats != null) bindStats(stats);
        });

        doctorHomeViewModel.getLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (progressBar != null) {
                progressBar.setVisibility(Boolean.TRUE.equals(isLoading) ? View.VISIBLE : View.GONE);
            }
        });

        doctorHomeViewModel.getErrorMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.trim().isEmpty() && isAdded()) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
            }
            doctorHomeViewModel.clearError();
        });
    }

    private void bindStats(DoctorHomeStats s) {
        textStatToday.setText(String.valueOf(s.getTodayAppointments()));
        textStatWeek.setText(String.valueOf(s.getWeekAppointments()));
        textStatPatients.setText(String.valueOf(s.getPatientsWithAppointments()));
    }

    private void openAppointments() {
        NavHostFragment.findNavController(this)
                .navigate(R.id.doctorAppointmentsFragment);
    }

    private void openPatients() {
        NavHostFragment.findNavController(this)
                .navigate(R.id.doctorPatientsFragment);
    }

    private void showComingSoon() {
        Toast.makeText(requireContext(),
                R.string.home_bottom_history_coming_soon,
                Toast.LENGTH_SHORT).show();
    }
}
