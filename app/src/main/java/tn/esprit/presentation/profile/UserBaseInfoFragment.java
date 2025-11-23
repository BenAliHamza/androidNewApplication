package tn.esprit.presentation.profile;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
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

public class UserBaseInfoFragment extends Fragment {

    private TextView textFullname;
    private TextView textEmail;
    private TextView textPhone;
    private TextView textRole;

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
        textPhone = view.findViewById(R.id.text_base_phone);
        textRole = view.findViewById(R.id.text_base_role);
        Button buttonEdit = view.findViewById(R.id.button_edit_base_info);

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
        // Reload in case user updated base info and navigated back.
        loadBaseInfo();
    }

    private void loadBaseInfo() {
        profileRepository.loadProfile(new ProfileRepository.ProfileCallback() {
            @Override
            public void onSuccess(User user,
                                  DoctorProfile doctorProfile,
                                  PatientProfile patientProfile) {
                if (!isAdded()) return;

                if (user == null) {
                    Toast.makeText(requireContext(),
                            R.string.profile_error_loading,
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                String displayName = null;
                String first = user.getFirstname() != null ? user.getFirstname() : "";
                String last = user.getLastname() != null ? user.getLastname() : "";
                String combined = (first + " " + last).trim();
                if (!combined.isEmpty()) {
                    displayName = combined;
                } else if (!TextUtils.isEmpty(user.getEmail())) {
                    displayName = user.getEmail();
                }

                // IMPORTANT: no hard-coded fallback name.
                if (TextUtils.isEmpty(displayName)) {
                    displayName = "";
                }

                if (textFullname != null) textFullname.setText(displayName);
                if (textEmail != null) {
                    textEmail.setText(user.getEmail() != null ? user.getEmail() : "");
                }
                if (textPhone != null) {
                    textPhone.setText(user.getPhone() != null ? user.getPhone() : "");
                }
                if (textRole != null) {
                    textRole.setText(user.getRole() != null ? user.getRole() : "");
                }
            }

            @Override
            public void onError(Throwable throwable, Integer httpCode, String errorBody) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(),
                        R.string.profile_error_loading,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}
