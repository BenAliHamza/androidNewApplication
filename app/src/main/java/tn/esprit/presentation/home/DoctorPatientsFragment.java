package tn.esprit.presentation.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import tn.esprit.R;
import tn.esprit.data.doctor.DoctorPatientsRepository;
import tn.esprit.domain.patient.PatientProfile;

/**
 * Fragment showing the current doctor's patients.
 *
 * Behavior:
 *  - Loads /api/doctors/me/patients via DoctorPatientsRepository.
 *  - Shows a centered progress bar while loading.
 *  - Shows an empty message when there are no patients.
 *  - On item click, navigates to PatientPublicProfileFragment with the patient's User.id.
 */
public class DoctorPatientsFragment extends Fragment {

    private ProgressBar progressBar;
    private TextView emptyView;
    private RecyclerView recyclerView;

    private DoctorPatientsAdapter adapter;
    private DoctorPatientsRepository repository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_doctor_patients, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        progressBar = view.findViewById(R.id.doctor_patients_progress);
        emptyView = view.findViewById(R.id.text_doctor_patients_empty);
        recyclerView = view.findViewById(R.id.recycler_doctor_patients);

        repository = new DoctorPatientsRepository(requireContext());

        adapter = new DoctorPatientsAdapter(this::onPatientClicked);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        loadPatients();
    }

    private void loadPatients() {
        showLoading(true);
        showEmpty(false);

        repository.getMyPatients(new DoctorPatientsRepository.LoadPatientsCallback() {
            @Override
            public void onSuccess(List<PatientProfile> patients) {
                if (!isAdded()) return;

                showLoading(false);

                if (patients == null || patients.isEmpty()) {
                    adapter.setPatients(patients);
                    showEmpty(true);
                } else {
                    adapter.setPatients(patients);
                    showEmpty(false);
                }
            }

            @Override
            public void onError(@Nullable Throwable throwable,
                                @Nullable Integer httpCode,
                                @Nullable String errorBody) {
                if (!isAdded()) return;

                showLoading(false);
                // Show "empty" message with error text if you want; for now reuse empty view
                emptyView.setText(R.string.doctor_patients_error_generic);
                showEmpty(true);
            }
        });
    }

    private void onPatientClicked(@NonNull PatientProfile patient) {
        Long patientUserId = patient.getUserId();
        if (patientUserId == null) {
            // No id means we can't navigate anywhere meaningful.
            return;
        }

        Bundle args = new Bundle();
        // Nav graph argument name: "patientId" (represents patient User.id).
        args.putLong("patientId", patientUserId);

        NavController navController = NavHostFragment.findNavController(this);
        navController.navigate(R.id.patientPublicProfileFragment, args);
    }

    private void showLoading(boolean loading) {
        if (progressBar != null) {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        if (recyclerView != null) {
            recyclerView.setVisibility(loading ? View.GONE : View.VISIBLE);
        }
    }

    private void showEmpty(boolean empty) {
        if (emptyView != null) {
            emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
        }
    }
}
