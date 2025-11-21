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
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import tn.esprit.R;
import tn.esprit.data.auth.AuthLocalDataSource;
import tn.esprit.data.auth.AuthRepository;
import tn.esprit.domain.auth.AuthTokens;

public class LoginActivity extends AppCompatActivity {

    private View rootView;
    private View loadingOverlay;
    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private MaterialButton loginButton;
    private TextView signupHintText;

    private AuthRepository authRepository;
    private AuthLocalDataSource authLocalDataSource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        rootView = findViewById(R.id.login_root);
        loadingOverlay = findViewById(R.id.loading_overlay);
        emailInput = findViewById(R.id.input_email);
        passwordInput = findViewById(R.id.input_password);
        loginButton = findViewById(R.id.button_login);
        signupHintText = findViewById(R.id.text_signup_hint);

        authRepository = new AuthRepository();
        authLocalDataSource = new AuthLocalDataSource(getApplicationContext());

        applyWindowInsets();
        setupListeners();
    }

    private void applyWindowInsets() {
        // Capture original padding so we don't accumulate it every time insets change
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
        loginButton.setOnClickListener(v -> {
            String email = emailInput.getText() != null ? emailInput.getText().toString().trim() : "";
            String password = passwordInput.getText() != null ? passwordInput.getText().toString().trim() : "";

            if (validateInputs(email, password)) {
                performLogin(email, password);
            }
        });

        signupHintText.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(intent);
        });
    }

    private boolean validateInputs(String email, String password) {
        boolean valid = true;

        if (TextUtils.isEmpty(email)) {
            emailInput.setError(getString(R.string.login_error_required_email));
            valid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError(getString(R.string.login_error_invalid_email));
            valid = false;
        } else {
            emailInput.setError(null);
        }

        if (TextUtils.isEmpty(password)) {
            passwordInput.setError(getString(R.string.login_error_required_password));
            valid = false;
        } else if (password.length() < 6) {
            passwordInput.setError(getString(R.string.login_error_short_password));
            valid = false;
        } else {
            passwordInput.setError(null);
        }

        return valid;
    }

    private void performLogin(String email, String password) {
        showLoading(true);

        authRepository.login(email, password, new AuthRepository.LoginCallback() {
            @Override
            public void onSuccess(AuthTokens tokens) {
                showLoading(false);

                // Persist tokens locally
                authLocalDataSource.saveTokens(tokens);

                Toast.makeText(LoginActivity.this, getString(R.string.login_success), Toast.LENGTH_SHORT).show();

                // IMPORTANT: let AuthGate decide (onboarding vs home)
                Intent intent = new Intent(LoginActivity.this, AuthGateActivity.class);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(Throwable throwable, Integer httpCode, String errorBody) {
                showLoading(false);

                String message;
                if (throwable != null) {
                    message = getString(R.string.login_error_network);
                } else if (httpCode != null && (httpCode == 400 || httpCode == 401)) {
                    message = getString(R.string.login_error_invalid_credentials);
                } else {
                    message = getString(R.string.login_error_generic);
                }

                Snackbar.make(rootView, message, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void showLoading(boolean show) {
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        loginButton.setEnabled(!show);
        emailInput.setEnabled(!show);
        passwordInput.setEnabled(!show);
        signupHintText.setEnabled(!show);
    }
}
