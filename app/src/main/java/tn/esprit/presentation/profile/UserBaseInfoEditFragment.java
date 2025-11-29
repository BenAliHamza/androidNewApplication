package tn.esprit.presentation.profile;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
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
import com.google.android.material.textfield.TextInputLayout;

import tn.esprit.MainActivity;
import tn.esprit.R;
import tn.esprit.data.profile.ProfileRepository;
import tn.esprit.data.remote.user.UserAccountApiService.UserUpdateRequestDto;
import tn.esprit.domain.doctor.DoctorProfile;
import tn.esprit.domain.patient.PatientProfile;
import tn.esprit.domain.user.User;

public class UserBaseInfoEditFragment extends Fragment {

    private TextInputLayout layoutFirstname;
    private TextInputLayout layoutLastname;
    private TextInputLayout layoutEmail;
    private TextInputLayout layoutPhone;

    private TextInputEditText inputFirstname;
    private TextInputEditText inputLastname;
    private TextInputEditText inputEmail;
    private TextInputEditText inputPhone;
    private View loadingOverlay;

    private MaterialButton buttonCancel;
    private MaterialButton buttonSave;

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

        layoutFirstname = view.findViewById(R.id.layout_firstname);
        layoutLastname = view.findViewById(R.id.layout_lastname);
        layoutEmail = view.findViewById(R.id.layout_email);
        layoutPhone = view.findViewById(R.id.layout_phone);

        inputFirstname = view.findViewById(R.id.input_firstname);
        inputLastname = view.findViewById(R.id.input_lastname);
        inputEmail = view.findViewById(R.id.input_email);
        inputPhone = view.findViewById(R.id.input_phone);
        loadingOverlay = view.findViewById(R.id.base_edit_loading_overlay);

        buttonCancel = view.findViewById(R.id.button_cancel_base_info);
        buttonSave = view.findViewById(R.id.button_save_base_info);

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

        attachClearErrorTextWatchers();
        loadUser();
    }

    private void attachClearErrorTextWatchers() {
        if (inputFirstname != null && layoutFirstname != null) {
            inputFirstname.addTextChangedListener(new SimpleClearErrorWatcher(layoutFirstname));
        }
        if (inputLastname != null && layoutLastname != null) {
            inputLastname.addTextChangedListener(new SimpleClearErrorWatcher(layoutLastname));
        }
        if (inputEmail != null && layoutEmail != null) {
            inputEmail.addTextChangedListener(new SimpleClearErrorWatcher(layoutEmail));
        }
        if (inputPhone != null && layoutPhone != null) {
            inputPhone.addTextChangedListener(new SimpleClearErrorWatcher(layoutPhone));
        }
    }

    private static class SimpleClearErrorWatcher implements TextWatcher {

        private final TextInputLayout layout;

        SimpleClearErrorWatcher(TextInputLayout layout) {
            this.layout = layout;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // no-op
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (layout != null && layout.getError() != null) {
                layout.setError(null);
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
            // no-op
        }
    }

    private void showLoading(boolean show) {
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        }

        boolean enabled = !show;

        if (inputFirstname != null) inputFirstname.setEnabled(enabled);
        if (inputLastname != null) inputLastname.setEnabled(enabled);
        if (inputEmail != null) inputEmail.setEnabled(enabled);
        if (inputPhone != null) inputPhone.setEnabled(enabled);

        if (buttonSave != null) buttonSave.setEnabled(enabled);
        if (buttonCancel != null) buttonCancel.setEnabled(enabled);
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

        clearAllErrors();

        UserUpdateRequestDto request = new UserUpdateRequestDto();

        String first = null;
        String last = null;
        String email = null;
        String phone = null;

        if (inputFirstname != null) {
            first = trimOrNull(inputFirstname.getText());
            request.setFirstname(first);
        }
        if (inputLastname != null) {
            last = trimOrNull(inputLastname.getText());
            request.setLastname(last);
        }
        if (inputEmail != null) {
            email = trimOrNull(inputEmail.getText());
        }
        if (inputPhone != null) {
            phone = trimOrNull(inputPhone.getText());
        }

        // First name required
        if (TextUtils.isEmpty(first)) {
            if (layoutFirstname != null) {
                layoutFirstname.setError(getString(R.string.profile_error_firstname_required));
            }
            if (inputFirstname != null) {
                inputFirstname.requestFocus();
            }
            return;
        }

        // Email validation: required + valid format
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            if (layoutEmail != null) {
                layoutEmail.setError(getString(R.string.profile_error_invalid_email));
            }
            if (inputEmail != null) {
                inputEmail.requestFocus();
            }
            return;
        }
        request.setEmail(email);

        // Phone validation: optional, but if provided must be reasonably valid
        if (!TextUtils.isEmpty(phone) && !isValidPhone(phone)) {
            if (layoutPhone != null) {
                layoutPhone.setError(getString(R.string.profile_error_invalid_phone));
            }
            if (inputPhone != null) {
                inputPhone.requestFocus();
            }
            return;
        }
        request.setPhone(phone);

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

    private void clearAllErrors() {
        if (layoutFirstname != null) layoutFirstname.setError(null);
        if (layoutLastname != null) layoutLastname.setError(null);
        if (layoutEmail != null) layoutEmail.setError(null);
        if (layoutPhone != null) layoutPhone.setError(null);
    }

    private boolean isValidPhone(@NonNull String phone) {
        // Very permissive but avoids obvious bad input (letters, extremely short, etc.)
        String trimmed = phone.trim();
        if (trimmed.length() < 6 || trimmed.length() > 20) {
            return false;
        }
        return trimmed.matches("[0-9+()\\s-]+");
    }

    @Nullable
    private String trimOrNull(@Nullable CharSequence cs) {
        if (cs == null) return null;
        String s = cs.toString().trim();
        return s.isEmpty() ? null : s;
    }
}
