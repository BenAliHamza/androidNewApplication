package tn.esprit.presentation.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import tn.esprit.R;

/**
 * Doctor "Office" screen:
 *  - Shows weekly schedule
 *  - Shows a spinner while loading from backend
 *  - Allows navigation to edit schedule screen
 */
public class DoctorOfficeFragment extends Fragment {

    private DoctorScheduleViewModel viewModel;

    private ProgressBar progressBar;
    private View contentContainer;
    private TextView emptyText;

    private TextView mondaySummary;
    private TextView tuesdaySummary;
    private TextView wednesdaySummary;
    private TextView thursdaySummary;
    private TextView fridaySummary;
    private TextView saturdaySummary;
    private TextView sundaySummary;

    private Button buttonEditSchedule;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_doctor_office, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        progressBar = view.findViewById(R.id.doctor_schedule_progress);
        contentContainer = view.findViewById(R.id.doctor_schedule_content);
        emptyText = view.findViewById(R.id.text_schedule_empty);

        mondaySummary = view.findViewById(R.id.text_monday_summary);
        tuesdaySummary = view.findViewById(R.id.text_tuesday_summary);
        wednesdaySummary = view.findViewById(R.id.text_wednesday_summary);
        thursdaySummary = view.findViewById(R.id.text_thursday_summary);
        fridaySummary = view.findViewById(R.id.text_friday_summary);
        saturdaySummary = view.findViewById(R.id.text_saturday_summary);
        sundaySummary = view.findViewById(R.id.text_sunday_summary);

        buttonEditSchedule = view.findViewById(R.id.button_edit_schedule);

        viewModel = new ViewModelProvider(requireActivity())
                .get(DoctorScheduleViewModel.class);

        observeViewModel();
        setupClickListeners();
    }

    private void setupClickListeners() {
        if (buttonEditSchedule != null) {
            buttonEditSchedule.setOnClickListener(v -> {
                NavController navController = NavHostFragment.findNavController(this);
                // Uses the doctorScheduleEditFragment destination we already added in nav_main.xml
                navController.navigate(R.id.doctorScheduleEditFragment);
            });
        }
    }

    private void observeViewModel() {
        viewModel.getUiState().observe(getViewLifecycleOwner(), state -> {
            if (state == null) return;

            if (state.isLoading()) {
                if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
                if (contentContainer != null) contentContainer.setVisibility(View.GONE);
            } else {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                if (contentContainer != null) contentContainer.setVisibility(View.VISIBLE);
            }

            if (emptyText != null) {
                emptyText.setVisibility(state.hasSchedule() ? View.GONE : View.VISIBLE);
            }

            updateDaySummary(mondaySummary, state, "MONDAY");
            updateDaySummary(tuesdaySummary, state, "TUESDAY");
            updateDaySummary(wednesdaySummary, state, "WEDNESDAY");
            updateDaySummary(thursdaySummary, state, "THURSDAY");
            updateDaySummary(fridaySummary, state, "FRIDAY");
            updateDaySummary(saturdaySummary, state, "SATURDAY");
            updateDaySummary(sundaySummary, state, "SUNDAY");
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg == null || msg.isEmpty()) return;
            // you can show a Toast here if you want
        });
    }

    private void updateDaySummary(@Nullable TextView tv,
                                  @NonNull DoctorScheduleUiState state,
                                  @NonNull String dayCode) {
        if (tv == null) return;

        DoctorScheduleUiState.DayScheduleSummary match = null;
        for (DoctorScheduleUiState.DayScheduleSummary d : state.getDays()) {
            if (dayCode.equalsIgnoreCase(d.getDayCode())) {
                match = d;
                break;
            }
        }

        if (match != null) {
            // using getSummaryText() according to your change
            tv.setText(match.getSummaryText());
        } else {
            tv.setText(getString(R.string.doctor_office_day_off));
        }
    }
}
