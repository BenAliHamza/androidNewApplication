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
import androidx.navigation.fragment.NavHostFragment;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import tn.esprit.R;
import tn.esprit.domain.doctor.DoctorHomeStats;
import tn.esprit.presentation.appointment.AppointmentUiHelper;
import tn.esprit.presentation.appointment.DoctorHomeViewModel;

public class HomeFragment extends Fragment {

    private ProgressBar progressBar;
    private FrameLayout doctorContainer;
    private FrameLayout patientContainer;

    private TextView textStatToday;
    private TextView textStatWeek;
    private TextView textStatPatients;
    private TextView textNextTime;
    private TextView textNextPatient;

    private TextView textHighlightTitle;
    private TextView textHighlightSubtitle;

    private TextView chipQuickBook;
    private TextView chipQuickMessages;
    private TextView chipQuickReports;

    private DoctorHomeViewModel doctorHomeViewModel;

    private boolean doctorViewInflated = false;
    private boolean firstStatsApplied = false;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        progressBar = view.findViewById(R.id.home_progress);
        doctorContainer = view.findViewById(R.id.home_doctor_container);
        patientContainer = view.findViewById(R.id.home_patient_container);

        // Start with doctor container hidden to avoid showing fake zeros
        if (doctorContainer != null) {
            doctorContainer.setVisibility(View.INVISIBLE);
        }

        showDoctorHome();
    }

    private void showDoctorHome() {
        if (doctorContainer == null) return;

        doctorContainer.setVisibility(View.INVISIBLE); // will be made visible once stats are loaded
        if (patientContainer != null) {
            patientContainer.setVisibility(View.GONE);
        }

        if (!doctorViewInflated) {
            LayoutInflater.from(requireContext())
                    .inflate(R.layout.view_home_doctor_content, doctorContainer, true);
            doctorViewInflated = true;
        }

        // Bind views from the inflated doctor layout
        textStatToday = doctorContainer.findViewById(R.id.text_stat_today_count);
        textStatWeek = doctorContainer.findViewById(R.id.text_stat_week_count);
        textStatPatients = doctorContainer.findViewById(R.id.text_stat_patients_count);

        textNextTime = doctorContainer.findViewById(R.id.text_next_time);
        textNextPatient = doctorContainer.findViewById(R.id.text_next_patient);

        textHighlightTitle = doctorContainer.findViewById(R.id.text_highlight_title);
        textHighlightSubtitle = doctorContainer.findViewById(R.id.text_highlight_subtitle);

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
        doctorHomeViewModel.getStats().observe(getViewLifecycleOwner(), s -> {
            if (s != null) {
                bindStats(s);

                // First time we get real data -> show the doctor container
                if (!firstStatsApplied && doctorContainer != null) {
                    firstStatsApplied = true;
                    doctorContainer.setVisibility(View.VISIBLE);
                }
            }
        });

        doctorHomeViewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            if (progressBar != null) {
                progressBar.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE);
            }
        });

        doctorHomeViewModel.getErrorMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.trim().isEmpty()) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
                doctorHomeViewModel.clearError();
            }
        });
    }

    private void bindStats(@NonNull DoctorHomeStats s) {
        // static highlight for now (you can later personalize based on stats if you want)
        if (textHighlightTitle != null) {
            textHighlightTitle.setText(getString(R.string.home_highlight_title));
        }
        if (textHighlightSubtitle != null) {
            textHighlightSubtitle.setText(getString(R.string.home_highlight_subtitle));
        }

        if (textStatToday != null) {
            textStatToday.setText(String.valueOf(s.todayAppointments));
        }
        if (textStatWeek != null) {
            textStatWeek.setText(String.valueOf(s.weekAppointments));
        }
        if (textStatPatients != null) {
            textStatPatients.setText(String.valueOf(s.totalPatients));
        }

        if (textNextTime != null) {
            textNextTime.setText(formatNextAppointmentTime(s.nextAppointmentStart));
        }

        if (textNextPatient != null) {
            if (s.nextAppointmentPatientName != null && !s.nextAppointmentPatientName.isEmpty()) {
                textNextPatient.setText(s.nextAppointmentPatientName);
            } else {
                textNextPatient.setText("");
            }
        }
    }

    /**
     * Formats the next appointment start ISO string into a doctor-friendly label:
     * - If empty -> "No upcoming appointment"
     * - If today  -> "Today · HH:mm"
     * - Else      -> "EEE, d MMM · HH:mm"
     */
    @NonNull
    private String formatNextAppointmentTime(@Nullable String nextStartIso) {
        if (nextStartIso == null || nextStartIso.trim().isEmpty()) {
            return getString(R.string.home_next_appointment_none);
        }

        // Try to reuse AppointmentUiHelper.parseDate for consistency with the rest of the app
        Date date = AppointmentUiHelper.parseDate(nextStartIso);
        if (date == null) {
            // Fallback: show raw string if parsing fails
            return nextStartIso;
        }

        Calendar nowCal = Calendar.getInstance();
        Calendar targetCal = Calendar.getInstance();
        targetCal.setTime(date);

        boolean sameDay =
                nowCal.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR)
                        && nowCal.get(Calendar.DAY_OF_YEAR) == targetCal.get(Calendar.DAY_OF_YEAR);

        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String timePart = timeFormat.format(date);

        if (sameDay) {
            // Example: "Today · 09:30"
            return getString(R.string.home_next_appointment_today_label, timePart);
        } else {
            // Example: "Fri, 28 Nov · 09:30"
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, d MMM", Locale.getDefault());
            String datePart = dateFormat.format(date);
            return datePart + " · " + timePart;
        }
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
                Toast.LENGTH_SHORT
        ).show();
    }
}
