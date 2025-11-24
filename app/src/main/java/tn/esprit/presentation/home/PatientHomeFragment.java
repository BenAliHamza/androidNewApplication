package tn.esprit.presentation.home;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

import tn.esprit.R;
import tn.esprit.domain.doctor.DoctorSearchResult;

public class PatientHomeFragment extends Fragment implements DoctorSearchResultAdapter.OnDoctorClickListener {

    // Faster typing response
    private static final long SEARCH_DEBOUNCE_MS = 200L;

    private PatientHomeViewModel viewModel;

    private TextInputEditText inputSearch;
    private RecyclerView recyclerResults;
    private ProgressBar progressBar;
    private TextView textEmpty;
    private Button buttonMyIndicators;

    private DoctorSearchResultAdapter adapter;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pendingSearchRunnable;

    public PatientHomeFragment() {
        // Required empty constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home_patient, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        inputSearch = view.findViewById(R.id.input_search_doctors);
        recyclerResults = view.findViewById(R.id.recycler_doctor_results);
        progressBar = view.findViewById(R.id.patient_home_progress);
        textEmpty = view.findViewById(R.id.text_patient_home_empty);
        buttonMyIndicators = view.findViewById(R.id.button_my_indicators);

        adapter = new DoctorSearchResultAdapter(this);
        if (recyclerResults != null) {
            recyclerResults.setLayoutManager(new LinearLayoutManager(requireContext()));
            recyclerResults.setAdapter(adapter);
        }

        viewModel = new ViewModelProvider(this).get(PatientHomeViewModel.class);

        setupObservers();
        setupSearchInput();
        setupMyIndicatorsButton();
    }

    private void setupObservers() {
        viewModel.getResults().observe(getViewLifecycleOwner(), this::bindResults);
        viewModel.getLoading().observe(getViewLifecycleOwner(), this::bindLoading);
        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), this::bindError);
        viewModel.getHasSearched().observe(getViewLifecycleOwner(), hasSearched -> {
            // Update empty state when hasSearched toggles
            List<DoctorSearchResult> current = viewModel.getResults().getValue();
            bindEmptyState(current, hasSearched != null && hasSearched);
        });
    }

    private void setupSearchInput() {
        if (inputSearch == null) return;

        inputSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s,
                                          int start,
                                          int count,
                                          int after) {
                // no-op
            }

            @Override
            public void onTextChanged(CharSequence s,
                                      int start,
                                      int before,
                                      int count) {
                // Debounce typing
                if (pendingSearchRunnable != null) {
                    handler.removeCallbacks(pendingSearchRunnable);
                }

                final String query = s != null ? s.toString() : "";
                pendingSearchRunnable = () -> viewModel.searchDoctors(query);
                handler.postDelayed(pendingSearchRunnable, SEARCH_DEBOUNCE_MS);
            }

            @Override
            public void afterTextChanged(Editable s) {
                // no-op
            }
        });
    }

    private void setupMyIndicatorsButton() {
        if (buttonMyIndicators == null) return;

        buttonMyIndicators.setOnClickListener(v -> {
            if (!isAdded()) return;
            NavHostFragment.findNavController(PatientHomeFragment.this)
                    .navigate(R.id.patientIndicatorsFragment);
        });
    }

    private void bindResults(@Nullable List<DoctorSearchResult> list) {
        if (list == null) {
            adapter.submitList(null);
            bindEmptyState(null, Boolean.TRUE.equals(viewModel.getHasSearched().getValue()));
            return;
        }
        adapter.submitList(list);
        bindEmptyState(list, Boolean.TRUE.equals(viewModel.getHasSearched().getValue()));
    }

    private void bindLoading(Boolean isLoading) {
        if (progressBar != null) {
            progressBar.setVisibility(Boolean.TRUE.equals(isLoading) ? View.VISIBLE : View.GONE);
        }
    }

    private void bindError(@Nullable String error) {
        if (error == null || error.trim().isEmpty()) {
            return;
        }
        if (!isAdded()) return;
        Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
    }

    private void bindEmptyState(@Nullable List<DoctorSearchResult> list,
                                boolean hasSearched) {
        if (textEmpty == null) return;

        String currentQuery = viewModel.getQuery().getValue();
        String trimmed = currentQuery != null ? currentQuery.trim() : "";

        boolean hasResults = list != null && !list.isEmpty();

        if (trimmed.length() < 3) {
            // Idle state: encourage user to start typing
            textEmpty.setText(R.string.home_patient_search_empty_idle);
            textEmpty.setVisibility(View.VISIBLE);
        } else if (!hasResults && hasSearched) {
            // User searched and got no results
            textEmpty.setText(R.string.home_patient_search_empty_results);
            textEmpty.setVisibility(View.VISIBLE);
        } else {
            textEmpty.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDoctorClicked(@NonNull DoctorSearchResult doctor) {
        if (!isAdded()) return;

        // Navigation to a dedicated DoctorPublicProfileFragment is wired in nav_main,
        // but we still keep this toast as a fallback / debug confirmation.
        String name = doctor.getLastName();
        if (name == null || name.isEmpty()) {
            name = getString(R.string.profile_role_doctor);
        }
        Toast.makeText(
                requireContext(),
                getString(R.string.home_patient_doctor_click_toast, name),
                Toast.LENGTH_SHORT
        ).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (pendingSearchRunnable != null) {
            handler.removeCallbacks(pendingSearchRunnable);
        }
    }
}
