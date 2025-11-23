package tn.esprit;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
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

import com.bumptech.glide.Glide;
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

    // Header avatar
    private ImageView imageAvatar;

    // Drawer header views
    private ImageView drawerAvatar;
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
        imageAvatar = findViewById(R.id.image_avatar);

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
                drawerAvatar = headerView.findViewById(R.id.drawer_avatar);
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

        // Greeting can be set immediately (not user-specific)
        if (textGreeting != null) {
            int greetingResId = HomeUiHelper.resolveGreetingResId();
            textGreeting.setText(getString(greetingResId));
        }

        // Clear any placeholders (no fake names)
        clearUserHeaderFields();

        // Set up bottom nav listener ONCE
        setupBottomNavNavigation();

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

    /**
     * Public API used by profile/edit fragments after a successful update.
     * Must NEVER force navigation (no auto-jumps to Home).
     */
    public void refreshUserProfileUi() {
        loadUserAndApplyRole();
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
                // Fallback: clear header + generic bottom nav. No leaking previous user.
                applyHeaderForUser(null, null);
                applyBottomNavForRole(null);
            }
        });
    }

    private void applyHeaderForUser(@Nullable User user, @Nullable String role) {
        // Greeting by time (safe)
        if (textGreeting != null) {
            int greetingResId = HomeUiHelper.resolveGreetingResId();
            textGreeting.setText(getString(greetingResId));
        }

        // Name + email
        String displayName = null;
        String email = null;
        String imageUrl = null;

        if (user != null) {
            String first = user.getFirstname() != null ? user.getFirstname() : "";
            String last = user.getLastname() != null ? user.getLastname() : "";
            String combined = (first + " " + last).trim();

            if (!combined.isEmpty()) {
                displayName = combined;
            } else if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                displayName = user.getEmail();
            }

            if (user.getEmail() != null) {
                email = user.getEmail();
            }

            // New: profile image URL from backend
            imageUrl = user.getProfileImage();
        }

        // NO placeholders: if we don't know, show blank.
        if (displayName == null) displayName = "";
        if (email == null) email = "";

        if (textUserName != null) textUserName.setText(displayName);
        if (drawerUserName != null) drawerUserName.setText(displayName);
        if (drawerUserEmail != null) drawerUserEmail.setText(email);

        // Role label (header + drawer)
        int roleLabelResId = HomeUiHelper.resolveRoleLabelResId(role);
        String roleLabel = getString(roleLabelResId);

        if (textUserRole != null) textUserRole.setText(roleLabel);
        if (drawerUserRole != null) drawerUserRole.setText(roleLabel);

        // Avatar in header + drawer, using Glide
        if (imageAvatar != null) {
            if (!TextUtils.isEmpty(imageUrl)) {
                Glide.with(this)
                        .load(imageUrl)
                        .placeholder(R.drawable.logo)
                        .error(R.drawable.logo)
                        .circleCrop()
                        .into(imageAvatar);
            } else {
                imageAvatar.setImageResource(R.drawable.logo);
            }
        }

        if (drawerAvatar != null) {
            if (!TextUtils.isEmpty(imageUrl)) {
                Glide.with(this)
                        .load(imageUrl)
                        .placeholder(R.drawable.logo)
                        .error(R.drawable.logo)
                        .circleCrop()
                        .into(drawerAvatar);
            } else {
                drawerAvatar.setImageResource(R.drawable.logo);
            }
        }
    }

    private void clearUserHeaderFields() {
        if (textUserName != null) textUserName.setText("");
        if (textUserRole != null) textUserRole.setText("");
        if (drawerUserName != null) drawerUserName.setText("");
        if (drawerUserEmail != null) drawerUserEmail.setText("");
        if (drawerUserRole != null) drawerUserRole.setText("");

        // Reset avatars to logo
        if (imageAvatar != null) {
            imageAvatar.setImageResource(R.drawable.logo);
        }
        if (drawerAvatar != null) {
            drawerAvatar.setImageResource(R.drawable.logo);
        }
    }

    /**
     * Only changes which menu is shown (doctor / patient / generic) and
     * syncs the checked item with the CURRENT destination.
     *
     * Critically: it does NOT forcibly navigate to Home.
     */
    private void applyBottomNavForRole(@Nullable String role) {
        if (bottomNavigationView == null) {
            return;
        }

        int currentDestId = 0;
        if (navController != null && navController.getCurrentDestination() != null) {
            currentDestId = navController.getCurrentDestination().getId();
        }

        bottomNavigationView.getMenu().clear();

        if (role != null && "DOCTOR".equalsIgnoreCase(role)) {
            bottomNavigationView.inflateMenu(R.menu.menu_home_bottom_doctor);
        } else if (role != null && "PATIENT".equalsIgnoreCase(role)) {
            bottomNavigationView.inflateMenu(R.menu.menu_home_bottom_patient);
        } else {
            bottomNavigationView.inflateMenu(R.menu.menu_home_bottom);
        }

        // Keep bottom nav selection in sync with where we actually are.
        if (currentDestId == R.id.homeFragment) {
            bottomNavigationView.setSelectedItemId(R.id.menu_home);
        } else {
            // Leave nothing explicitly selected; avoids auto-jumping when in Profile.
        }
    }

    private void setupBottomNavNavigation() {
        if (bottomNavigationView == null || navController == null) {
            return;
        }

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.menu_home) {
                // Navigate to home ONLY if we're not already there.
                if (navController.getCurrentDestination() == null
                        || navController.getCurrentDestination().getId() != R.id.homeFragment) {
                    navController.navigate(R.id.homeFragment);
                }
                return true;
            }

            // Other items (calendar, patients, office, etc.) will be wired later.
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
