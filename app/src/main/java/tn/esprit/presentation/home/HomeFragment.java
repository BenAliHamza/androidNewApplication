package tn.esprit.presentation.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;

import tn.esprit.R;

public class HomeFragment extends Fragment {

    private View cardHighlight;
    private TextView textHighlightTitle;
    private TextView textHighlightSubtitle;

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

        cardHighlight = view.findViewById(R.id.card_highlight);
        textHighlightTitle = view.findViewById(R.id.text_highlight_title);
        textHighlightSubtitle = view.findViewById(R.id.text_highlight_subtitle);

        // Generic highlight text (we can make this role-specific later if needed)
        textHighlightTitle.setText(getString(R.string.home_highlight_title));
        textHighlightSubtitle.setText(getString(R.string.home_highlight_subtitle));

        // Simple entrance animation
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
}
