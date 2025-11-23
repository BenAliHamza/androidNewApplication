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

    // "Chips" – actually TextViews styled as chips
    private TextView chipQuickBook;
    private TextView chipQuickMessages;
    private TextView chipQuickReports;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home,
                container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Highlight card views
        cardHighlight = view.findViewById(R.id.card_highlight);
        textHighlightTitle = view.findViewById(R.id.text_highlight_title);
        textHighlightSubtitle = view.findViewById(R.id.text_highlight_subtitle);

        // Quick action "chips" (TextViews)
        chipQuickBook = view.findViewById(R.id.chip_quick_book);
        chipQuickMessages = view.findViewById(R.id.chip_quick_messages);
        chipQuickReports = view.findViewById(R.id.chip_quick_reports);

        // Generic highlight text
        if (textHighlightTitle != null) {
            textHighlightTitle.setText(getString(R.string.home_highlight_title));
        }
        if (textHighlightSubtitle != null) {
            textHighlightSubtitle.setText(getString(R.string.home_highlight_subtitle));
        }

        // "Book visit" chip: no action for now
        if (chipQuickBook != null) {
            chipQuickBook.setOnClickListener(v -> {
                // intentionally empty
            });
        }

        // Reserved for future features
        if (chipQuickMessages != null) {
            chipQuickMessages.setOnClickListener(v -> {
                // TODO: messages screen later
            });
        }

        if (chipQuickReports != null) {
            chipQuickReports.setOnClickListener(v -> {
                // TODO: reports screen later
            });
        }

        // Simple entrance animation on highlight card
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
