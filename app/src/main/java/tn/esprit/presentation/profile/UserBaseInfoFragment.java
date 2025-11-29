package tn.esprit.presentation.profile;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import tn.esprit.R;
import tn.esprit.data.profile.ProfileRepository;
import tn.esprit.domain.doctor.DoctorProfile;
import tn.esprit.domain.patient.PatientProfile;
import tn.esprit.domain.user.User;
import tn.esprit.presentation.home.HomeUiHelper;

public class UserBaseInfoFragment extends Fragment {

    private TextView textFullname;
    private TextView textEmail;
    private TextView textEmailLabel;
    private TextView textPhone;
    private TextView textPhoneLabel;
    private TextView textRole;
    private ProgressBar progressBar;

    private ProfileRepository profileRepository;

    public UserBaseInfoFragment() {
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
        return inflater.inflate(R.layout.fragment_user_base_info, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageButton buttonBack = view.findViewById(R.id.button_back_base_info);
        textFullname = view.findViewById(R.id.text_base_fullname);
        textEmail = view.findViewById(R.id.text_base_email);
        textEmailLabel = view.findViewById(R.id.text_base_email_label);
        textPhone = view.findViewById(R.id.text_base_phone);
        textPhoneLabel = view.findViewById(R.id.text_base_phone_label);
        textRole = view.findViewById(R.id.text_base_role);
        Button buttonEdit = view.findViewById(R.id.button_edit_base_info);
        progressBar = view.findViewById(R.id.user_base_info_progress);

        if (buttonBack != null) {
            buttonBack.setOnClickListener(v ->
                    NavHostFragment.findNavController(this).navigateUp());
        }

        if (buttonEdit != null) {
            buttonEdit.setOnClickListener(v ->
                    NavHostFragment.findNavController(this)
                            .navigate(R.id.action_userBaseInfoFragment_to_userBaseInfoEditFragment));
        }

        loadBaseInfo();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadBaseInfo();
    }

    private void setLoading(boolean loading) {
        if (progressBar != null) {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
    }

    private void loadBaseInfo() {
        setLoading(true);
        profileRepository.loadProfile(new ProfileRepository.ProfileCallback() {
            @Override
            public void onSuccess(User user,
                                  DoctorProfile doctorProfile,
                                  PatientProfile patientProfile) {
                if (!isAdded()) return;

                setLoading(false);

                if (user == null) {
                    Toast.makeText(requireContext(),
                            R.string.profile_error_loading,
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                String displayName = null;
                String first = user.getFirstname() != null ? user.getFirstname().trim() : "";
                String last = user.getLastname() != null ? user.getLastname().trim() : "";
                String combined = (first + " " + last).trim();
                if (!combined.isEmpty()) {
                    displayName = combined;
                } else if (!TextUtils.isEmpty(user.getEmail())) {
                    displayName = user.getEmail();
                }

                if (TextUtils.isEmpty(displayName)) {
                    displayName = "";
                }

                if (textFullname != null) {
                    textFullname.setText(displayName);
                }

                // Email
                String email = user.getEmail();
                boolean hasEmail = !TextUtils.isEmpty(email);
                if (textEmail != null) {
                    textEmail.setText(hasEmail ? email : "");
                }
                if (textEmailLabel != null) {
                    textEmailLabel.setVisibility(hasEmail ? View.VISIBLE : View.GONE);
                }

                // Phone
                String phone = user.getPhone();
                boolean hasPhone = !TextUtils.isEmpty(phone);
                if (textPhone != null) {
                    textPhone.setText(hasPhone ? phone : "");
                }
                if (textPhoneLabel != null) {
                    textPhoneLabel.setVisibility(hasPhone ? View.VISIBLE : View.GONE);
                }

                // Role label (Doctor / Patient / etc.)
                if (textRole != null) {
                    String roleLabel = "";
                    String role = user.getRole();
                    if (!TextUtils.isEmpty(role)) {
                        int resId = HomeUiHelper.resolveRoleLabelResId(role);
                        roleLabel = getString(resId);
                    }
                    textRole.setText(roleLabel);
                }
            }

            @Override
            public void onError(Throwable throwable, Integer httpCode, String errorBody) {
                if (!isAdded()) return;
                setLoading(false);
                Toast.makeText(requireContext(),
                        R.string.profile_error_loading,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}
