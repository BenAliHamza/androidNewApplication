package tn.esprit.presentation.medication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import tn.esprit.R;
import tn.esprit.domain.medication.Medication;

/**
 * Auto-complete adapter for medication search.
 *
 * - Filters on Medication.getDisplayName() (name or code).
 * - Shows name and, if available, code in the dropdown row.
 */
public class MedicationAutoCompleteAdapter
        extends ArrayAdapter<Medication>
        implements Filterable {

    private final List<Medication> allItems;
    private final List<Medication> filteredItems;

    public MedicationAutoCompleteAdapter(
            @NonNull Context context,
            @NonNull List<Medication> items
    ) {
        super(context, 0, new ArrayList<>(items));
        this.allItems = new ArrayList<>(items);
        this.filteredItems = new ArrayList<>(items);
    }

    @Override
    public int getCount() {
        return filteredItems.size();
    }

    @Nullable
    @Override
    public Medication getItem(int position) {
        if (position < 0 || position >= filteredItems.size()) return null;
        return filteredItems.get(position);
    }

    @NonNull
    @Override
    public View getView(int position,
                        @Nullable View convertView,
                        @NonNull ViewGroup parent) {
        return createItemView(position, convertView, parent);
    }

    @NonNull
    @Override
    public View getDropDownView(int position,
                                @Nullable View convertView,
                                @NonNull ViewGroup parent) {
        return createItemView(position, convertView, parent);
    }

    private View createItemView(int position,
                                @Nullable View convertView,
                                @NonNull ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_medication_autocomplete, parent, false);
        }

        TextView textName = view.findViewById(R.id.text_medication_name);
        TextView textCode = view.findViewById(R.id.text_medication_code);

        Medication item = getItem(position);
        if (item == null) {
            textName.setText("");
            textCode.setText("");
            textCode.setVisibility(View.GONE);
            return view;
        }

        String displayName = item.getDisplayName();
        textName.setText(displayName);

        String code = item.getCode();
        if (code != null && !code.trim().isEmpty() && !code.trim().equalsIgnoreCase(displayName)) {
            textCode.setVisibility(View.VISIBLE);
            textCode.setText(code.trim());
        } else {
            textCode.setVisibility(View.GONE);
        }

        return view;
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return medicationFilter;
    }

    private final Filter medicationFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<Medication> suggestions = new ArrayList<>();

            if (constraint == null || constraint.length() == 0) {
                suggestions.addAll(allItems);
            } else {
                String pattern = constraint.toString().toLowerCase().trim();
                for (Medication med : allItems) {
                    if (med == null) continue;
                    String name = med.getDisplayName().toLowerCase();
                    if (name.contains(pattern)) {
                        suggestions.add(med);
                    }
                }
            }

            FilterResults results = new FilterResults();
            results.values = suggestions;
            results.count = suggestions.size();
            return results;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void publishResults(CharSequence constraint, FilterResults results) {
            filteredItems.clear();
            if (results != null && results.values instanceof List) {
                filteredItems.addAll((List<Medication>) results.values);
            }
            notifyDataSetChanged();
        }

        @Override
        public CharSequence convertResultToString(Object resultValue) {
            if (resultValue instanceof Medication) {
                return ((Medication) resultValue).getDisplayName();
            }
            return super.convertResultToString(resultValue);
        }
    };
}
