package tn.esprit.presentation.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import tn.esprit.R;
import tn.esprit.domain.user.User;

public class UserBaseInfoFragment extends Fragment {

    private ProfileViewModel viewModel;

    private TextView textFullName;
    private TextView textEmail;
    private TextView textPhone;
    private TextView textRole;
    private Button buttonEditBase;

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

        viewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);

        textFullName = view.findViewById(R.id.text_base_fullname);
        textEmail = view.findViewById(R.id.text_base_email);
        textPhone = view.findViewById(R.id.text_base_phone);
        textRole = view.findViewById(R.id.text_base_role);
        buttonEditBase = view.findViewById(R.id.button_edit_base_info);

        viewModel.getUser().observe(getViewLifecycleOwner(), this::bindUser);

        buttonEditBase.setOnClickListener(v ->
                NavHostFragment.findNavController(UserBaseInfoFragment.this)
                        .navigate(R.id.action_userBaseInfoFragment_to_userBaseInfoEditFragment)
        );
    }

    private void bindUser(@Nullable User user) {
        if (user == null) {
            textFullName.setText("-");
            textEmail.setText("-");
            textPhone.setText("-");
            textRole.setText("-");
            return;
        }

        String first = user.getFirstname() != null ? user.getFirstname() : "";
        String last = user.getLastname() != null ? user.getLastname() : "";
        String combined = (first + " " + last).trim();
        if (combined.isEmpty() && user.getEmail() != null) {
            combined = user.getEmail();
        }
        if (combined.isEmpty()) {
            combined = getString(R.string.home_drawer_user_name_placeholder);
        }

        textFullName.setText(combined);
        textEmail.setText(user.getEmail() != null ? user.getEmail() : "-");
        textPhone.setText(user.getPhone() != null ? user.getPhone() : "-");

        String role = user.getRole() != null ? user.getRole() : "";
        if (!role.isEmpty()) {
            textRole.setText(role);
        } else {
            textRole.setText(getString(R.string.profile_role_unknown));
        }
    }
}
