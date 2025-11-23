package tn.esprit.presentation.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import tn.esprit.R;

/**
 * Lightweight gate fragment:
 * - Shown as the nav graph start destination.
 * - Displays a minimal loading UI.
 * - MainActivity decides where to navigate (doctor vs patient home) once role is known.
 */
public class HomeGateFragment extends Fragment {

    public HomeGateFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home_gate, container, false);
    }
}
