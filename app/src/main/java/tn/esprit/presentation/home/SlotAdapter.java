package tn.esprit.presentation.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import tn.esprit.R;
import tn.esprit.domain.appointment.Slot;

/**
 * Grid adapter for time slots in the book-appointment bottom sheet.
 * - Shows "HH:mm"
 * - Highlights the currently selected slot.
 */
public class SlotAdapter extends ListAdapter<Slot, SlotAdapter.SlotViewHolder> {

    public interface OnSlotClickListener {
        void onSlotClicked(@NonNull Slot slot);
    }

    @Nullable
    private final OnSlotClickListener listener;

    // Track selected position for highlight
    private int selectedPosition = RecyclerView.NO_POSITION;

    private static final DiffUtil.ItemCallback<Slot> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Slot>() {
                @Override
                public boolean areItemsTheSame(@NonNull Slot oldItem, @NonNull Slot newItem) {
                    // Use time string as an id (08:30, 09:00, ...)
                    String t1 = oldItem.getTime();
                    String t2 = newItem.getTime();
                    if (t1 == null || t2 == null) {
                        return oldItem == newItem;
                    }
                    return t1.equals(t2);
                }

                @Override
                public boolean areContentsTheSame(@NonNull Slot oldItem, @NonNull Slot newItem) {
                    String t1 = oldItem.getTime();
                    String t2 = newItem.getTime();
                    if (t1 == null ? t2 != null : !t1.equals(t2)) {
                        return false;
                    }
                    return oldItem.isAvailable() == newItem.isAvailable();
                }
            };

    public SlotAdapter(@Nullable OnSlotClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public SlotViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_slot_time, parent, false);
        return new SlotViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull SlotViewHolder holder, int position) {
        Slot slot = getItem(position);
        boolean isSelected = (position == selectedPosition);
        holder.bind(slot, isSelected);

        holder.itemView.setOnClickListener(v -> {
            if (!slot.isAvailable()) {
                // Should not happen because we filter, but just in case
                return;
            }

            int oldPos = selectedPosition;
            selectedPosition = holder.getBindingAdapterPosition();

            if (oldPos != RecyclerView.NO_POSITION) {
                notifyItemChanged(oldPos);
            }
            if (selectedPosition != RecyclerView.NO_POSITION) {
                notifyItemChanged(selectedPosition);
            }

            if (listener != null) {
                listener.onSlotClicked(slot);
            }
        });
    }

    /**
     * Called when you change the day so previous selection is cleared.
     */
    public void clearSelection() {
        int oldPos = selectedPosition;
        selectedPosition = RecyclerView.NO_POSITION;
        if (oldPos != RecyclerView.NO_POSITION) {
            notifyItemChanged(oldPos);
        }
    }

    static class SlotViewHolder extends RecyclerView.ViewHolder {

        private final MaterialCardView card;
        private final TextView textTime;

        SlotViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.card_slot_time);
            textTime = itemView.findViewById(R.id.text_slot_time);
        }

        void bind(@NonNull Slot slot, boolean isSelected) {
            String time = slot.getTime();
            textTime.setText(time != null ? time : "");

            boolean available = slot.isAvailable();

            if (!available) {
                // Greyed out (in case backend ever sends unavailable)
                card.setCardBackgroundColor(
                        ContextCompat.getColor(itemView.getContext(), android.R.color.transparent)
                );
                card.setStrokeWidth(1);
                card.setStrokeColor(
                        ContextCompat.getColor(itemView.getContext(), R.color.color_on_background_secondary)
                );
                textTime.setTextColor(
                        ContextCompat.getColor(itemView.getContext(), R.color.color_on_background_secondary)
                );
                card.setAlpha(0.4f);
                return;
            }

            card.setAlpha(1f);

            if (isSelected) {
                // Selected: filled primary color + white text
                card.setCardBackgroundColor(
                        ContextCompat.getColor(itemView.getContext(), R.color.color_primary)
                );
                card.setStrokeWidth(0);
                textTime.setTextColor(
                        ContextCompat.getColor(itemView.getContext(), android.R.color.white)
                );
            } else {
                // Normal available: transparent background + subtle border
                card.setCardBackgroundColor(
                        ContextCompat.getColor(itemView.getContext(), android.R.color.transparent)
                );
                card.setStrokeWidth(1);
                card.setStrokeColor(
                        ContextCompat.getColor(itemView.getContext(), R.color.color_on_background_secondary)
                );
                textTime.setTextColor(
                        ContextCompat.getColor(itemView.getContext(), R.color.color_on_background)
                );
            }
        }
    }
}
