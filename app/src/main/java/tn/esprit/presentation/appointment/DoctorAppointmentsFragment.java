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
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;

import java.util.List;

import tn.esprit.R;
import tn.esprit.domain.appointment.Appointment;

/**
 * Doctor "My schedule" screen.
 *
 * Tabs:
 *  - Today
 *  - Upcoming
 *  - Past
 *
 * Allows accepting / rejecting pending appointments, and opening patient profile.
 */
public class DoctorAppointmentsFragment extends Fragment {

    private static final String KEY_CURRENT_SECTION = "doctor_appointments_current_section";

    private DoctorAppointmentsViewModel viewModel;

    private TabLayout tabs;
    private RecyclerView recycler;
    private ProgressBar progressBar;
    private TextView textEmpty;

    private DoctorAppointmentAdapter adapter;

    private enum Section {
        TODAY,
        UPCOMING,
        PAST
    }

    private Section currentSection = Section.TODAY;

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

        tabs = view.findViewById(R.id.tabs_doctor_appointments);
        recycler = view.findViewById(R.id.recycler_doctor_appointments);
        progressBar = view.findViewById(R.id.doctor_appointments_progress);
        textEmpty = view.findViewById(R.id.text_doctor_appointments_empty);

        adapter = new DoctorAppointmentAdapter(
                new DoctorAppointmentAdapter.OnAppointmentActionListener() {
                    @Override
                    public void onAccept(@NonNull Appointment appointment) {
                        if (viewModel != null) {
                            viewModel.acceptAppointment(appointment);
                        }
                    }

                    @Override
                    public void onReject(@NonNull Appointment appointment) {
                        if (viewModel != null) {
                            viewModel.rejectAppointment(appointment);
                        }
                    }
                },
                this::openPatientProfileForAppointment
        );

        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(DoctorAppointmentsViewModel.class);

        // Restore current section (tab) if we have saved state
        if (savedInstanceState != null) {
            String secName = savedInstanceState.getString(KEY_CURRENT_SECTION);
            if (secName != null) {
                try {
                    currentSection = Section.valueOf(secName);
                } catch (IllegalArgumentException ignore) {
                    currentSection = Section.TODAY;
                }
            }
        }

        setupTabs();
        observeViewModel();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.loadAppointments();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_CURRENT_SECTION, currentSection.name());
    }

    private void setupTabs() {
        if (tabs == null) return;

        tabs.removeAllTabs();

        TabLayout.Tab tabToday = tabs.newTab()
                .setText(R.string.doctor_appointments_tab_today)
                .setTag(Section.TODAY);
        tabs.addTab(tabToday, currentSection == Section.TODAY);

        TabLayout.Tab tabUpcoming = tabs.newTab()
                .setText(R.string.doctor_appointments_tab_upcoming)
                .setTag(Section.UPCOMING);
        tabs.addTab(tabUpcoming, currentSection == Section.UPCOMING);

        TabLayout.Tab tabPast = tabs.newTab()
                .setText(R.string.doctor_appointments_tab_past)
                .setTag(Section.PAST);
        tabs.addTab(tabPast, currentSection == Section.PAST);

        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                Object tag = tab.getTag();
                if (tag instanceof Section) {
                    currentSection = (Section) tag;
                    updateListForCurrentSection();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // no-op
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                Object tag = tab.getTag();
                if (tag instanceof Section) {
                    currentSection = (Section) tag;
                    updateListForCurrentSection();
                }
            }
        });

        // Ensure list matches the restored section
        updateListForCurrentSection();
    }

    private void observeViewModel() {
        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            if (loading == null) return;
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        });

        viewModel.getTodayAppointments().observe(getViewLifecycleOwner(), list -> {
            if (currentSection == Section.TODAY) {
                showAppointments(list);
            }
        });

        viewModel.getUpcomingAppointments().observe(getViewLifecycleOwner(), list -> {
            if (currentSection == Section.UPCOMING) {
                showAppointments(list);
            }
        });

        viewModel.getPastAppointments().observe(getViewLifecycleOwner(), list -> {
            if (currentSection == Section.PAST) {
                showAppointments(list);
            }
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

    private void updateListForCurrentSection() {
        if (viewModel == null) return;
        switch (currentSection) {
            case TODAY:
                showAppointments(viewModel.getTodayAppointments().getValue());
                break;
            case UPCOMING:
                showAppointments(viewModel.getUpcomingAppointments().getValue());
                break;
            case PAST:
                showAppointments(viewModel.getPastAppointments().getValue());
                break;
        }
    }

    private void showAppointments(@Nullable List<Appointment> list) {
        if (list == null || list.isEmpty()) {
            adapter.submitList(null);
            textEmpty.setVisibility(View.VISIBLE);
            recycler.setVisibility(View.GONE);
        } else {
            adapter.submitList(list);
            textEmpty.setVisibility(View.GONE);
            recycler.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Open patient public profile from an appointment.
     * IMPORTANT: uses patientUserId (not patientId).
     */
    private void openPatientProfileForAppointment(@NonNull Appointment appointment) {
        Long patientUserId = appointment.getPatientUserId();
        if (patientUserId == null || patientUserId <= 0L) {
            return;
        }

        Bundle args = new Bundle();
        // nav_main.xml + PatientPublicProfileFragment expect "patientId" = User.id
        args.putLong("patientId", patientUserId);

        NavController navController = NavHostFragment.findNavController(this);
        navController.navigate(R.id.patientPublicProfileFragment, args);
    }
}
