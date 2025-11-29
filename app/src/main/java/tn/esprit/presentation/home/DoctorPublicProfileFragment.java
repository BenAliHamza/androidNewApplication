package tn.esprit.presentation.home;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;

import java.math.BigDecimal;
import java.util.List;

import tn.esprit.R;
import tn.esprit.data.doctor.DoctorDirectoryRepository;
import tn.esprit.domain.doctor.DoctorPublicProfile;

/**
 * Public doctor profile screen, opened from patient home search.
 *
 * Shows:
 *  - avatar, name, specialty, city/country
 *  - flags (accepting new patients, teleconsultation)
 *  - fee, bio, clinic info
 *  - list of acts
 *  - CTAs (Book)
 */
public class DoctorPublicProfileFragment extends Fragment {

    private static final String ARG_DOCTOR_ID = "doctorId";

    private DoctorDirectoryRepository doctorDirectoryRepository;

    @Nullable
    private DoctorPublicProfile currentProfile;

    private ProgressBar progressBar;
    private View errorContainer;
    private TextView errorText;
    private View contentContainer;

    private ImageView imageAvatar;
    private TextView textName;
    private TextView textSpecialty;
    private TextView textLocation;
    private Chip chipAcceptingNew;
    private Chip chipTeleconsultation;
    private TextView textConsultationFee;

    private TextView textBio;
    private TextView textExperience;

    private TextView textClinicAddress;
    private TextView textClinicCityCountry;

    private RecyclerView recyclerActs;
    private TextView textActsEmpty;

    private Button buttonBook;

    private DoctorActAdapter actsAdapter;

    public DoctorPublicProfileFragment() {
        // Required empty constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_doctor_public_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        doctorDirectoryRepository = new DoctorDirectoryRepository(requireContext());

        progressBar = view.findViewById(R.id.doctor_public_progress);
        errorContainer = view.findViewById(R.id.doctor_public_error);
        errorText = view.findViewById(R.id.doctor_public_error_message);
        contentContainer = view.findViewById(R.id.doctor_public_content);

        imageAvatar = view.findViewById(R.id.image_doctor_avatar);
        textName = view.findViewById(R.id.text_doctor_name);
        textSpecialty = view.findViewById(R.id.text_doctor_specialty);
        textLocation = view.findViewById(R.id.text_doctor_location);
        chipAcceptingNew = view.findViewById(R.id.chip_accepting_new);
        chipTeleconsultation = view.findViewById(R.id.chip_teleconsultation);
        textConsultationFee = view.findViewById(R.id.text_consultation_fee);

        textBio = view.findViewById(R.id.text_bio);
        textExperience = view.findViewById(R.id.text_experience);

        textClinicAddress = view.findViewById(R.id.text_clinic_address);
        textClinicCityCountry = view.findViewById(R.id.text_clinic_city_country);

        recyclerActs = view.findViewById(R.id.recycler_acts);
        textActsEmpty = view.findViewById(R.id.text_acts_empty);

        buttonBook = view.findViewById(R.id.button_book);

        actsAdapter = new DoctorActAdapter();
        if (recyclerActs != null) {
            recyclerActs.setLayoutManager(new LinearLayoutManager(requireContext()));
            recyclerActs.setAdapter(actsAdapter);
        }

        if (buttonBook != null) {
            buttonBook.setOnClickListener(v -> handleBookClick());
        }

        long doctorId = -1L;
        Bundle args = getArguments();
        if (args != null) {
            doctorId = args.getLong(ARG_DOCTOR_ID, -1L);
        }

        if (doctorId <= 0L) {
            showLoading(false);
            showContent(false);
            showError(getString(R.string.doctor_public_error_missing_id), false);
            return;
        }

        loadDoctorProfile(doctorId);
    }

