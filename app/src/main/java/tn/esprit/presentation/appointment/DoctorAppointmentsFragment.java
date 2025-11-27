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
                        if (viewModel != null) viewModel.acceptAppointment(appointment);
                    }

                    @Override
                    public void onReject(@NonNull Appointment appointment) {
                        if (viewModel != null) viewModel.rejectAppointment(appointment);
                    }
                },
                this::openPatientProfileForAppointment
        );

        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        recycler.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(DoctorAppointmentsViewModel.class);

        if (savedInstanceState != null) {
            try {
                currentSection = Section.valueOf(
                        savedInstanceState.getString(KEY_CURRENT_SECTION, "TODAY")
                );
            } catch (Exception ignore) {
                currentSection = Section.TODAY;
            }
        }

        setupTabs();
        observeViewModel();

        viewModel.loadAppointments();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) viewModel.loadAppointments();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString(KEY_CURRENT_SECTION, currentSection.name());
        super.onSaveInstanceState(outState);
    }

    private void setupTabs() {
        tabs.removeAllTabs();

        addTab(Section.TODAY, getString(R.string.doctor_appointments_tab_today));
        addTab(Section.UPCOMING, getString(R.string.doctor_appointments_tab_upcoming));
        addTab(Section.PAST, getString(R.string.doctor_appointments_tab_past));

        TabLayout.Tab tab = tabs.getTabAt(currentSection.ordinal());
        if (tab != null) tab.select();

        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab selectedTab) {
                Section sec = (Section) selectedTab.getTag();
                if (sec != null) {
                    currentSection = sec;
                    updateListForCurrentSection();
                }
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {
                Section sec = (Section) tab.getTag();
                if (sec != null) {
                    currentSection = sec;
                    updateListForCurrentSection();
                }
            }
        });
    }

    private void addTab(Section section, String label) {
        TabLayout.Tab tab = tabs.newTab();
        tab.setText(label);
        tab.setTag(section);
        tabs.addTab(tab, section == currentSection);
    }

    private void observeViewModel() {

        viewModel.getLoading().observe(getViewLifecycleOwner(), loading ->
                progressBar.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE)
        );

        viewModel.getTodayAppointments().observe(getViewLifecycleOwner(), list -> {
            if (currentSection == Section.TODAY) showAppointments(list);
        });

        viewModel.getUpcomingAppointments().observe(getViewLifecycleOwner(), list -> {
            if (currentSection == Section.UPCOMING) showAppointments(list);
        });

        viewModel.getPastAppointments().observe(getViewLifecycleOwner(), list -> {
            if (currentSection == Section.PAST) showAppointments(list);
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.trim().isEmpty()) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
                viewModel.clearMessages();
            }
        });

        viewModel.getActionMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.trim().isEmpty()) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
                viewModel.clearMessages();
            }
        });
    }

    private void updateListForCurrentSection() {
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
            textEmpty.setVisibility(View.VISIBLE);
            recycler.setVisibility(View.GONE);
            adapter.submitList(null);
        } else {
            textEmpty.setVisibility(View.GONE);
            recycler.setVisibility(View.VISIBLE);
            adapter.submitList(list);
        }
    }

    private void openPatientProfileForAppointment(@NonNull Appointment appointment) {
        Long patientUserId = appointment.getPatientUserId();
        if (patientUserId == null || patientUserId <= 0L) return;

        Bundle args = new Bundle();
        args.putLong("patientId", patientUserId);

        NavController navController = NavHostFragment.findNavController(this);
        navController.navigate(R.id.patientPublicProfileFragment, args);
    }
}
