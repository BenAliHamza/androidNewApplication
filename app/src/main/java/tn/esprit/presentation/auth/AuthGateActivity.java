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
import tn.esprit.data.auth.AuthRepository;
import tn.esprit.domain.auth.AuthTokens;

public class AuthGateActivity extends AppCompatActivity {

    private View rootView;
    private AuthLocalDataSource authLocalDataSource;
    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_auth_gate);

        rootView = findViewById(R.id.auth_gate_root);
        applyWindowInsets();

        authLocalDataSource = new AuthLocalDataSource(getApplicationContext());
        authRepository = new AuthRepository();

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

        if (existingTokens == null ||
                existingTokens.getRefreshToken() == null ||
                existingTokens.getRefreshToken().isEmpty()) {
            goToLogin();
            return;
        }

        authRepository.refreshToken(
                existingTokens.getRefreshToken(),
                new AuthRepository.RefreshCallback() {
                    @Override
                    public void onSuccess(AuthTokens tokens) {
                        authLocalDataSource.saveTokens(tokens);
                        goToMain();
                    }

                    @Override
                    public void onError(Throwable throwable, Integer httpCode, String errorBody) {
                        authLocalDataSource.clearTokens();
                        goToLogin();
                    }
                }
        );
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
}
