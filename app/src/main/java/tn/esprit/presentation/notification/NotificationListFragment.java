package tn.esprit.presentation.notification;

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

import tn.esprit.MainActivity;
import tn.esprit.R;
import tn.esprit.domain.notification.NotificationItem;

/**
 * Simple notifications list screen.
 *
 * Responsibilities:
 *  - Shows list / empty state / loading
 *  - Uses activity-scoped NotificationsViewModel
 *  - Clicking a notification marks it as read and navigates
 *    to the appropriate appointments screen based on role.
 */
public class NotificationListFragment extends Fragment {

    private NotificationsViewModel viewModel;

    private ProgressBar progressBar;
    private RecyclerView recyclerView;
    private TextView emptyText;

    private NotificationAdapter adapter;

    public NotificationListFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_notifications, container, false);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(view, savedInstanceState);

        progressBar = view.findViewById(R.id.notifications_progress);
        recyclerView = view.findViewById(R.id.recycler_notifications);
        emptyText = view.findViewById(R.id.text_notifications_empty);

        adapter = new NotificationAdapter(notification -> {
            if (viewModel != null) {
                viewModel.markAsRead(notification);
            }

            // After marking as read, open the right appointments screen
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openAppointmentsForCurrentRole();
            }
        });

        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
            recyclerView.setAdapter(adapter);
        }

        // Activity-scoped ViewModel so drawer badge + screen share state
        viewModel = new ViewModelProvider(requireActivity())
                .get(NotificationsViewModel.class);

        observeViewModel();

        // Initial load – safe to call, ViewModel will no-op if already loading
        viewModel.loadNotifications();
    }

    private void observeViewModel() {
        if (viewModel == null) return;

        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            if (loading == null) return;
            if (progressBar != null) {
                progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            }
        });

        viewModel.getNotifications().observe(getViewLifecycleOwner(), this::bindNotifications);

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg == null || msg.trim().isEmpty()) return;
            if (!isAdded()) return;
            Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
            viewModel.clearError();
        });
    }

    private void bindNotifications(@Nullable List<NotificationItem> list) {
        if (list == null || list.isEmpty()) {
            if (emptyText != null) {
                emptyText.setVisibility(View.VISIBLE);
            }
            if (recyclerView != null) {
                recyclerView.setVisibility(View.GONE);
            }
            adapter.submitList(null);
        } else {
            if (emptyText != null) {
                emptyText.setVisibility(View.GONE);
            }
            if (recyclerView != null) {
                recyclerView.setVisibility(View.VISIBLE);
            }
            adapter.submitList(list);
        }
    }
}