    private void loadDoctorProfile(long doctorId) {
        showLoading(true);
        showError(null, false);
        showContent(false);
        currentProfile = null;

        doctorDirectoryRepository.getDoctorPublicProfile(doctorId,
                new DoctorDirectoryRepository.PublicProfileCallback() {
                    @Override
                    public void onSuccess(DoctorPublicProfile profile) {
                        if (!isAdded()) return;
                        currentProfile = profile;
                        bindProfile(profile);
                    }

                    @Override
                    public void onError(@Nullable Throwable throwable,
                                        @Nullable Integer httpCode,
                                        @Nullable String errorBody) {
                        if (!isAdded()) return;

                        currentProfile = null;
                        showLoading(false);
                        showContent(false);

                        String msg = getString(R.string.doctor_public_error_generic);
                        if (httpCode != null) {
                            msg = msg + " (" + httpCode + ")";
                        }
                        if (errorBody != null && !errorBody.isEmpty()) {
                            msg = msg + " " + errorBody;
                        }

                        showError(msg, true);
                    }
                });
    }

    private void bindProfile(@NonNull DoctorPublicProfile profile) {
        showLoading(false);
        showError(null, false);
        showContent(true);

        if (contentContainer != null) {
            contentContainer.startAnimation(
                    AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in_short)
            );
        }

        if (!TextUtils.isEmpty(profile.getProfileImageUrl())) {
            Glide.with(this)
                    .load(profile.getProfileImageUrl())
                    .placeholder(R.drawable.logo)
                    .error(R.drawable.logo)
                    .circleCrop()
                    .into(imageAvatar);
        } else {
            imageAvatar.setImageResource(R.drawable.logo);
        }

        String fullName = profile.getFullName();
        if (TextUtils.isEmpty(fullName)) {
            fullName = getString(R.string.profile_role_doctor);
        }
        textName.setText(fullName);

        if (!TextUtils.isEmpty(profile.getSpecialtyName())) {
            textSpecialty.setVisibility(View.VISIBLE);
            textSpecialty.setText(profile.getSpecialtyName());
        } else {
            textSpecialty.setVisibility(View.GONE);
        }

        String city = profile.getCity() != null ? profile.getCity().trim() : "";
        String country = profile.getCountry() != null ? profile.getCountry().trim() : "";
        StringBuilder locationBuilder = new StringBuilder();
        if (!city.isEmpty()) {
            locationBuilder.append(city);
        }
        if (!country.isEmpty()) {
            if (locationBuilder.length() > 0) locationBuilder.append(", ");
            locationBuilder.append(country);
        }
        if (locationBuilder.length() > 0) {
            textLocation.setVisibility(View.VISIBLE);
            textLocation.setText(locationBuilder.toString());
        } else {
            textLocation.setVisibility(View.GONE);
        }

        Boolean acceptingNew = profile.getAcceptingNewPatients();
        if (chipAcceptingNew != null) {
            if (acceptingNew == null) {
                chipAcceptingNew.setVisibility(View.GONE);
            } else {
                chipAcceptingNew.setVisibility(View.VISIBLE);
                if (acceptingNew) {
                    chipAcceptingNew.setText(R.string.doctor_public_flag_accepts_new);
                } else {
                    chipAcceptingNew.setText(R.string.doctor_public_flag_not_accepting);
                }
            }
        }

        Boolean tele = profile.getTeleconsultationEnabled();
        if (chipTeleconsultation != null) {
            if (tele == null) {
                chipTeleconsultation.setVisibility(View.GONE);
            } else {
                chipTeleconsultation.setVisibility(View.VISIBLE);
                if (Boolean.TRUE.equals(tele)) {
                    chipTeleconsultation.setText(R.string.doctor_public_flag_teleconsultation);
                } else {
                    chipTeleconsultation.setText(R.string.doctor_public_flag_no_teleconsultation);
                }
            }
        }

