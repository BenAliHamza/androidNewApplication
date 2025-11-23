package tn.esprit.presentation.appointments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import tn.esprit.R;
import tn.esprit.data.remote.appointment.AppointmentCreateRequest;

public class PatientAppointmentFragment extends Fragment {

    private CalendarView calendarView;
    private ListView lvTimeSlots;
    private Button btnConfirm;

    private AppointmentViewModel viewModel;

    private String selectedDate = null;
    private String selectedTime = null;

    // TODO: remplacer plus tard par des créneaux venant du backend
    private final String[] slots = {
            "09:00", "10:00", "11:00", "14:00", "15:00", "16:00"
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_patient_appointment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        calendarView = view.findViewById(R.id.calendarView);
        lvTimeSlots = view.findViewById(R.id.lvTimeSlots);
        btnConfirm = view.findViewById(R.id.btnConfirmAppointment);

        viewModel = new ViewModelProvider(this).get(AppointmentViewModel.class);
        viewModel.init(requireContext());

        // Liste des heures
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                slots
        );
        lvTimeSlots.setAdapter(adapter);

        lvTimeSlots.setOnItemClickListener((parent, v1, position, id) -> {
            selectedTime = slots[position];
            Toast.makeText(requireContext(),
                    "Heure : " + selectedTime, Toast.LENGTH_SHORT).show();
        });

        // Date depuis le calendrier
        calendarView.setOnDateChangeListener((view1, year, month, day) -> {
            // mois 0-based → +1
            selectedDate = String.format(Locale.US, "%04d-%02d-%02d",
                    year, month + 1, day);
            Toast.makeText(requireContext(),
                    "Date : " + selectedDate, Toast.LENGTH_SHORT).show();
        });

        btnConfirm.setOnClickListener(v -> {
            if (selectedDate == null || selectedTime == null) {
                Toast.makeText(requireContext(),
                        "Choisissez une date et une heure",
                        Toast.LENGTH_LONG).show();
                return;
            }

            AppointmentCreateRequest req = new AppointmentCreateRequest();
            // ⚠️ à remplacer plus tard par le vrai doctorUserId passé via arguments
            req.doctorUserId = 2L;
            req.date = selectedDate;
            req.startTime = selectedTime;
            req.endTime = add30Minutes(selectedTime);
            req.reason = "Consultation";

            viewModel.requestAppointment(req);
        });

        viewModel.getAppointmentActionResult()
                .observe(getViewLifecycleOwner(), result -> {
                    if (result != null) {
                        Toast.makeText(requireContext(),
                                "Rendez-vous envoyé 👍",
                                Toast.LENGTH_LONG).show();
                    }
                });

        viewModel.getError()
                .observe(getViewLifecycleOwner(), err -> {
                    if (err != null) {
                        Toast.makeText(requireContext(),
                                "Erreur : " + err,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private String add30Minutes(String start) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.US);
            Date d = sdf.parse(start);
            Calendar c = Calendar.getInstance();
            c.setTime(d);
            c.add(Calendar.MINUTE, 30);
            return sdf.format(c.getTime());
        } catch (Exception e) {
            return start;
        }
    }
}
