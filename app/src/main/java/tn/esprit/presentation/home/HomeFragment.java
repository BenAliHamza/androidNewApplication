package tn.esprit.presentation.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

import java.util.Calendar;

import tn.esprit.R;
import tn.esprit.data.auth.AuthLocalDataSource;
import tn.esprit.presentation.auth.AuthGateActivity;
import tn.esprit.presentation.profile.ProfileActivity;

public class HomeFragment extends Fragment {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private BottomNavigationView bottomNavigationView;

    private View cardHighlight;
    private TextView textGreeting;
    private TextView textUserName;
    private TextView textHighlightTitle;
    private TextView textHighlightSubtitle;

    private AuthLocalDataSource authLocalDataSource;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        drawerLayout = view.findViewById(R.id.drawer_layout);
        navigationView = view.findViewById(R.id.nav_view);
        bottomNavigationView = view.findViewById(R.id.bottom_nav);

        cardHighlight = view.findViewById(R.id.card_highlight);
        textGreeting = view.findViewById(R.id.text_greeting);
        textUserName = view.findViewById(R.id.text_user_name);
        textHighlightTitle = view.findViewById(R.id.text_highlight_title);
        textHighlightSubtitle = view.findViewById(R.id.text_highlight_subtitle);

        authLocalDataSource = new AuthLocalDataSource(requireContext().getApplicationContext());

        ImageButton buttonMenu = view.findViewById(R.id.button_menu);
        if (buttonMenu != null) {
            buttonMenu.setOnClickListener(v -> {
                if (drawerLayout != null) {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            });
        }

        // Bottom navigation: purely visual for now (placeholders).
        if (bottomNavigationView != null) {
            bottomNavigationView.setOnItemSelectedListener(item -> {
                // TODO: later wire real navigation for Home / Appointments / Messages / More
                return true; // keep selection only
            });
        }

        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(item -> {
                int id = item.getItemId();

                if (id == R.id.menu_profile) {
                    // Open profile screen in its own activity
                    if (drawerLayout != null) {
                        drawerLayout.closeDrawer(GravityCompat.START);
                    }
                    Intent intent = new Intent(requireContext(), ProfileActivity.class);
                    startActivity(intent);
                    return true;
                } else if (id == R.id.menu_logout) {
                    if (drawerLayout != null) {
                        drawerLayout.closeDrawer(GravityCompat.START);
                    }

                    // Clear session tokens
                    authLocalDataSource.clearTokens();

                    // Go back through AuthGate to re-check session and show login
                    Intent intent = new Intent(requireContext(), AuthGateActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    return true;
                } else {
                    // Settings and any other future items: placeholder for now
                    if (drawerLayout != null) {
                        drawerLayout.closeDrawer(GravityCompat.START);
                    }
                    return true;
                }
            });
        }

        updateHeaderAndHighlightPlaceholders();

        // Simple entrance animation for the highlight card.
        if (cardHighlight != null) {
            cardHighlight.setAlpha(0f);
            cardHighlight.setTranslationY(24f);
            ViewCompat.animate(cardHighlight)
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(400L)
                    .setStartDelay(80L)
                    .start();
        }
    }

    private void updateHeaderAndHighlightPlaceholders() {
        if (getContext() == null) return;

        // Time-based greeting
        String greeting = getGreetingForCurrentTime();
        textGreeting.setText(greeting);

        // Placeholder name (will be replaced by real UserDto later)
        textUserName.setText(getString(R.string.home_drawer_user_name_placeholder));

        // Sync drawer header placeholders
        if (navigationView != null) {
            View header = navigationView.getHeaderView(0);
            if (header != null) {
                TextView drawerUserName = header.findViewById(R.id.drawer_user_name);
                TextView drawerUserEmail = header.findViewById(R.id.drawer_user_email);

                if (drawerUserName != null) {
                    drawerUserName.setText(getString(R.string.home_drawer_user_name_placeholder));
                }
                if (drawerUserEmail != null) {
                    drawerUserEmail.setText(getString(R.string.home_drawer_email_placeholder));
                }
            }
        }

        // Highlight card texts
        textHighlightTitle.setText(getString(R.string.home_highlight_title));
        textHighlightSubtitle.setText(getString(R.string.home_highlight_subtitle));
    }

    private String getGreetingForCurrentTime() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);

        if (hour < 12) {
            return getString(R.string.home_greeting_morning);
        } else if (hour < 18) {
            return getString(R.string.home_greeting_afternoon);
        } else {
            return getString(R.string.home_greeting_evening);
        }
    }
}