        String feeFormatted = formatFee(profile.getConsultationFee());
        if (feeFormatted != null) {
            textConsultationFee.setVisibility(View.VISIBLE);
            textConsultationFee.setText(feeFormatted);
        } else {
            textConsultationFee.setVisibility(View.GONE);
        }

        String bio = profile.getBio();
        if (TextUtils.isEmpty(bio)) {
            textBio.setText(R.string.doctor_public_bio_placeholder);
        } else {
            textBio.setText(bio.trim());
        }

        Integer years = profile.getYearsOfExperience();
        if (years != null && years > 0) {
            textExperience.setVisibility(View.VISIBLE);
            textExperience.setText(
                    getString(R.string.profile_doctor_experience_format, years)
            );
        } else {
            textExperience.setVisibility(View.GONE);
        }

        String clinicAddress = profile.getClinicAddress();
        if (TextUtils.isEmpty(clinicAddress)) {
            textClinicAddress.setText(R.string.doctor_public_clinic_address_placeholder);
        } else {
            textClinicAddress.setText(clinicAddress.trim());
        }

        if (locationBuilder.length() > 0) {
            textClinicCityCountry.setVisibility(View.VISIBLE);
            textClinicCityCountry.setText(locationBuilder.toString());
        } else {
            textClinicCityCountry.setVisibility(View.GONE);
        }

        List<DoctorPublicProfile.Act> acts = profile.getActs();
        if (acts == null || acts.isEmpty()) {
            textActsEmpty.setVisibility(View.VISIBLE);
            recyclerActs.setVisibility(View.GONE);
            actsAdapter.submitList(null);
        } else {
            textActsEmpty.setVisibility(View.GONE);
            recyclerActs.setVisibility(View.VISIBLE);
            actsAdapter.submitList(acts);
            recyclerActs.startAnimation(
                    AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in)
            );
        }

        if (buttonBook != null) {
            boolean canBook = acceptingNew == null || Boolean.TRUE.equals(acceptingNew);
            buttonBook.setEnabled(canBook);
            buttonBook.setAlpha(canBook ? 1f : 0.5f);
        }
    }

    @Nullable
    private String formatFee(@Nullable BigDecimal fee) {
        if (fee == null) {
            return null;
        }
        BigDecimal normalized = fee.stripTrailingZeros();
        String plain = normalized.toPlainString();
        return getString(R.string.profile_doctor_fee_format, plain);
    }

    private void showLoading(boolean loading) {
        if (progressBar != null) {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
    }

    private void showError(@Nullable String message, boolean show) {
        if (errorContainer != null) {
            errorContainer.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (errorText != null) {
            if (message == null || message.trim().isEmpty()) {
                errorText.setText(R.string.doctor_public_error_generic);
            } else {
                errorText.setText(message);
            }
        }
    }

    private void showContent(boolean show) {
        if (contentContainer != null) {
            contentContainer.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void handleBookClick() {
        if (!isAdded()) return;

        if (currentProfile == null) {
            Toast.makeText(
                    requireContext(),
                    getString(R.string.doctor_public_error_generic),
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        Boolean acceptingNew = currentProfile.getAcceptingNewPatients();
        if (acceptingNew != null && !acceptingNew) {
            Toast.makeText(
                    requireContext(),
                    getString(R.string.doctor_public_flag_not_accepting),
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        Long doctorId = currentProfile.getDoctorId();
        if (doctorId == null || doctorId <= 0L) {
            Toast.makeText(
                    requireContext(),
                    getString(R.string.doctor_public_error_generic),
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        String name = currentProfile.getFullName();
        if (TextUtils.isEmpty(name)) {
            name = getString(R.string.doctor_public_title);
        }
        String specialty = currentProfile.getSpecialtyName();
        boolean teleEnabled = Boolean.TRUE.equals(currentProfile.getTeleconsultationEnabled());

        BookAppointmentBottomSheet.show(
                getParentFragmentManager(),
                doctorId,
                name,
                specialty,
                teleEnabled
        );
    }
}
