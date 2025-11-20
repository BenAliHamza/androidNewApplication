package tn.esprit.presentation.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import tn.esprit.MainActivity;
import tn.esprit.R;
import tn.esprit.data.auth.AuthLocalDataSource;
import tn.esprit.data.auth.AuthRepository;
import tn.esprit.domain.auth.AuthTokens;

public class SignupActivity extends AppCompatActivity {

    private View rootView;
    private View loadingOverlay;

    private TextInputEditText inputFirstname;
    private TextInputEditText inputLastname;
    private TextInputEditText inputEmail;
    private TextInputEditText inputPhone;
    private TextInputEditText inputPassword;
    private TextInputEditText inputConfirmPassword;
    private ChipGroup chipGroupRole;
    private Chip chipDoctor;
    private Chip chipPatient;
    private MaterialButton buttonSignup;
    private TextView textBackToLogin;

    private AuthRepository authRepository;
    private AuthLocalDataSource authLocalDataSource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup);

        authRepository = new AuthRepository();
        authLocalDataSource = new AuthLocalDataSource(getApplicationContext());

        bindViews();
        applyWindowInsets();
        setupListeners();
    }

    private void bindViews() {
        rootView = findViewById(R.id.signup_root);
        loadingOverlay = findViewById(R.id.signup_loading_overlay);

        inputFirstname = findViewById(R.id.input_firstname);
        inputLastname = findViewById(R.id.input_lastname);
        inputEmail = findViewById(R.id.input_email);
        inputPhone = findViewById(R.id.input_phone);
        inputPassword = findViewById(R.id.input_password);
        inputConfirmPassword = findViewById(R.id.input_confirm_password);

        chipGroupRole = findViewById(R.id.chip_group_role);
        chipDoctor = findViewById(R.id.chip_doctor);
        chipPatient = findViewById(R.id.chip_patient);

        buttonSignup = findViewById(R.id.button_signup);
        textBackToLogin = findViewById(R.id.text_back_to_login);
    }

    private void applyWindowInsets() {
        final int paddingLeft = rootView.getPaddingLeft();
        final int paddingTop = rootView.getPaddingTop();
        final int paddingRight = rootView.getPaddingRight();
        final int paddingBottom = rootView.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(
                    paddingLeft + systemBars.left,
                    paddingTop + systemBars.top,
                    paddingRight + systemBars.right,
                    paddingBottom + systemBars.bottom
            );
            return insets;
        });
    }

    private void setupListeners() {
        buttonSignup.setOnClickListener(v -> {
            String firstname = getText(inputFirstname);
            String lastname = getText(inputLastname);
            String email = getText(inputEmail);
            String phone = getText(inputPhone);
            String password = getText(inputPassword);
            String confirmPassword = getText(inputConfirmPassword);
            String role = getSelectedRole();

            if (validateInputs(firstname, lastname, email, phone, password, confirmPassword, role)) {
                performSignup(firstname, lastname, email, phone, password, role);
            }
        });

        textBackToLogin.setOnClickListener(v -> {
            finish(); // go back to LoginActivity
        });
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private String getSelectedRole() {
        int checkedId = chipGroupRole.getCheckedChipId();
        if (checkedId == R.id.chip_doctor) {
            return "DOCTOR";
        } else if (checkedId == R.id.chip_patient) {
            return "PATIENT";
        } else {
            return null;
        }
    }

    private boolean validateInputs(String firstname,
                                   String lastname,
                                   String email,
                                   String phone,
                                   String password,
                                   String confirmPassword,
                                   String role) {
        boolean valid = true;

        if (TextUtils.isEmpty(firstname)) {
            inputFirstname.setError("First name is required");
            valid = false;
        } else {
            inputFirstname.setError(null);
        }

        if (TextUtils.isEmpty(lastname)) {
            inputLastname.setError("Last name is required");
            valid = false;
        } else {
            inputLastname.setError(null);
        }

        if (TextUtils.isEmpty(email)) {
            inputEmail.setError("Email is required");
            valid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            inputEmail.setError("Enter a valid email");
            valid = false;
        } else {
            inputEmail.setError(null);
        }

        // Phone optional
        inputPhone.setError(null);

        if (TextUtils.isEmpty(password)) {
            inputPassword.setError("Password is required");
            valid = false;
        } else if (password.length() < 6) {
            inputPassword.setError("Password must be at least 6 characters");
            valid = false;
        } else {
            inputPassword.setError(null);
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            inputConfirmPassword.setError("Confirm your password");
            valid = false;
        } else if (!password.equals(confirmPassword)) {
            inputConfirmPassword.setError("Passwords do not match");
            valid = false;
        } else {
            inputConfirmPassword.setError(null);
        }

        if (role == null) {
            Snackbar.make(rootView, "Please choose a role (Doctor or Patient)", Snackbar.LENGTH_LONG).show();
            valid = false;
        }

        return valid;
    }

    private void performSignup(String firstname,
                               String lastname,
                               String email,
                               String phone,
                               String password,
                               String role) {

        showLoading(true);

        authRepository.signup(firstname, lastname, email, phone, password, role,
                new AuthRepository.SignupCallback() {
                    @Override
                    public void onSuccess(AuthTokens tokens) {
                        showLoading(false);

                        authLocalDataSource.saveTokens(tokens);
                        Toast.makeText(SignupActivity.this, "Account created", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(SignupActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onError(Throwable throwable, Integer httpCode, String errorBody) {
                        showLoading(false);

                        String message;
                        if (throwable != null) {
                            message = "Network error. Please check your connection.";
                        } else if (httpCode != null && httpCode == 409) {
                            message = "This email is already registered.";
                        } else {
                            message = "Could not create account. Please try again.";
                        }

                        Snackbar.make(rootView, message, Snackbar.LENGTH_LONG).show();
                    }
                });
    }

    private void showLoading(boolean show) {
        loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);

        inputFirstname.setEnabled(!show);
        inputLastname.setEnabled(!show);
        inputEmail.setEnabled(!show);
        inputPhone.setEnabled(!show);
        inputPassword.setEnabled(!show);
        inputConfirmPassword.setEnabled(!show);
        chipDoctor.setEnabled(!show);
        chipPatient.setEnabled(!show);
        buttonSignup.setEnabled(!show);
    }
}
