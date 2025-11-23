package tn.esprit.presentation.home;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;

import java.util.List;

import tn.esprit.R;
import tn.esprit.data.doctor.DoctorDirectoryRepository;
import tn.esprit.domain.doctor.DoctorPublicProfile;

public class DoctorPublicProfileFragment extends Fragment {

    public static final String ARG_DOCTOR_ID = "arg_doctor_id";

    private ProgressBar progressBar;
    private View contentRoot;
    private TextView textError;

    private ImageView imageAvatar;
    private TextView textName;
    private TextView textSpecialty;
    private TextView textLocation;

    private Chip chipAcceptingNew;
    private Chip chipTeleconsultation;

    private TextView textBio;
    private TextView textExperience;
    private TextView textFee;

    private TextView textClinicAddress;
    private TextView textClinicLocation;

    private RecyclerView recyclerActs;
    private TextView textActsEmpty;

    private DoctorDirectoryRepository doctorDirectoryRepository;
    private DoctorActAdapter actAdapter;

    private long doctorId = -1L;

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

        progressBar = view.findViewById(R.id.doctor_public_progress);
        contentRoot = view.findViewById(R.id.doctor_public_content_root);
        textError = view.findViewById(R.id.doctor_public_error);

        imageAvatar = view.findViewById(R.id.doctor_public_avatar);
        textName = view.findViewById(R.id.doctor_public_name);
        textSpecialty = view.findViewById(R.id.doctor_public_specialty);
        textLocation = view.findViewById(R.id.doctor_public_location);

        chipAcceptingNew = view.findViewById(R.id.chip_doctor_accepting_new);
        chipTeleconsultation = view.findViewById(R.id.chip_doctor_teleconsultation);

        textBio = view.findViewById(R.id.doctor_public_bio);
        textExperience = view.findViewById(R.id.doctor_public_experience);
        textFee = view.findViewById(R.id.doctor_public_fee);

        textClinicAddress = view.findViewById(R.id.doctor_public_clinic_address);
        textClinicLocation = view.findViewById(R.id.doctor_public_clinic_location);

        recyclerActs = view.findViewById(R.id.recycler_doctor_acts);
        textActsEmpty = view.findViewById(R.id.text_doctor_acts_empty);

        actAdapter = new DoctorActAdapter();
        if (recyclerActs != null) {
            recyclerActs.setLayoutManager(new LinearLayoutManager(requireContext()));
            recyclerActs.setAdapter(actAdapter);
        }

        doctorDirectoryRepository = new DoctorDirectoryRepository(requireContext().getApplicationContext());

        // Read doctorId from arguments
        Bundle args = getArguments();
        if (args != null) {
            doctorId = args.getLong(ARG_DOCTOR_ID, -1L);
        }

