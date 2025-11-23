package tn.esprit.presentation.appointment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import tn.esprit.R;
import tn.esprit.domain.appointment.Slot;

public class DoctorSlotAdapter extends RecyclerView.Adapter<DoctorSlotAdapter.SlotViewHolder> {

    private final List<Slot> slots = new ArrayList<>();

    public void setSlots(List<Slot> newSlots) {
        slots.clear();
        if (newSlots != null) {
            slots.addAll(newSlots);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SlotViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_doctor_slot, parent, false);
        return new SlotViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull SlotViewHolder holder, int position) {
        Slot slot = slots.get(position);

        String start = slot.startDateTime;
        String end = slot.endDateTime;

        String timeText;
        try {
            // expecting: "YYYY-MM-DDTHH:MM:SS"
            String startTime = (start != null && start.length() >= 16)
                    ? start.substring(11, 16)
                    : start;
            String endTime = (end != null && end.length() >= 16)
                    ? end.substring(11, 16)
                    : end;

            timeText = startTime + " - " + endTime;
        } catch (Exception e) {
            timeText = (start != null ? start : "") + " - " + (end != null ? end : "");
        }

        holder.textSlotTime.setText(timeText);
        holder.textSlotStatus.setText(slot.status);
    }

    @Override
    public int getItemCount() {
        return slots.size();
    }

    static class SlotViewHolder extends RecyclerView.ViewHolder {
        TextView textSlotTime;
        TextView textSlotStatus;

        SlotViewHolder(@NonNull View itemView) {
            super(itemView);
            textSlotTime = itemView.findViewById(R.id.textSlotTime);
            textSlotStatus = itemView.findViewById(R.id.textSlotStatus);
        }
    }
}
