package tn.esprit.presentation.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import tn.esprit.R;

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
 *  - Wires the "View all patients" CTA to navigate to DoctorPatientsFragment.
 */
public class HomeFragment extends Fragment {

    private ProgressBar progressBar;
    private FrameLayout doctorContainer;
    private FrameLayout patientContainer;

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

        // In the current navigation setup:
        //  - HomeGateFragment + MainActivity decide the role.
        //  - PATIENT => navigates to patientHomeFragment directly.
        //  - DOCTOR/unknown => navigates to this HomeFragment.
        // So here we only need to show the doctor home.
        showDoctorHome();
    }

    private void showDoctorHome() {
        if (doctorContainer == null) return;

        // Show doctor container, hide patient container
        doctorContainer.setVisibility(View.VISIBLE);
        if (patientContainer != null) {
            patientContainer.setVisibility(View.GONE);
        }

        // Hide progress
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }

        // Inflate doctor home content into the container the first time only
        if (doctorContainer.getChildCount() == 0) {
            LayoutInflater.from(requireContext())
                    .inflate(R.layout.view_home_doctor_content, doctorContainer, true);
        }

        // Wire "View all patients" CTA
        TextView viewAllPatients = doctorContainer.findViewById(R.id.text_view_all_patients);
        if (viewAllPatients != null) {
            viewAllPatients.setOnClickListener(v -> navigateToDoctorPatients());
        }
    }

    private void navigateToDoctorPatients() {
        NavController navController = NavHostFragment.findNavController(this);
        navController.navigate(R.id.doctorPatientsFragment);
    }
}
