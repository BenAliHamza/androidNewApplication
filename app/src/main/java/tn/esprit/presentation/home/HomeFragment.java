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
import tn.esprit.domain.appointment.DoctorHomeStats;

/**
 * Role-aware Home container.
 *
 * In the current flow:
 *  - PATIENT users are routed directly to PatientHomeFragment (not here) by HomeGate/MainActivity.
 *  - DOCTOR (and unknown) users land here, and we show the doctor home content.
 *
 * This fragment:
 *  - Shows a progress bar (kept for possible future async work).
 *  - Inflates the doctor-specific home layout into the doctor container.
 *  - Wires:
 *      - "View all patients" CTA -> DoctorPatientsFragment
 *      - "My appointments" button -> DoctorAppointmentsFragment
 *      - Highlight stats -> DoctorHomeViewModel (today / week / patients)
 */
public class HomeFragment extends Fragment {

    private ProgressBar progressBar;
    private FrameLayout doctorContainer;
    private FrameLayout patientContainer;

    // Doctor stats views
    @Nullable
    private TextView textStatTodayCount;
    @Nullable
    private TextView textStatWeekCount;
    @Nullable
    private TextView textStatPatientsCount;

    @Nullable
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

        // Current navigation: this fragment is used for doctors.
        showDoctorHome();
    }

    private void showDoctorHome() {
        if (doctorContainer == null) return;

        doctorContainer.setVisibility(View.VISIBLE);
        if (patientContainer != null) {
            patientContainer.setVisibility(View.GONE);
        }

        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }

        if (doctorContainer.getChildCount() == 0) {
            LayoutInflater.from(requireContext())
                    .inflate(R.layout.view_home_doctor_content, doctorContainer, true);
        }

        // Grab stats views
        textStatTodayCount = doctorContainer.findViewById(R.id.text_stat_today_count);
        textStatWeekCount = doctorContainer.findViewById(R.id.text_stat_week_count);
        textStatPatientsCount = doctorContainer.findViewById(R.id.text_stat_patients_count);

        // "View all patients"
        TextView viewAllPatients = doctorContainer.findViewById(R.id.text_view_all_patients);
        if (viewAllPatients != null) {
            viewAllPatients.setOnClickListener(v -> navigateToDoctorPatients());
        }

        // "My appointments" CTA
        View btnAppointments = doctorContainer.findViewById(R.id.button_doctor_home_appointments);
        if (btnAppointments != null) {
            btnAppointments.setOnClickListener(v -> navigateToDoctorAppointments());
        }

        // ViewModel for stats
        doctorHomeViewModel = new ViewModelProvider(this)
                .get(DoctorHomeViewModel.class);

        observeDoctorHomeStats();

        if (doctorHomeViewModel != null) {
            doctorHomeViewModel.loadStats();
        }
    }

    private void observeDoctorHomeStats() {
        if (doctorHomeViewModel == null) return;

        doctorHomeViewModel.getStats().observe(
                getViewLifecycleOwner(),
                stats -> {
                    if (stats == null) return;
                    bindStatsToViews(stats);
                }
        );

        doctorHomeViewModel.getErrorMessage().observe(
                getViewLifecycleOwner(),
                msg -> {
                    if (msg == null || msg.trim().isEmpty()) return;
                    if (isAdded()) {
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
                    }
                    doctorHomeViewModel.clearError();
                }
        );
    }

    private void bindStatsToViews(@NonNull DoctorHomeStats stats) {
        if (textStatTodayCount != null) {
            textStatTodayCount.setText(String.valueOf(stats.getTodayAppointments()));
        }
        if (textStatWeekCount != null) {
            textStatWeekCount.setText(String.valueOf(stats.getWeekAppointments()));
        }
        if (textStatPatientsCount != null) {
            textStatPatientsCount.setText(
                    String.valueOf(stats.getPatientsWithAppointments())
            );
        }
    }

    private void navigateToDoctorPatients() {
        NavController navController = NavHostFragment.findNavController(this);
        navController.navigate(R.id.doctorPatientsFragment);
    }

    private void navigateToDoctorAppointments() {
        NavController navController = NavHostFragment.findNavController(this);
        navController.navigate(R.id.doctorAppointmentsFragment);
    }
}
