package tn.esprit.presentation.appointment;

import android.app.DatePickerDialog;
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import tn.esprit.R;
import tn.esprit.domain.appointment.Appointment;

public class DoctorAppointmentsFragment extends Fragment {

    private static final String KEY_CURRENT_SECTION = "doctor_appointments_current_section";

    private DoctorAppointmentsViewModel viewModel;

    private TabLayout tabs;
    private RecyclerView recycler;
    private ProgressBar progressBar;
    private TextView textEmpty;

    // Header views
    private TextView overviewTitle;
    private TextView overviewCounts;
    private TextView pickDateText;
    private TextView clearFilterText;

    private DoctorAppointmentAdapter adapter;

    private enum Section {
        TODAY,
        UPCOMING,
        PAST
    }

    private Section currentSection = Section.TODAY;

    // Cached lists for overview and section switching
    @Nullable
    private List<Appointment> todayList = Collections.emptyList();
    @Nullable
    private List<Appointment> upcomingList = Collections.emptyList();
    @Nullable
    private List<Appointment> pastList = Collections.emptyList();
    @Nullable
    private List<Appointment> filteredList = Collections.emptyList();

    private boolean filteredMode = false;
    @Nullable
    private String selectedDateIso;

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

        overviewTitle = view.findViewById(R.id.text_doctor_appointments_overview_title);
        overviewCounts = view.findViewById(R.id.text_doctor_appointments_overview_counts);
        pickDateText = view.findViewById(R.id.text_doctor_appointments_pick_date);
        clearFilterText = view.findViewById(R.id.text_doctor_appointments_filter_clear);

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
        setupHeaderClicks();
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
                    updateListAndOverview();
                }
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {
                Section sec = (Section) tab.getTag();
                if (sec != null) {
                    currentSection = sec;
                    updateListAndOverview();
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

    private void setupHeaderClicks() {
        // Dedicated "Choose date" chip
        if (pickDateText != null) {
            pickDateText.setOnClickListener(v -> openDatePicker());
        }

        // Title & counts clickable too
        if (overviewTitle != null) {
            overviewTitle.setOnClickListener(v -> openDatePicker());
        }
        if (overviewCounts != null) {
            overviewCounts.setOnClickListener(v -> openDatePicker());
        }

        // Clear filter chip
        if (clearFilterText != null) {
            clearFilterText.setOnClickListener(v -> {
                if (viewModel != null) {
                    viewModel.clearDateFilter();
                }
            });
        }
    }

    private void observeViewModel() {

        viewModel.getLoading().observe(getViewLifecycleOwner(), loading ->
                progressBar.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE)
        );

        viewModel.getTodayAppointments().observe(getViewLifecycleOwner(), list -> {
            todayList = list != null ? list : Collections.emptyList();
            if (!filteredMode && currentSection == Section.TODAY) {
                updateListAndOverview();
            } else if (!filteredMode) {
                updateOverviewOnly();
            }
        });

        viewModel.getUpcomingAppointments().observe(getViewLifecycleOwner(), list -> {
            upcomingList = list != null ? list : Collections.emptyList();
            if (!filteredMode && currentSection == Section.UPCOMING) {
                updateListAndOverview();
            } else if (!filteredMode) {
                updateOverviewOnly();
            }
        });

        viewModel.getPastAppointments().observe(getViewLifecycleOwner(), list -> {
            pastList = list != null ? list : Collections.emptyList();
            if (!filteredMode && currentSection == Section.PAST) {
                updateListAndOverview();
            } else if (!filteredMode) {
                updateOverviewOnly();
            }
        });

        viewModel.getFilteredMode().observe(getViewLifecycleOwner(), isFiltered -> {
            filteredMode = Boolean.TRUE.equals(isFiltered);
            updateListAndOverview();
        });

        viewModel.getSelectedDateIso().observe(getViewLifecycleOwner(), iso -> {
            selectedDateIso = iso;
            updateOverviewOnly();
        });

        viewModel.getFilteredAppointments().observe(getViewLifecycleOwner(), list -> {
            filteredList = list != null ? list : Collections.emptyList();
            if (filteredMode) {
                updateListAndOverview();
            }
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

    private void updateListAndOverview() {
        List<Appointment> listToShow;

        if (filteredMode) {
            listToShow = filteredList != null ? filteredList : Collections.emptyList();
        } else {
            switch (currentSection) {
                case TODAY:
                    listToShow = todayList != null ? todayList : Collections.emptyList();
                    break;
                case UPCOMING:
                    listToShow = upcomingList != null ? upcomingList : Collections.emptyList();
                    break;
                case PAST:
                default:
                    listToShow = pastList != null ? pastList : Collections.emptyList();
                    break;
            }
        }

        showAppointments(listToShow);
        updateOverviewOnly();
    }

    private void updateOverviewOnly() {
        if (!isAdded()) return;

        if (filteredMode) {
            if (clearFilterText != null) {
                clearFilterText.setVisibility(View.VISIBLE);
            }

            String labelDate;
            if (selectedDateIso != null && !selectedDateIso.trim().isEmpty()) {
                labelDate = AppointmentUiHelper.formatIsoDateToPretty(selectedDateIso);
            } else {
                labelDate = getString(R.string.doctor_appointments_overview_filtered_unknown_date);
            }

            if (overviewTitle != null) {
                overviewTitle.setText(
                        getString(R.string.doctor_appointments_overview_filtered_title, labelDate)
                );
            }

            int count = filteredList != null ? filteredList.size() : 0;
            if (overviewCounts != null) {
                overviewCounts.setText(
                        getString(R.string.doctor_appointments_overview_filtered_count, count)
                );
            }
        } else {
            if (clearFilterText != null) {
                clearFilterText.setVisibility(View.GONE);
            }

            String todayIso = AppointmentUiHelper.getTodayDatePrefix();
            String todayPretty = AppointmentUiHelper.formatIsoDateToPretty(todayIso);

            if (overviewTitle != null) {
                overviewTitle.setText(
                        getString(R.string.doctor_appointments_overview_title_default, todayPretty)
                );
            }

            int todayCount = todayList != null ? todayList.size() : 0;
            int upcomingCount = upcomingList != null ? upcomingList.size() : 0;
            int pastCount = pastList != null ? pastList.size() : 0;

            if (overviewCounts != null) {
                overviewCounts.setText(
                        getString(R.string.doctor_appointments_overview_counts,
                                todayCount, upcomingCount, pastCount)
                );
            }
        }
    }

    private void showAppointments(@Nullable List<Appointment> list) {
        List<Appointment> safeList = (list != null) ? list : Collections.emptyList();

        if (safeList.isEmpty()) {
            textEmpty.setVisibility(View.VISIBLE);
            recycler.setVisibility(View.GONE);
            adapter.submitList(Collections.emptyList());
        } else {
            textEmpty.setVisibility(View.GONE);
            recycler.setVisibility(View.VISIBLE);
            adapter.submitList(safeList);
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

    // -------------------------------------------------------------------------
    // Date picker
    // -------------------------------------------------------------------------

    private void openDatePicker() {
        if (!isAdded()) return;

        final Calendar cal = Calendar.getInstance();

        String baseIso = selectedDateIso;
        if (baseIso == null || baseIso.trim().isEmpty()) {
            baseIso = AppointmentUiHelper.getTodayDatePrefix();
        }

        try {
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date d = fmt.parse(baseIso);
            if (d != null) {
                cal.setTime(d);
            }
        } catch (ParseException ignored) {
        }

        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dialog = new DatePickerDialog(
                requireContext(),
                (view, y, m, dayOfMonth) -> {
                    Calendar chosen = Calendar.getInstance();
                    chosen.set(Calendar.YEAR, y);
                    chosen.set(Calendar.MONTH, m);
                    chosen.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    SimpleDateFormat out = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    String iso = out.format(chosen.getTime());

                    if (viewModel != null) {
                        viewModel.filterByDate(iso);
                    }
                },
                year, month, day
        );
        dialog.show();
    }
}
