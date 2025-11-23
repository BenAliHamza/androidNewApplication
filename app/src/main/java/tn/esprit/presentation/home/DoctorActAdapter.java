package tn.esprit.presentation.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import tn.esprit.R;
import tn.esprit.domain.doctor.DoctorPublicProfile;

public class DoctorActAdapter extends ListAdapter<DoctorPublicProfile.Act, DoctorActAdapter.ActViewHolder> {

    public DoctorActAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<DoctorPublicProfile.Act> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<DoctorPublicProfile.Act>() {
                @Override
                public boolean areItemsTheSame(@NonNull DoctorPublicProfile.Act oldItem,
                                               @NonNull DoctorPublicProfile.Act newItem) {
                    if (oldItem.getId() == null || newItem.getId() == null) {
                        return oldItem == newItem;
                    }
                    return oldItem.getId().equals(newItem.getId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull DoctorPublicProfile.Act oldItem,
                                                  @NonNull DoctorPublicProfile.Act newItem) {
                    return safeEquals(oldItem.getName(), newItem.getName())
                            && safeEquals(oldItem.getDescription(), newItem.getDescription());
                }

                private boolean safeEquals(Object a, Object b) {
                    if (a == b) return true;
                    if (a == null || b == null) return false;
                    return a.equals(b);
                }
            };

    @NonNull
    @Override
    public ActViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_doctor_act, parent, false);
        return new ActViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ActViewHolder holder, int position) {
        DoctorPublicProfile.Act act = getItem(position);
        holder.bind(act);
    }

    static class ActViewHolder extends RecyclerView.ViewHolder {

        private final TextView textName;
        private final TextView textDescription;

        ActViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.text_act_name);
            textDescription = itemView.findViewById(R.id.text_act_description);
        }

        void bind(@NonNull DoctorPublicProfile.Act act) {
            String name = act.getName();
            if (name == null) name = "";
            textName.setText(name);

            String description = act.getDescription();
            if (description == null || description.trim().isEmpty()) {
                textDescription.setVisibility(View.GONE);
            } else {
                textDescription.setVisibility(View.VISIBLE);
                textDescription.setText(description.trim());
            }
        }
    }
}
