package tn.esprit.presentation.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import tn.esprit.MainActivity;
import tn.esprit.R;
import tn.esprit.data.auth.AuthLocalDataSource;
import tn.esprit.data.remote.ApiClient;
import tn.esprit.data.remote.user.UserApiService;
import tn.esprit.domain.auth.AuthTokens;
import tn.esprit.domain.user.User;
import tn.esprit.presentation.onboarding.PatientOnboardingActivity;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthGateActivity extends AppCompatActivity {

    private View rootView;
    private AuthLocalDataSource authLocalDataSource;
    private UserApiService userApiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_auth_gate);

        rootView = findViewById(R.id.auth_gate_root);
        applyWindowInsets();

        authLocalDataSource = new AuthLocalDataSource(getApplicationContext());
        userApiService = ApiClient.createService(UserApiService.class);

        checkSession();
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

    private void checkSession() {
        AuthTokens existingTokens = authLocalDataSource.getTokens();

        // No tokens or no access token -> go to login
        if (existingTokens == null
                || existingTokens.getAccessToken() == null
                || existingTokens.getAccessToken().isEmpty()) {
            goToLogin();
            return;
        }

        // We already have tokens (from login/signup or previous session) -> just route
        fetchUserAndRoute(existingTokens);
    }

    private void fetchUserAndRoute(AuthTokens tokens) {
        String authHeader = buildAuthHeader(tokens);

        userApiService.getCurrentUser(authHeader).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    // If we can't load /me, fallback to login to be safe
                    authLocalDataSource.clearTokens();
                    goToLogin();
                    return;
                }

                User user = response.body();
                String role = user.getRole();
                Boolean firstLoginFlag = user.getFirstLogin();
                boolean isFirstLogin = firstLoginFlag != null && firstLoginFlag;

                if ("PATIENT".equalsIgnoreCase(role) && isFirstLogin) {
                    goToPatientOnboarding();
                } else {
                    // For DOCTOR and others (or non-first login), go to main home
                    goToMain();
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                // On network failure, safest is to go to login
                authLocalDataSource.clearTokens();
                goToLogin();
            }
        });
    }

    private String buildAuthHeader(AuthTokens tokens) {
        String type = tokens.getTokenType() != null ? tokens.getTokenType() : "Bearer";
        return type + " " + tokens.getAccessToken();
    }

    private void goToLogin() {
        Intent intent = new Intent(AuthGateActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void goToMain() {
        Intent intent = new Intent(AuthGateActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void goToPatientOnboarding() {
        Intent intent = new Intent(AuthGateActivity.this, PatientOnboardingActivity.class);
        startActivity(intent);
        finish();
    }
}
