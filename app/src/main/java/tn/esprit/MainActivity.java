package tn.esprit;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

import tn.esprit.data.auth.AuthLocalDataSource;
import tn.esprit.data.profile.ProfileRepository;
import tn.esprit.domain.doctor.DoctorProfile;
import tn.esprit.domain.patient.PatientProfile;
import tn.esprit.domain.user.User;
import tn.esprit.presentation.auth.AuthGateActivity;
import tn.esprit.presentation.home.HomeUiHelper;

public class MainActivity extends AppCompatActivity {

    private View rootView;

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private BottomNavigationView bottomNavigationView;

    private TextView textGreeting;
    private TextView textUserName;
    private TextView textUserRole;

    // Drawer header views
    private TextView drawerUserName;
    private TextView drawerUserEmail;
    private TextView drawerUserRole;

    private AuthLocalDataSource authLocalDataSource;
    private ProfileRepository profileRepository;
    private NavController navController;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        rootView = findViewById(R.id.main_root);
        applyWindowInsets();

        drawerLayout = findViewById(R.id.drawer_layout_main);
        navigationView = findViewById(R.id.nav_view);
        bottomNavigationView = findViewById(R.id.bottom_nav);

        textGreeting = findViewById(R.id.text_greeting);
        textUserName = findViewById(R.id.text_user_name);
        textUserRole = findViewById(R.id.text_user_role);

        authLocalDataSource = new AuthLocalDataSource(getApplicationContext());
        profileRepository = new ProfileRepository(getApplicationContext());

        NavHostFragment navHostFragment =
                (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_main);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
        }

        // Drawer header
        if (navigationView != null) {
            View headerView = navigationView.getHeaderView(0);
            if (headerView != null) {
                drawerUserName = headerView.findViewById(R.id.drawer_user_name);
                drawerUserEmail = headerView.findViewById(R.id.drawer_user_email);
                drawerUserRole = headerView.findViewById(R.id.drawer_user_role);
            }

            navigationView.setNavigationItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.menu_profile) {
                    if (navController != null) {
                        navController.navigate(R.id.profileFragment);
                    }
                } else if (id == R.id.menu_settings) {
                    // Settings placeholder
                } else if (id == R.id.menu_logout) {
                    performLogout();
                }

                if (drawerLayout != null) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                }
                return true;
            });
        }

        // Drawer toggle button
        ImageButton buttonMenu = findViewById(R.id.button_menu);
        if (buttonMenu != null) {
            buttonMenu.setOnClickListener(v -> {
                if (drawerLayout != null) {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            });
        }

        // Initial defaults
        if (textGreeting != null) {
            int greetingResId = HomeUiHelper.resolveGreetingResId();
            textGreeting.setText(getString(greetingResId));
        }
        if (textUserName != null) {
            textUserName.setText(getString(R.string.home_drawer_user_name_placeholder));
        }
        if (textUserRole != null) {
            textUserRole.setText(getString(R.string.profile_role_unknown));
        }

        if (drawerUserName != null) {
            drawerUserName.setText(getString(R.string.home_drawer_user_name_placeholder));
        }
        if (drawerUserEmail != null) {
            drawerUserEmail.setText(getString(R.string.home_drawer_email_placeholder));
        }
        if (drawerUserRole != null) {
            drawerUserRole.setText(getString(R.string.home_drawer_user_role_placeholder));
        }

        // Load profile and adapt header + bottom nav to role
        loadUserAndApplyRole();
    }

    private void applyWindowInsets() {
        if (rootView == null) return;

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

    private void loadUserAndApplyRole() {
        if (profileRepository == null) return;

        profileRepository.loadProfile(new ProfileRepository.ProfileCallback() {
            @Override
            public void onSuccess(User user,
                                  DoctorProfile doctorProfile,
                                  PatientProfile patientProfile) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                String role = user != null ? user.getRole() : null;
                applyHeaderForUser(user, role);
                applyBottomNavForRole(role);
            }

            @Override
            public void onError(Throwable throwable, Integer httpCode, String errorBody) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                // Fallback to generic bottom nav
                applyBottomNavForRole(null);
            }
        });
    }

    private void applyHeaderForUser(@Nullable User user, @Nullable String role) {
        // Greeting by time
        if (textGreeting != null) {
            int greetingResId = HomeUiHelper.resolveGreetingResId();
            textGreeting.setText(getString(greetingResId));
        }

        // Name + email
        String displayName = null;
        String email = null;

        if (user != null) {
            String first = user.getFirstname() != null ? user.getFirstname() : "";
            String last = user.getLastname() != null ? user.getLastname() : "";
            String combined = (first + " " + last).trim();

            if (!combined.isEmpty()) {
                displayName = combined;
            } else if (user.getEmail() != null) {
                displayName = user.getEmail();
            }

            if (user.getEmail() != null) {
                email = user.getEmail();
            }
        }

        if (displayName == null || displayName.isEmpty()) {
            displayName = getString(R.string.home_drawer_user_name_placeholder);
        }
        if (email == null || email.isEmpty()) {
            email = getString(R.string.home_drawer_email_placeholder);
        }

        if (textUserName != null) {
            textUserName.setText(displayName);
        }
        if (drawerUserName != null) {
            drawerUserName.setText(displayName);
        }
        if (drawerUserEmail != null) {
            drawerUserEmail.setText(email);
        }

        // Role label (header + drawer)
        int roleLabelResId = HomeUiHelper.resolveRoleLabelResId(role);
        String roleLabel = getString(roleLabelResId);

        if (textUserRole != null) {
            textUserRole.setText(roleLabel);
        }
        if (drawerUserRole != null) {
            drawerUserRole.setText(roleLabel);
        }
    }

    private void applyBottomNavForRole(@Nullable String role) {
        if (bottomNavigationView == null) {
            return;
        }

        bottomNavigationView.getMenu().clear();

        if (role != null && "DOCTOR".equalsIgnoreCase(role)) {
            bottomNavigationView.inflateMenu(R.menu.menu_home_bottom_doctor);
        } else if (role != null && "PATIENT".equalsIgnoreCase(role)) {
            bottomNavigationView.inflateMenu(R.menu.menu_home_bottom_patient);
        } else {
            bottomNavigationView.inflateMenu(R.menu.menu_home_bottom);
        }

        // Default selected tab
        bottomNavigationView.setSelectedItemId(R.id.menu_home);

        // Wire navigation to NavController
        setupBottomNavNavigation();
    }

    private void setupBottomNavNavigation() {
        if (bottomNavigationView == null || navController == null) {
            return;
        }

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.menu_home) {
                navController.navigate(R.id.homeFragment);
                return true;
            } else if (id == R.id.menu_profile) {
                navController.navigate(R.id.profileFragment);
                return true;
            }

            // Other items (settings, etc.) can be handled here later
            return true;
        });
    }

    private void performLogout() {
        if (authLocalDataSource != null) {
            authLocalDataSource.clearTokens();
        }

        Intent intent = new Intent(this, AuthGateActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