        if (doctorId <= 0L) {
            showError(getString(R.string.doctor_public_error_missing_id), false);
        } else {
            loadDoctorPublicProfile();
        }
    }

    private void loadDoctorPublicProfile() {
        showLoading();

        doctorDirectoryRepository.getDoctorPublicProfile(doctorId, new DoctorDirectoryRepository.PublicProfileCallback() {
            @Override
            public void onSuccess(DoctorPublicProfile profile) {
                if (!isAdded()) return;
                if (profile == null) {
                    showError(getString(R.string.doctor_public_error_generic), true);
                    return;
                }
                bindProfile(profile);
            }

            @Override
            public void onError(@Nullable Throwable throwable,
                                @Nullable Integer httpCode,
                                @Nullable String errorBody) {
                if (!isAdded()) return;

                String msg = getString(R.string.doctor_public_error_generic);
                showError(msg, true);
            }
        });
    }

    private void showLoading() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        if (contentRoot != null) contentRoot.setVisibility(View.INVISIBLE);
        if (textError != null) {
            textError.setVisibility(View.GONE);
            textError.setOnClickListener(null);
        }
    }

    private void showError(String message, boolean retryable) {
        if (progressBar != null) progressBar.setVisibility(View.GONE);
        if (contentRoot != null) contentRoot.setVisibility(View.GONE);
        if (textError != null) {
            textError.setText(message);
            textError.setVisibility(View.VISIBLE);
            if (retryable) {
                textError.setOnClickListener(v -> loadDoctorPublicProfile());
            } else {
                textError.setOnClickListener(null);
            }
        }
    }

    private void showContent() {
        if (progressBar != null) progressBar.setVisibility(View.GONE);
        if (contentRoot != null) contentRoot.setVisibility(View.VISIBLE);
        if (textError != null) {
            textError.setVisibility(View.GONE);
            textError.setOnClickListener(null);
        }
    }

    private void bindProfile(@NonNull DoctorPublicProfile profile) {
        showContent();

        // Avatar
        String imageUrl = profile.getProfileImageUrl();
        if (!TextUtils.isEmpty(imageUrl)) {
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.logo)
                    .error(R.drawable.logo)
                    .circleCrop()
                    .into(imageAvatar);
        } else {
            imageAvatar.setImageResource(R.drawable.logo);
        }

        // Header: name, specialty, location
        String fullName = profile.getFullName();
        if (fullName == null) fullName = "";
        textName.setText(fullName);

        String specialty = profile.getSpecialtyName();
        if (specialty == null) specialty = "";
        textSpecialty.setText(specialty);

        StringBuilder locationBuilder = new StringBuilder();
        if (!TextUtils.isEmpty(profile.getCity())) {
            locationBuilder.append(profile.getCity().trim());
        }
        if (!TextUtils.isEmpty(profile.getCountry())) {
            if (locationBuilder.length() > 0) locationBuilder.append(", ");
            locationBuilder.append(profile.getCountry().trim());
        }
        textLocation.setText(locationBuilder.toString());

        // Flags
        Boolean acceptsNew = profile.getAcceptingNewPatients();
        Boolean tele = profile.getTeleconsultationEnabled();

        if (chipAcceptingNew != null) {
            if (Boolean.TRUE.equals(acceptsNew)) {
                chipAcceptingNew.setVisibility(View.VISIBLE);
                chipAcceptingNew.setText(R.string.profile_flag_accepts_new);
            } else if (Boolean.FALSE.equals(acceptsNew)) {
                chipAcceptingNew.setVisibility(View.VISIBLE);
                chipAcceptingNew.setText(R.string.profile_flag_not_accepts_new);
            } else {
                chipAcceptingNew.setVisibility(View.GONE);
            }
        }

        if (chipTeleconsultation != null) {
            if (Boolean.TRUE.equals(tele)) {
                chipTeleconsultation.setVisibility(View.VISIBLE);
                chipTeleconsultation.setText(R.string.profile_flag_teleconsultation);
            } else if (Boolean.FALSE.equals(tele)) {
                chipTeleconsultation.setVisibility(View.VISIBLE);
                chipTeleconsultation.setText(R.string.profile_flag_no_teleconsultation);
            } else {
                chipTeleconsultation.setVisibility(View.GONE);
            }
        }

        // About section
        String bio = profile.getBio();
        if (TextUtils.isEmpty(bio)) {
            textBio.setText(R.string.doctor_public_bio_placeholder);
        } else {
            textBio.setText(bio.trim());
        }

        Integer years = profile.getYearsOfExperience();
        if (years != null && years > 0) {
            String expText = getString(R.string.profile_doctor_experience_format, years);
            textExperience.setText(expText);
            textExperience.setVisibility(View.VISIBLE);
        } else {
            textExperience.setVisibility(View.GONE);
        }

        if (profile.getConsultationFee() != null) {
            String fee = profile.getConsultationFee().toPlainString();
            String feeText = getString(R.string.profile_doctor_fee_format, fee);
            textFee.setText(feeText);
            textFee.setVisibility(View.VISIBLE);
        } else {
            textFee.setVisibility(View.GONE);
        }

        // Clinic section
        String address = profile.getClinicAddress();
        if (!TextUtils.isEmpty(address)) {
            textClinicAddress.setText(address.trim());
        } else {
            textClinicAddress.setText(R.string.doctor_public_clinic_address_placeholder);
        }
        textClinicLocation.setText(locationBuilder.toString());

        // Acts
        List<DoctorPublicProfile.Act> acts = profile.getActs();
        if (acts == null || acts.isEmpty()) {
            actAdapter.submitList(null);
            textActsEmpty.setVisibility(View.VISIBLE);
        } else {
            actAdapter.submitList(acts);
            textActsEmpty.setVisibility(View.GONE);
        }
    }
}
