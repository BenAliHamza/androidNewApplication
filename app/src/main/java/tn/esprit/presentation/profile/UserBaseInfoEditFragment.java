package tn.esprit.presentation.profile;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import tn.esprit.R;
import tn.esprit.data.auth.AuthLocalDataSource;
import tn.esprit.data.remote.ApiClient;
import tn.esprit.data.remote.user.UserAccountApiService;
import tn.esprit.domain.auth.AuthTokens;
import tn.esprit.domain.user.User;

public class UserBaseInfoEditFragment extends Fragment {

    private ProfileViewModel viewModel;

    private TextInputEditText inputFirstname;
    private TextInputEditText inputLastname;
    private TextInputEditText inputEmail;
    private TextInputEditText inputPhone;
    private MaterialButton buttonSave;
    private MaterialButton buttonCancel;
    private View loadingOverlay;

    private UserAccountApiService userAccountApiService;
    private AuthLocalDataSource authLocalDataSource;

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

        viewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);
        userAccountApiService = ApiClient.createService(UserAccountApiService.class);
        authLocalDataSource = new AuthLocalDataSource(requireContext().getApplicationContext());

        inputFirstname = view.findViewById(R.id.input_firstname);
        inputLastname = view.findViewById(R.id.input_lastname);
        inputEmail = view.findViewById(R.id.input_email);
        inputPhone = view.findViewById(R.id.input_phone);
        buttonSave = view.findViewById(R.id.button_save_base_info);
        buttonCancel = view.findViewById(R.id.button_cancel_base_info);
        loadingOverlay = view.findViewById(R.id.base_edit_loading_overlay);

        viewModel.getUser().observe(getViewLifecycleOwner(), this::prefillUser);

        buttonSave.setOnClickListener(v -> submit());
        buttonCancel.setOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());
    }

    private void prefillUser(@Nullable User user) {
        if (user == null) return;

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

    private void submit() {
        String firstname = textOf(inputFirstname);
        String lastname = textOf(inputLastname);
        String email = textOf(inputEmail);
        String phone = textOf(inputPhone);

        if (TextUtils.isEmpty(email)) {
            inputEmail.setError("Email is required");
            return;
        } else {
            inputEmail.setError(null);
        }

        AuthTokens tokens = authLocalDataSource.getTokens();
        if (tokens == null || tokens.getAccessToken() == null || tokens.getAccessToken().isEmpty()) {
            Toast.makeText(requireContext(),
                    getString(R.string.profile_error_not_authenticated),
                    Toast.LENGTH_LONG).show();
            return;
        }

        String type = tokens.getTokenType() != null ? tokens.getTokenType() : "Bearer";
        String authHeader = type + " " + tokens.getAccessToken();

        UserAccountApiService.UserUpdateRequestDto body =
                new UserAccountApiService.UserUpdateRequestDto();
        body.setFirstname(emptyToNull(firstname));
        body.setLastname(emptyToNull(lastname));
        body.setEmail(emptyToNull(email));
        body.setPhone(emptyToNull(phone));

        showLoading(true);

        userAccountApiService.updateCurrentUser(authHeader, body)
                .enqueue(new Callback<User>() {
                    @Override
                    public void onResponse(Call<User> call, Response<User> response) {
                        if (!isAdded()) return;
                        showLoading(false);

                        if (!response.isSuccessful() || response.body() == null) {
                            Toast.makeText(requireContext(),
                                    getString(R.string.profile_error_generic),
                                    Toast.LENGTH_LONG).show();
                            return;
                        }

                        User updated = response.body();
                        viewModel.setUser(updated);

                        Toast.makeText(requireContext(),
                                "Base information updated",
                                Toast.LENGTH_SHORT).show();

                        NavHostFragment.findNavController(UserBaseInfoEditFragment.this)
                                .navigateUp();
                    }

                    @Override
                    public void onFailure(Call<User> call, Throwable t) {
                        if (!isAdded()) return;
                        showLoading(false);
                        Toast.makeText(requireContext(),
                                getString(R.string.profile_error_network),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void showLoading(boolean show) {
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        boolean enabled = !show;
        inputFirstname.setEnabled(enabled);
        inputLastname.setEnabled(enabled);
        inputEmail.setEnabled(enabled);
        inputPhone.setEnabled(enabled);
        buttonSave.setEnabled(enabled);
        buttonCancel.setEnabled(enabled);
    }

    private String textOf(TextInputEditText editText) {
        return editText.getText() != null
                ? editText.getText().toString().trim()
                : "";
    }

    private String emptyToNull(String value) {
        return TextUtils.isEmpty(value) ? null : value;
    }
}
