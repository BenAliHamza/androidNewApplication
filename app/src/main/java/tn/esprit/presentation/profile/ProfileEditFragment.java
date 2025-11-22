package tn.esprit.presentation.profile;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

import java.math.BigDecimal;

import tn.esprit.R;
import tn.esprit.domain.doctor.DoctorProfile;

/**
 * Doctor profile edit screen.
 *
 * This fragment ONLY edits the doctor profile that comes from the backend
 * via ProfileViewModel (ProfileRepository + /me + /api/doctors/me).
 *
 * There is no fake data or hard-coded doctor here.
 *
 * NEW:
 *  - A "Base information" button at the bottom that navigates to
 *    UserBaseInfoFragment (which then leads to UserBaseInfoEditFragment).
 */
public class ProfileEditFragment extends Fragment {

    private ProfileViewModel viewModel;

    private ImageButton buttonBack;
    private TextInputEditText inputName;
    private TextInputEditText inputPhone;
    private TextInputEditText inputClinic;
    private TextInputEditText inputCity;
    private TextInputEditText inputCountry;
    private TextInputEditText inputRegNumber;
    private TextInputEditText inputFee;
    private TextInputEditText inputBio;
    private SwitchMaterial switchAcceptsNew;
    private SwitchMaterial switchTeleconsult;

    private Button buttonSave;
    private Button buttonCancel;
    private Button buttonBaseInfo;

    private View loadingOverlay;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_profile_edit, container, false);

        buttonBack = root.findViewById(R.id.button_back);
        inputName = root.findViewById(R.id.input_name);
        inputPhone = root.findViewById(R.id.input_phone);
        inputClinic = root.findViewById(R.id.input_clinic);
        inputCity = root.findViewById(R.id.input_city);
        inputCountry = root.findViewById(R.id.input_country);
        inputRegNumber = root.findViewById(R.id.input_reg_number);
        inputFee = root.findViewById(R.id.input_fee);
        inputBio = root.findViewById(R.id.input_bio);
        switchAcceptsNew = root.findViewById(R.id.switch_accepts_new);
        switchTeleconsult = root.findViewById(R.id.switch_teleconsult);

        buttonSave = root.findViewById(R.id.button_save);
        buttonCancel = root.findViewById(R.id.button_cancel);
        buttonBaseInfo = root.findViewById(R.id.button_base_info);

        loadingOverlay = root.findViewById(R.id.edit_loading_overlay);

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);

        // Bind existing doctor profile data
        viewModel.getDoctorProfile().observe(getViewLifecycleOwner(), this::bindProfileForEdit);

        // Loading overlay
        viewModel.getLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (loadingOverlay != null) {
                loadingOverlay.setVisibility(isLoading != null && isLoading ? View.VISIBLE : View.GONE);
            }
        });

        buttonBack.setOnClickListener(v -> requireActivity().onBackPressed());
        buttonCancel.setOnClickListener(v -> requireActivity().onBackPressed());
        buttonSave.setOnClickListener(v -> saveChanges());

        if (buttonBaseInfo != null) {
            buttonBaseInfo.setOnClickListener(v -> openBaseInfo());
        }
    }

    private void openBaseInfo() {
        try {
            NavHostFragment.findNavController(this)
                    .navigate(R.id.userBaseInfoFragment);
        } catch (IllegalArgumentException ignored) {
            // If not hosted in nav graph (e.g. inside ProfileActivity),
            // we simply do nothing to avoid breaking existing behavior.
        }
    }

    private void bindProfileForEdit(@Nullable DoctorProfile profile) {
        if (profile == null) {
            return;
        }

        if (inputName != null) {
            String displayName;
            if (profile.getFirstname() != null && profile.getLastname() != null) {
                displayName = profile.getFirstname() + " " + profile.getLastname();
            } else if (profile.getFirstname() != null) {
                displayName = profile.getFirstname();
            } else {
                displayName = "";
            }
            inputName.setText(displayName);
        }

        if (inputPhone != null) {
            inputPhone.setText(profile.getPhone());
        }
        if (inputClinic != null) {
            inputClinic.setText(profile.getClinicAddress());
        }
        if (inputCity != null) {
            inputCity.setText(profile.getCity());
        }
        if (inputCountry != null) {
            inputCountry.setText(profile.getCountry());
        }
        if (inputRegNumber != null) {
            inputRegNumber.setText(profile.getMedicalRegistrationNumber());
        }
        if (inputFee != null && profile.getConsultationFee() != null) {
            inputFee.setText(profile.getConsultationFee().toPlainString());
        }
        if (inputBio != null) {
            inputBio.setText(profile.getBio());
        }

        if (switchAcceptsNew != null && profile.getAcceptsNewPatients() != null) {
            switchAcceptsNew.setChecked(profile.getAcceptsNewPatients());
        }
        if (switchTeleconsult != null && profile.getTeleconsultationEnabled() != null) {
            switchTeleconsult.setChecked(profile.getTeleconsultationEnabled());
        }
    }

    private void saveChanges() {
        String name = inputName != null ? getText(inputName) : "";
        String phone = inputPhone != null ? getText(inputPhone) : "";
        String clinic = inputClinic != null ? getText(inputClinic) : "";
        String city = inputCity != null ? getText(inputCity) : "";
        String country = inputCountry != null ? getText(inputCountry) : "";
        String regNumber = inputRegNumber != null ? getText(inputRegNumber) : "";
        String feeText = inputFee != null ? getText(inputFee) : "";
        String bio = inputBio != null ? getText(inputBio) : "";

        boolean acceptsNew = switchAcceptsNew != null && switchAcceptsNew.isChecked();
        boolean teleconsult = switchTeleconsult != null && switchTeleconsult.isChecked();

        BigDecimal fee = null;
        if (!TextUtils.isEmpty(feeText)) {
            try {
                fee = new BigDecimal(feeText);
            } catch (NumberFormatException ex) {
                Toast.makeText(requireContext(), "Invalid fee value", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // This only updates the ViewModel in-memory.
        // You can additionally call your DoctorApiService.updateMyProfile
        // from here if you want to immediately push to backend.
        viewModel.updateBasicInfo(
                name,
                phone,
                clinic,
                city,
                country,
                regNumber,
                fee,
                bio,
                acceptsNew,
                teleconsult
        );

        Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show();
        requireActivity().onBackPressed();
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }
}
