package tn.esprit.presentation.appointment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import tn.esprit.R;
import tn.esprit.data.auth.AuthLocalDataSource;
import tn.esprit.data.remote.appointment.AvailabilitySessionRequest;
import tn.esprit.domain.appointment.Slot;
import tn.esprit.domain.auth.AuthTokens;

public class DoctorCalendarFragment extends Fragment {

    private AppointmentViewModel viewModel;
    private DoctorSlotAdapter adapter;
    private TextView textSelectedDate;
    private CalendarView calendarView;
    private String currentDateStr;
    private List<Slot> allSlots = new ArrayList<>();
    private LinearLayout layoutAgenda;
    private View layoutAvailability;
    private NumberPicker npStartHour, npStartMinute, npEndHour, npEndMinute, npDuration;
    private CheckBox cbMon, cbTue, cbWed, cbThu, cbFri, cbSat, cbSun;
    private CheckBox cbWeekly;
    private AuthLocalDataSource authLocalDataSource;

    public DoctorCalendarFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_doctor_calendar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authLocalDataSource = new AuthLocalDataSource(requireContext());

        Button btnAvailabilitySetup = view.findViewById(R.id.btnAvailabilitySetup);
        Button btnAgendaView = view.findViewById(R.id.btnAgendaView);

        layoutAvailability = view.findViewById(R.id.layoutAvailabilitySetup);
        layoutAgenda = view.findViewById(R.id.layoutAgenda);

        npStartHour = view.findViewById(R.id.npStartHour);
        npStartMinute = view.findViewById(R.id.npStartMinute);
        npEndHour = view.findViewById(R.id.npEndHour);
        npEndMinute = view.findViewById(R.id.npEndMinute);
        npDuration = view.findViewById(R.id.npDuration);
        Button btnSaveAvailability = view.findViewById(R.id.btnSaveAvailability);

        cbMon = view.findViewById(R.id.cbMon);
        cbTue = view.findViewById(R.id.cbTue);
        cbWed = view.findViewById(R.id.cbWed);
        cbThu = view.findViewById(R.id.cbThu);
        cbFri = view.findViewById(R.id.cbFri);
        cbSat = view.findViewById(R.id.cbSat);
        cbSun = view.findViewById(R.id.cbSun);
        cbWeekly = view.findViewById(R.id.cbWeekly);

        setupNumberPickers();

        calendarView = view.findViewById(R.id.calendarView);
        textSelectedDate = view.findViewById(R.id.textSelectedDate);
        RecyclerView recyclerView = view.findViewById(R.id.recyclerSlots);

        adapter = new DoctorSlotAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        viewModel = new ViewModelProvider(requireActivity())
                .get(AppointmentViewModel.class);

        viewModel.createdAvailability.observe(getViewLifecycleOwner(), response -> {
            if (response != null) {
                Toast.makeText(requireContext(),
                        "Availability saved. Slots generated: " + response.getGeneratedSlotsCount(),
                        Toast.LENGTH_SHORT).show();
                layoutAvailability.setVisibility(View.GONE);
                layoutAgenda.setVisibility(View.VISIBLE);
                loadSlotsForNextDays(30);
            }
        });

        viewModel.availabilityError.observe(getViewLifecycleOwner(), error -> {
            if (error != null && error) {
                String msg = viewModel.availabilityErrorMessage != null
                        ? viewModel.availabilityErrorMessage.getValue()
                        : null;
                if (msg == null || msg.isEmpty()) {
                    msg = "Failed to save availability";
                }
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
            }
        });

        btnAvailabilitySetup.setOnClickListener(v -> {
            layoutAvailability.setVisibility(View.VISIBLE);
            layoutAgenda.setVisibility(View.GONE);
        });

        btnAgendaView.setOnClickListener(v -> {
            layoutAvailability.setVisibility(View.GONE);
            layoutAgenda.setVisibility(View.VISIBLE);
        });

        btnSaveAvailability.setOnClickListener(v -> saveAvailability());

