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

    // inside DoctorCalendarFragment

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
        String endTime   = String.format(Locale.US, "%02d:%02d", eh, em);

        boolean weekly = cbWeekly != null && cbWeekly.isChecked();
        List<String> selectedDays = collectSelectedDays();

        // Today (fallback)
        Calendar todayCal = Calendar.getInstance();
        String todayStr = formatDate(
                todayCal.get(Calendar.YEAR),
                todayCal.get(Calendar.MONTH),
                todayCal.get(Calendar.DAY_OF_MONTH)
        );

        // 1) WEEKLY ON + 1+ days → repeat weekly for the coming weeks (rule 1)
        if (weekly && !selectedDays.isEmpty()) {

            // Start from nearest next selected weekday
            Calendar startCal = getNearestNextDate(selectedDays);
            String startDateStr = formatDate(
                    startCal.get(Calendar.YEAR),
                    startCal.get(Calendar.MONTH),
                    startCal.get(Calendar.DAY_OF_MONTH)
            );

            Calendar endCal = (Calendar) startCal.clone();
            endCal.add(Calendar.DAY_OF_MONTH, 30); // ~4 weeks
            String endDateStr = formatDate(
                    endCal.get(Calendar.YEAR),
                    endCal.get(Calendar.MONTH),
                    endCal.get(Calendar.DAY_OF_MONTH)
            );

            AvailabilitySessionRequest req = new AvailabilitySessionRequest();
            req.setStartTime(startTime);
            req.setEndTime(endTime);
            req.setSlotDurationMinutes(duration);
            req.setStartDate(startDateStr);
            req.setEndDate(endDateStr);
            req.setRecurrenceType("WEEKLY");
            req.setDaysOfWeek(selectedDays);

            viewModel.createAvailability(token, req);
            return;
        }

        // 2) WEEKLY OFF + 1+ days → each selected day = ONE_TIME at its next occurrence (rules 2 & 3)
        if (!weekly && !selectedDays.isEmpty()) {

            for (String dayName : selectedDays) {
                Calendar next = getNextDateForDayName(dayName);
                String dateStr = formatDate(
                        next.get(Calendar.YEAR),
                        next.get(Calendar.MONTH),
                        next.get(Calendar.DAY_OF_MONTH)
                );

                AvailabilitySessionRequest req = new AvailabilitySessionRequest();
                req.setStartTime(startTime);
                req.setEndTime(endTime);
                req.setSlotDurationMinutes(duration);
                req.setStartDate(dateStr);
                req.setEndDate(dateStr);
                req.setRecurrenceType("ONE_TIME");
                req.setDaysOfWeek(null);

                // One request per selected weekday
                viewModel.createAvailability(token, req);
            }

            return;
        }

        // 3) No weekly, no day checkbox → ONE_TIME on selected calendar date (or today)
        String selectedDate = (currentDateStr != null) ? currentDateStr : todayStr;

        AvailabilitySessionRequest req = new AvailabilitySessionRequest();
        req.setStartTime(startTime);
        req.setEndTime(endTime);
        req.setSlotDurationMinutes(duration);
        req.setStartDate(selectedDate);
        req.setEndDate(selectedDate);
        req.setRecurrenceType("ONE_TIME");
        req.setDaysOfWeek(null);

        viewModel.createAvailability(token, req);
    }



    /**
     * Return the nearest (>= today) Calendar date among the selected weekday names.
     * Example selectedDays: ["FRIDAY", "MONDAY"]
     */
    private Calendar getNearestNextDate(List<String> selectedDays) {
        Calendar best = null;
        for (String dayName : selectedDays) {
            Calendar candidate = getNextDateForDayName(dayName);
            if (candidate == null) continue;
            if (best == null || candidate.before(best)) {
                best = candidate;
            }
        }
        // Fallback to today if something goes wrong
        if (best == null) {
            best = Calendar.getInstance();
        }
        return best;
    }

    /**
     * Given a day name like "MONDAY", returns the next date (>= today) with that weekday.
     */
    private Calendar getNextDateForDayName(String dayName) {
        int targetDow = mapDayNameToCalendar(dayName);
        if (targetDow == -1) return null;

        Calendar cal = Calendar.getInstance();
        int todayDow = cal.get(Calendar.DAY_OF_WEEK);

        int diff = targetDow - todayDow;
        if (diff < 0) diff += 7; // wrap to next week if needed

        // diff == 0 means "today" is the selected weekday
        cal.add(Calendar.DAY_OF_MONTH, diff);
        return cal;
    }

    /**
     * Map "MONDAY" -> Calendar.MONDAY, etc.
     */
    private int mapDayNameToCalendar(String dayName) {
        if (dayName == null) return -1;
        switch (dayName) {
            case "MONDAY":    return Calendar.MONDAY;
            case "TUESDAY":   return Calendar.TUESDAY;
            case "WEDNESDAY": return Calendar.WEDNESDAY;
            case "THURSDAY":  return Calendar.THURSDAY;
            case "FRIDAY":    return Calendar.FRIDAY;
            case "SATURDAY":  return Calendar.SATURDAY;
            case "SUNDAY":    return Calendar.SUNDAY;
            default:          return -1;
        }
    }


}
