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
import tn.esprit.domain.doctor.DoctorSearchResult;

public class DoctorSearchResultAdapter
        extends ListAdapter<DoctorSearchResult, DoctorSearchResultAdapter.DoctorViewHolder> {

    public interface OnDoctorClickListener {
        void onDoctorClicked(@NonNull DoctorSearchResult doctor);
    }

    private final OnDoctorClickListener clickListener;

    public DoctorSearchResultAdapter(@NonNull OnDoctorClickListener clickListener) {
        super(DIFF_CALLBACK);
        this.clickListener = clickListener;
    }

    private static final DiffUtil.ItemCallback<DoctorSearchResult> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<DoctorSearchResult>() {
                @Override
                public boolean areItemsTheSame(@NonNull DoctorSearchResult oldItem,
                                               @NonNull DoctorSearchResult newItem) {
                    if (oldItem.getDoctorId() == null || newItem.getDoctorId() == null) {
                        return oldItem == newItem;
                    }
                    return oldItem.getDoctorId().equals(newItem.getDoctorId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull DoctorSearchResult oldItem,
                                                  @NonNull DoctorSearchResult newItem) {
                    return safeEquals(oldItem.getFirstName(), newItem.getFirstName())
                            && safeEquals(oldItem.getLastName(), newItem.getLastName())
                            && safeEquals(oldItem.getSpecialtyName(), newItem.getSpecialtyName())
                            && safeEquals(oldItem.getCity(), newItem.getCity())
                            && safeEquals(oldItem.getCountry(), newItem.getCountry());
                }

                private boolean safeEquals(Object a, Object b) {
                    if (a == b) return true;
                    if (a == null || b == null) return false;
                    return a.equals(b);
                }
            };

    @NonNull
    @Override
    public DoctorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_doctor_search_result, parent, false);
        return new DoctorViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull DoctorViewHolder holder, int position) {
        DoctorSearchResult item = getItem(position);
        holder.bind(item);
    }

    class DoctorViewHolder extends RecyclerView.ViewHolder {

        private final TextView textTitle;
        private final TextView textSubtitle;

        DoctorViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.text_doctor_title);
            textSubtitle = itemView.findViewById(R.id.text_doctor_subtitle);

            itemView.setOnClickListener(v -> {
                int pos = getBindingAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;
                DoctorSearchResult item = getItem(pos);
                if (item != null && clickListener != null) {
                    clickListener.onDoctorClicked(item);
                }
            });
        }

        void bind(@NonNull DoctorSearchResult doctor) {
            String displayName = doctor.getDisplayNameCompact();
            if (displayName == null) {
                displayName = "";
            }

            String specialty = doctor.getSpecialtyName();
            if (specialty == null) {
                specialty = "";
            }

            // Title: "Lastname F. · Specialty"
            String titleText;
            if (!specialty.isEmpty()) {
                titleText = itemView.getContext().getString(
                        R.string.home_patient_doctor_title_format,
                        displayName,
                        specialty
                );
                textTitle.setText(titleText);
            } else {
                titleText = displayName;
                textTitle.setText(displayName);
            }

            // Subtitle: city, country
            StringBuilder subtitle = new StringBuilder();
            if (doctor.getCity() != null && !doctor.getCity().trim().isEmpty()) {
                subtitle.append(doctor.getCity().trim());
            }
            if (doctor.getCountry() != null && !doctor.getCountry().trim().isEmpty()) {
                if (subtitle.length() > 0) subtitle.append(", ");
                subtitle.append(doctor.getCountry().trim());
            }

            String subtitleText = subtitle.toString();
            textSubtitle.setText(subtitleText);

            // Accessibility: announce full title + location
            StringBuilder contentDesc = new StringBuilder();
            if (titleText != null && !titleText.isEmpty()) {
                contentDesc.append(titleText);
            }
            if (!subtitleText.isEmpty()) {
                if (contentDesc.length() > 0) contentDesc.append(", ");
                contentDesc.append(subtitleText);
            }
            itemView.setContentDescription(contentDesc.toString());
        }
    }
}
