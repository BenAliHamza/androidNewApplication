package tn.esprit.presentation.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;

import tn.esprit.R;
import tn.esprit.data.profile.ProfileRepository;
import tn.esprit.domain.doctor.DoctorProfile;
import tn.esprit.domain.patient.PatientProfile;
import tn.esprit.domain.user.User;

/**
 * Role-aware Home container:
 *  - Shows loading while determining role.
 *  - For DOCTOR: inflates doctor home layout (highlight, stats, quick actions).
 *  - For PATIENT: hosts PatientHomeFragment (doctor search).
 */
public class HomeFragment extends Fragment {

    private View progressView;
    private View doctorContainer;
    private View patientContainer;

    private ProfileRepository profileRepository;

    private boolean doctorContentInflated = false;
    private boolean patientFragmentAttached = false;

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

        progressView = view.findViewById(R.id.home_progress);
        doctorContainer = view.findViewById(R.id.home_doctor_container);
        patientContainer = view.findViewById(R.id.home_patient_container);

        profileRepository = new ProfileRepository(requireContext().getApplicationContext());

        showLoadingState();
        loadRoleAndBind();
    }

    private void showLoadingState() {
        if (progressView != null) {
            progressView.setVisibility(View.VISIBLE);
        }
        if (doctorContainer != null) {
            doctorContainer.setVisibility(View.GONE);
        }
        if (patientContainer != null) {
            patientContainer.setVisibility(View.GONE);
        }
    }

    private void hideLoading() {
        if (progressView != null) {
            progressView.setVisibility(View.GONE);
        }
    }

    private void loadRoleAndBind() {
        profileRepository.loadProfile(new ProfileRepository.ProfileCallback() {
            @Override
            public void onSuccess(User user,
                                  DoctorProfile doctorProfile,
                                  PatientProfile patientProfile) {
                if (!isAdded()) return;

                String role = user != null ? user.getRole() : null;
                requireActivity().runOnUiThread(() -> showRoleSpecificHome(role));
            }

            @Override
            public void onError(Throwable throwable,
                                Integer httpCode,
                                String errorBody) {
                if (!isAdded()) return;
                // Fallback: unknown role -> show doctor-style home (previous behavior)
                requireActivity().runOnUiThread(() -> showRoleSpecificHome(null));
            }
        });
    }

    private void showRoleSpecificHome(@Nullable String role) {
        hideLoading();

        boolean isPatient = role != null && "PATIENT".equalsIgnoreCase(role);
        boolean isDoctor = role != null && "DOCTOR".equalsIgnoreCase(role);

        if (isPatient) {
            showPatientHome();
        } else if (isDoctor) {
            showDoctorHome();
        } else {
            // Unknown role: fallback to doctor layout as a generic home
            showDoctorHome();
        }
    }

    private void showDoctorHome() {
        if (doctorContainer == null) return;

        doctorContainer.setVisibility(View.VISIBLE);
        if (patientContainer != null) {
            patientContainer.setVisibility(View.GONE);
        }

        if (!doctorContentInflated && doctorContainer instanceof ViewGroup) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            inflater.inflate(R.layout.layout_home_doctor, (ViewGroup) doctorContainer, true);
            doctorContentInflated = true;

            // Optional subtle entrance animation on the highlight card
            View highlightCard = doctorContainer.findViewById(R.id.card_highlight);
            if (highlightCard != null) {
                highlightCard.setAlpha(0f);
                highlightCard.setTranslationY(24f);
                ViewCompat.animate(highlightCard)
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(400L)
                        .setStartDelay(80L)
                        .start();
            }
        }
    }

    private void showPatientHome() {
        if (patientContainer == null) return;

        patientContainer.setVisibility(View.VISIBLE);
        if (doctorContainer != null) {
            doctorContainer.setVisibility(View.GONE);
        }

        if (!patientFragmentAttached) {
            // Attach PatientHomeFragment as a child fragment into the container
            Fragment existing = getChildFragmentManager()
                    .findFragmentByTag("patient_home_fragment");
            if (existing == null) {
                getChildFragmentManager()
                        .beginTransaction()
                        .replace(R.id.home_patient_container,
                                new PatientHomeFragment(),
                                "patient_home_fragment")
                        .commit();
            }
            patientFragmentAttached = true;
        }
    }
}
