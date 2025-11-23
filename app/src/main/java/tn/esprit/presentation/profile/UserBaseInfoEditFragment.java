package tn.esprit.presentation.profile;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import tn.esprit.MainActivity;
import tn.esprit.R;
import tn.esprit.data.profile.ProfileRepository;
import tn.esprit.data.remote.user.UserAccountApiService.UserUpdateRequestDto;
import tn.esprit.domain.doctor.DoctorProfile;
import tn.esprit.domain.patient.PatientProfile;
import tn.esprit.domain.user.User;

public class UserBaseInfoEditFragment extends Fragment {

    private TextInputEditText inputFirstname;
    private TextInputEditText inputLastname;
    private TextInputEditText inputEmail;
    private TextInputEditText inputPhone;
    private View loadingOverlay;

    private ProfileRepository profileRepository;
    private User currentUser;

    public UserBaseInfoEditFragment() {
        // Required empty ctor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        profileRepository = new ProfileRepository(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_user_base_info_edit, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageButton buttonBack = view.findViewById(R.id.button_back_base_info_edit);
        inputFirstname = view.findViewById(R.id.input_firstname);
        inputLastname = view.findViewById(R.id.input_lastname);
        inputEmail = view.findViewById(R.id.input_email);
        inputPhone = view.findViewById(R.id.input_phone);
        loadingOverlay = view.findViewById(R.id.base_edit_loading_overlay);

        MaterialButton buttonCancel = view.findViewById(R.id.button_cancel_base_info);
        MaterialButton buttonSave = view.findViewById(R.id.button_save_base_info);

        if (buttonBack != null) {
            buttonBack.setOnClickListener(v ->
                    NavHostFragment.findNavController(this).navigateUp());
        }

        if (buttonCancel != null) {
            buttonCancel.setOnClickListener(v ->
                    NavHostFragment.findNavController(this).navigateUp());
        }
        if (buttonSave != null) {
            buttonSave.setOnClickListener(v -> saveBaseInfo());
        }

        // Start empty so we never show data from a previous session by accident.
        if (inputFirstname != null) inputFirstname.setText("");
        if (inputLastname != null) inputLastname.setText("");
        if (inputEmail != null) inputEmail.setText("");
        if (inputPhone != null) inputPhone.setText("");

        loadUser();
    }

    private void showLoading(boolean show) {
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void loadUser() {
        showLoading(true);
        profileRepository.loadProfile(new ProfileRepository.ProfileCallback() {
            @Override
            public void onSuccess(User user,
                                  DoctorProfile doctorProfile,
                                  PatientProfile patientProfile) {
                if (!isAdded()) return;
                showLoading(false);
                currentUser = user;

                if (user != null) {
                    if (inputFirstname != null) {
                        inputFirstname.setText(user.getFirstname());
                    }
                    if (inputLastname != null) {
                        inputLastname.setText(user.getLastname());
                    }
                    if (inputEmail != null) {
                        inputEmail.setText(user.getEmail());
                    }
                    if (inputPhone != null) {
                        inputPhone.setText(user.getPhone());
                    }
                }
            }

            @Override
            public void onError(Throwable throwable, Integer httpCode, String errorBody) {
                if (!isAdded()) return;
                showLoading(false);
                Toast.makeText(requireContext(),
                        R.string.profile_error_loading,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveBaseInfo() {
        if (currentUser == null) {
            Toast.makeText(requireContext(),
                    R.string.profile_error_loading,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        UserUpdateRequestDto request = new UserUpdateRequestDto();

        if (inputFirstname != null) {
            String first = trimOrNull(inputFirstname.getText());
            request.setFirstname(first);
        }
        if (inputLastname != null) {
            String last = trimOrNull(inputLastname.getText());
            request.setLastname(last);
        }
        if (inputEmail != null) {
            String email = trimOrNull(inputEmail.getText());
            if (TextUtils.isEmpty(email)) {
                Toast.makeText(requireContext(),
                        R.string.profile_error_invalid_email,
                        Toast.LENGTH_SHORT).show();
                return;
            }
            request.setEmail(email);
        }
        if (inputPhone != null) {
            request.setPhone(trimOrNull(inputPhone.getText()));
        }

        showLoading(true);
        profileRepository.updateBaseUser(request, new ProfileRepository.BaseUserUpdateCallback() {
            @Override
            public void onSuccess(User updatedUser) {
                if (!isAdded()) return;
                showLoading(false);
                Toast.makeText(requireContext(),
                        R.string.profile_saved,
                        Toast.LENGTH_SHORT).show();

                Activity activity = getActivity();
                if (activity instanceof MainActivity) {
                    ((MainActivity) activity).refreshUserProfileUi();
                }

                // Go back to base info view
                NavHostFragment.findNavController(UserBaseInfoEditFragment.this).navigateUp();
            }

            @Override
            public void onError(Throwable throwable, Integer httpCode, String errorBody) {
                if (!isAdded()) return;
                showLoading(false);
                Toast.makeText(requireContext(),
                        R.string.profile_error_saving,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Nullable
    private String trimOrNull(@Nullable CharSequence cs) {
        if (cs == null) return null;
        String s = cs.toString().trim();
        return s.isEmpty() ? null : s;
    }
}