        Calendar cal = Calendar.getInstance();
        currentDateStr = formatDate(cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH));
        updateSelectedDateLabel();

        calendarView.setOnDateChangeListener((cv, year, month, dayOfMonth) -> {
            currentDateStr = formatDate(year, month, dayOfMonth);
            updateSelectedDateLabel();
            filterSlotsForCurrentDate();
        });

        viewModel.doctorSlots.observe(getViewLifecycleOwner(), slots -> {
            if (slots != null) {
                allSlots = slots;
                filterSlotsForCurrentDate();
            }
        });

        loadSlotsForNextDays(30);
    }

    private void setupNumberPickers() {
        npStartHour.setMinValue(0);
        npStartHour.setMaxValue(23);
        npEndHour.setMinValue(0);
        npEndHour.setMaxValue(23);
        npStartMinute.setMinValue(0);
        npStartMinute.setMaxValue(59);
        npEndMinute.setMinValue(0);
        npEndMinute.setMaxValue(59);
        npStartHour.setValue(9);
        npEndHour.setValue(17);
        npDuration.setMinValue(5);
        npDuration.setMaxValue(120);
        npDuration.setValue(30);
    }

    private String formatDate(int year, int monthZeroBased, int dayOfMonth) {
        int monthOneBased = monthZeroBased + 1;
        return String.format(Locale.US, "%04d-%02d-%02d", year, monthOneBased, dayOfMonth);
    }

    private void updateSelectedDateLabel() {
        if (textSelectedDate != null && currentDateStr != null) {
            textSelectedDate.setText("Slots for " + currentDateStr);
        }
    }

    private void filterSlotsForCurrentDate() {
        if (allSlots == null || currentDateStr == null) {
            adapter.setSlots(new ArrayList<>());
            return;
        }

        List<Slot> filtered = new ArrayList<>();
        for (Slot s : allSlots) {
            if (s.startDateTime != null && s.startDateTime.length() >= 10) {
                String dateOnly = s.startDateTime.substring(0, 10);
                if (dateOnly.equals(currentDateStr)) {
                    filtered.add(s);
                }
            }
        }

        adapter.setSlots(filtered);
    }

    private String getBearerToken() {
        AuthTokens tokens = authLocalDataSource.getTokens();
        if (tokens == null || tokens.getAccessToken() == null) {
            return null;
        }
        String type = tokens.getTokenType();
        if (type == null || type.isEmpty()) {
            type = "Bearer";
        }
        return type + " " + tokens.getAccessToken();
    }

    private void loadSlotsForNextDays(int days) {
        String token = getBearerToken();
        if (token == null) {
            Toast.makeText(requireContext(),
                    "Missing access token (please login again)",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        Calendar cal = Calendar.getInstance();
        String fromStr = formatDate(cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH));

        cal.add(Calendar.DAY_OF_MONTH, days);
        String toStr = formatDate(cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH));

        viewModel.loadDoctorSlots(token, fromStr, toStr, null);
    }

    private List<String> collectSelectedDays() {
        List<String> days = new ArrayList<>();
        if (cbMon != null && cbMon.isChecked()) days.add("MONDAY");
        if (cbTue != null && cbTue.isChecked()) days.add("TUESDAY");
        if (cbWed != null && cbWed.isChecked()) days.add("WEDNESDAY");
        if (cbThu != null && cbThu.isChecked()) days.add("THURSDAY");
        if (cbFri != null && cbFri.isChecked()) days.add("FRIDAY");
        if (cbSat != null && cbSat.isChecked()) days.add("SATURDAY");
        if (cbSun != null && cbSun.isChecked()) days.add("SUNDAY");
        return days;
    }

    private void saveAvailability() {
        String token = getBearerToken();
        if (token == null) {
            Toast.makeText(requireContext(),
                    "Missing access token (please login again)",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        int sh = npStartHour.getValue();
        int sm = npStartMinute.getValue();
        int eh = npEndHour.getValue();
        int em = npEndMinute.getValue();
        int duration = npDuration.getValue();

        String startTime = String.format(Locale.US, "%02d:%02d", sh, sm);
        String endTime = String.format(Locale.US, "%02d:%02d", eh, em);

        AvailabilitySessionRequest req = new AvailabilitySessionRequest();
        req.setStartTime(startTime);
        req.setEndTime(endTime);
        req.setSlotDurationMinutes(duration);

        boolean weekly = cbWeekly != null && cbWeekly.isChecked();

        if (weekly) {
            Calendar cal = Calendar.getInstance();
            String startDate = formatDate(cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH));
            cal.add(Calendar.DAY_OF_MONTH, 30);
            String endDate = formatDate(cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH));

            req.setStartDate(startDate);
            req.setEndDate(endDate);
            req.setRecurrenceType("WEEKLY");
            req.setDaysOfWeek(collectSelectedDays());

        } else {
            Calendar cal = Calendar.getInstance();
            String today = formatDate(cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH));

            req.setStartDate(today);
            req.setEndDate(today);
            req.setRecurrenceType("ONE_TIME");
            req.setDaysOfWeek(null);
        }

        viewModel.createAvailability(token, req);
    }

}
