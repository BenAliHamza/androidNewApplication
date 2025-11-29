package tn.esprit.presentation.history;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
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
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import tn.esprit.MainActivity;
import tn.esprit.R;
import tn.esprit.domain.history.UserHistoryEntry;

/**
 * Screen showing the authenticated user's history
 * (appointments, profile changes, medications, etc.).
 */
public class UserHistoryFragment extends Fragment {

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView textEmpty;

    // Filters
    private ChipGroup chipGroupFilters;
    private Chip chipFilterAll;
    private Chip chipFilterAppointments;
    private Chip chipFilterMedications;
    private Chip chipFilterIndicators;
    private Chip chipFilterProfile;
    private Chip chipFilterOther;

    private UserHistoryViewModel viewModel;
    private UserHistoryAdapter adapter;

    // Full list from backend (unfiltered)
    private final List<UserHistoryEntry> fullHistory = new ArrayList<>();

    private enum FilterType {
        ALL,
        APPOINTMENTS,
        MEDICATIONS,
        INDICATORS,
        PROFILE,
        OTHER
    }

    private FilterType currentFilter = FilterType.ALL;

    public UserHistoryFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_user_history, container, false);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        swipeRefreshLayout = view.findViewById(R.id.history_swipe_refresh);
        recyclerView = view.findViewById(R.id.recycler_user_history);
        progressBar = view.findViewById(R.id.history_progress);
        textEmpty = view.findViewById(R.id.text_history_empty);

        chipGroupFilters = view.findViewById(R.id.history_chip_group_filters);
        chipFilterAll = view.findViewById(R.id.chip_history_filter_all);
        chipFilterAppointments = view.findViewById(R.id.chip_history_filter_appointments);
        chipFilterMedications = view.findViewById(R.id.chip_history_filter_medications);
        chipFilterIndicators = view.findViewById(R.id.chip_history_filter_indicators);
        chipFilterProfile = view.findViewById(R.id.chip_history_filter_profile);
        chipFilterOther = view.findViewById(R.id.chip_history_filter_other);

        // Recycler setup
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new UserHistoryAdapter(this::onHistoryItemClicked);
        recyclerView.setAdapter(adapter);

        // Swipe-to-refresh
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (viewModel != null) {
                viewModel.reloadHistory();
            }
        });

        // Filters behavior
        setupFilters();

        // ViewModel
        viewModel = new ViewModelProvider(this).get(UserHistoryViewModel.class);
        observeViewModel();

        // Initial load
        viewModel.reloadHistory();
    }

    // ---------------------------------------------------------------------
    // Filters
    // ---------------------------------------------------------------------

    private void setupFilters() {
        if (chipGroupFilters == null) return;

        // Default selection
        if (chipFilterAll != null) {
            chipFilterAll.setChecked(true);
        }
        currentFilter = FilterType.ALL;

        if (chipFilterAll != null) {
            chipFilterAll.setOnClickListener(v -> {
                currentFilter = FilterType.ALL;
                applyFilterAndUpdateList();
            });
        }

        if (chipFilterAppointments != null) {
            chipFilterAppointments.setOnClickListener(v -> {
                currentFilter = FilterType.APPOINTMENTS;
                applyFilterAndUpdateList();
            });
        }

        if (chipFilterMedications != null) {
            chipFilterMedications.setOnClickListener(v -> {
                currentFilter = FilterType.MEDICATIONS;
                applyFilterAndUpdateList();
            });
        }

        if (chipFilterIndicators != null) {
            chipFilterIndicators.setOnClickListener(v -> {
                currentFilter = FilterType.INDICATORS;
                applyFilterAndUpdateList();
            });
        }

        if (chipFilterProfile != null) {
            chipFilterProfile.setOnClickListener(v -> {
                currentFilter = FilterType.PROFILE;
                applyFilterAndUpdateList();
            });
        }

        if (chipFilterOther != null) {
            chipFilterOther.setOnClickListener(v -> {
                currentFilter = FilterType.OTHER;
                applyFilterAndUpdateList();
            });
        }
    }

    private void applyFilterAndUpdateList() {
        if (!isAdded()) return;

        List<UserHistoryEntry> source =
                fullHistory.isEmpty() ? Collections.emptyList() : new ArrayList<>(fullHistory);

        List<UserHistoryEntry> filtered = new ArrayList<>();

        for (UserHistoryEntry entry : source) {
            if (matchesFilter(entry, currentFilter)) {
                filtered.add(entry);
            }
        }

        adapter.submitList(filtered);

        boolean isEmpty = filtered.isEmpty();
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        textEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    private boolean matchesFilter(@NonNull UserHistoryEntry entry, @NonNull FilterType filter) {
        if (filter == FilterType.ALL) {
            return true;
        }

        String type = entry.getEventType();
        if (type == null) {
            // Unknown -> show only in OTHER
            return filter == FilterType.OTHER;
        }

        type = type.trim().toUpperCase();

        switch (filter) {
            case APPOINTMENTS:
                return isAppointmentEvent(type);
            case MEDICATIONS:
                return isMedicationEvent(type);
            case INDICATORS:
                return isIndicatorEvent(type);
            case PROFILE:
                return isProfileEvent(type);
            case OTHER:
                // Anything not in the known categories
                return !isAppointmentEvent(type)
                        && !isMedicationEvent(type)
                        && !isIndicatorEvent(type)
                        && !isProfileEvent(type);
            default:
                return true;
        }
    }

    // ---------------------------------------------------------------------
    // Observers
    // ---------------------------------------------------------------------

    private void observeViewModel() {
        viewModel.getHistoryEntries().observe(getViewLifecycleOwner(), this::applyHistoryItems);

        viewModel.getLoading().observe(getViewLifecycleOwner(), isLoading -> {
            boolean loading = Boolean.TRUE.equals(isLoading);
            showLoading(loading);
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), msg -> {
            if (!TextUtils.isEmpty(msg) && isAdded()) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
                viewModel.clearError();
            }
        });
    }

    private void applyHistoryItems(@Nullable List<UserHistoryEntry> items) {
        if (!isAdded()) return;

        fullHistory.clear();
        if (items != null) {
            fullHistory.addAll(items);
        }

        applyFilterAndUpdateList();
    }

    private void showLoading(boolean loading) {
        if (!isAdded()) return;

        // SwipeRefreshLayout has its own spinner for user-triggered refresh
        swipeRefreshLayout.setRefreshing(false);

        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    // ---------------------------------------------------------------------
    // Click handling
    // ---------------------------------------------------------------------

    private void onHistoryItemClicked(@NonNull UserHistoryEntry entry) {
        String eventType = entry.getEventType();
        if (eventType == null) {
            // No event type → try details bottom sheet or simple toast.
            maybeShowDetailsOrToast(entry);
            return;
        }

        eventType = eventType.trim().toUpperCase();

        if (isAppointmentEvent(eventType)) {
            openAppointmentsForCurrentRole();
        } else if (isMedicationEvent(eventType)) {
            openMedicationsScreen();
        } else if (isIndicatorEvent(eventType)) {
            openIndicatorsScreen();
        } else if (isProfileEvent(eventType)) {
            openProfileScreen();
        } else {
            // For LOGIN / LOGOUT / PATIENT_ACCOUNT_CREATED / etc.
            maybeShowDetailsOrToast(entry);
        }
    }

    private boolean isAppointmentEvent(@NonNull String type) {
        switch (type) {
            case "APPOINTMENT_CREATED":
            case "APPOINTMENT_CANCELLED":
            case "APPOINTMENT_RESCHEDULED":
            case "APPOINTMENT_ACCEPTED":
            case "APPOINTMENT_REJECTED":
                return true;
            default:
                return false;
        }
    }

    private boolean isMedicationEvent(@NonNull String type) {
        switch (type) {
            case "PRESCRIPTION_CREATED":
            case "PRESCRIPTION_DELETED":
            case "MEDICATION_REMINDER_UPDATED":
                return true;
            default:
                return false;
        }
    }

    private boolean isIndicatorEvent(@NonNull String type) {
        switch (type) {
            case "INDICATOR_ADDED":
            case "INDICATOR_DELETED":
                return true;
            default:
                return false;
        }
    }

    private boolean isProfileEvent(@NonNull String type) {
        switch (type) {
            case "PROFILE_UPDATED":
            case "PASSWORD_CHANGED":
                return true;
            default:
                return false;
        }
    }

    private void openAppointmentsForCurrentRole() {
        Activity activity = getActivity();
        if (activity instanceof MainActivity) {
            ((MainActivity) activity).openAppointmentsForCurrentRole();
            return;
        }

        // Fallback: open patient appointments (most common for history)
        NavHostFragment.findNavController(this)
                .navigate(R.id.patientAppointmentsFragment);
    }

    private void openMedicationsScreen() {
        // There is currently only a patient medications screen
        NavHostFragment.findNavController(this)
                .navigate(R.id.patientMedicationsFragment);
    }

    private void openIndicatorsScreen() {
        NavHostFragment.findNavController(this)
                .navigate(R.id.patientIndicatorsFragment);
    }

    private void openProfileScreen() {
        NavHostFragment.findNavController(this)
                .navigate(R.id.profileFragment);
    }

    private void showSimpleToast(@NonNull UserHistoryEntry entry) {
        if (!isAdded()) return;
        String msg = entry.getSafeMessage();
        if (TextUtils.isEmpty(msg)) {
            msg = getString(R.string.history_default_click_message);
        }
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
    }

    /**
     * If we have detailsJson, show the fancy bottom sheet; otherwise fall back to toast.
     */
    private void maybeShowDetailsOrToast(@NonNull UserHistoryEntry entry) {
        String details = entry.getDetailsJson();
        if (!TextUtils.isEmpty(details)) {
            UserHistoryDetailBottomSheet sheet =
                    UserHistoryDetailBottomSheet.newInstance(entry);
            sheet.show(getParentFragmentManager(), "history_detail");
        } else {
            showSimpleToast(entry);
        }
    }
}
