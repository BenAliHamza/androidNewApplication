package tn.esprit.presentation.home;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import tn.esprit.R;
import tn.esprit.data.appointment.AppointmentRepository;
import tn.esprit.domain.appointment.AppointmentCreateRequest;
import tn.esprit.domain.appointment.DailySchedule;
import tn.esprit.domain.appointment.Slot;
import tn.esprit.domain.appointment.WeeklyCalendarResponse;

/**
 * Bottom sheet for booking an appointment with a doctor.
 *
 * Uses backend weekly-calendar:
 *   GET /api/doctors/{doctorId}/weekly-calendar
 *
 * Patient rules:
 *   - Can only book in the future (no past days/times).
 *   - Can navigate between "This week" and "Next week".
 *   - On successful booking, navigates to patient appointments screen.
 */
public class BookAppointmentBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_DOCTOR_ID = "arg_doctor_id";
    private static final String ARG_DOCTOR_NAME = "arg_doctor_name";
    private static final String ARG_DOCTOR_SPECIALTY = "arg_doctor_specialty";
    private static final String ARG_TELE_ENABLED = "arg_tele_enabled";

    // Helper used from DoctorPublicProfileFragment
    public static void show(@NonNull FragmentManager fm,
                            long doctorId,
                            @NonNull String doctorName,
                            @Nullable String specialty,
                            boolean teleEnabled) {
        BookAppointmentBottomSheet sheet = new BookAppointmentBottomSheet();
        Bundle args = new Bundle();
        args.putLong(ARG_DOCTOR_ID, doctorId);
        args.putString(ARG_DOCTOR_NAME, doctorName);
        args.putString(ARG_DOCTOR_SPECIALTY, specialty);
        args.putBoolean(ARG_TELE_ENABLED, teleEnabled);
        sheet.setArguments(args);
        sheet.show(fm, "BookAppointmentBottomSheet");
    }

    // Data
    private long doctorId = -1L;
    @Nullable
    private String doctorName;
    @Nullable
    private String doctorSpecialty;
    private boolean teleEnabledByDoctor = false;

    private AppointmentRepository appointmentRepository;

    // UI
    private TextView textDoctorName;
    private TextView textDoctorSpecialty;

    // Week toggle: "This week" / "Next week"
    private TextView chipWeekCurrent;
    private TextView chipWeekNext;
    private boolean showingNextWeek = false;

    private TabLayout tabsDays;
    private RecyclerView recyclerSlots;
    private TextView textSlotsEmpty;
    private ProgressBar progressCalendar;

    private TextInputEditText inputReason;
    private SwitchMaterial switchTeleconsultation;

    private Button buttonConfirm;
    private ProgressBar progressConfirm;

    private SlotAdapter slotAdapter;

    // Calendar data
    @Nullable
    private WeeklyCalendarResponse weeklyCalendar;
    private final List<DailySchedule> days = new ArrayList<>();

    @Nullable
    private DailySchedule selectedDay;
    @Nullable
    private Slot selectedSlot;

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_book_appointment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        appointmentRepository = new AppointmentRepository(requireContext());

        // Read arguments
        Bundle args = getArguments();
        if (args != null) {
            doctorId = args.getLong(ARG_DOCTOR_ID, -1L);
            doctorName = args.getString(ARG_DOCTOR_NAME);
            doctorSpecialty = args.getString(ARG_DOCTOR_SPECIALTY);
            teleEnabledByDoctor = args.getBoolean(ARG_TELE_ENABLED, false);
        }

        // Header views
        textDoctorName = view.findViewById(R.id.text_doctor_name);
        textDoctorSpecialty = view.findViewById(R.id.text_doctor_specialty);
        ImageButton buttonClose = view.findViewById(R.id.button_close);

        if (!TextUtils.isEmpty(doctorName)) {
            textDoctorName.setText(doctorName);
        }
        if (!TextUtils.isEmpty(doctorSpecialty)) {
            textDoctorSpecialty.setVisibility(View.VISIBLE);
            textDoctorSpecialty.setText(doctorSpecialty);
        } else {
            textDoctorSpecialty.setVisibility(View.GONE);
        }

        if (buttonClose != null) {
            buttonClose.setOnClickListener(v -> dismissAllowingStateLoss());
        }

        // Week toggle
        chipWeekCurrent = view.findViewById(R.id.chip_week_current);
        chipWeekNext = view.findViewById(R.id.chip_week_next);

        if (chipWeekCurrent != null) {
            chipWeekCurrent.setOnClickListener(v -> {
                if (showingNextWeek) {
                    showingNextWeek = false;
                    updateWeekToggleUi();
                    loadWeeklyCalendar();
                }
            });
        }

        if (chipWeekNext != null) {
            chipWeekNext.setOnClickListener(v -> {
                if (!showingNextWeek) {
                    showingNextWeek = true;
                    updateWeekToggleUi();
                    loadWeeklyCalendar();
                }
            });
        }

        updateWeekToggleUi();

        // Calendar / slots UI
        tabsDays = view.findViewById(R.id.tabs_days);
        recyclerSlots = view.findViewById(R.id.recycler_slots);
        textSlotsEmpty = view.findViewById(R.id.text_slots_empty);
        progressCalendar = view.findViewById(R.id.progress_calendar);

        // Reason / tele / confirm
        inputReason = view.findViewById(R.id.input_reason);
        switchTeleconsultation = view.findViewById(R.id.switch_teleconsultation);
        buttonConfirm = view.findViewById(R.id.button_confirm);
        progressConfirm = view.findViewById(R.id.progress_confirm);

        // Tele switch initial state: only meaningful if doctor supports teleconsultation
        if (switchTeleconsultation != null) {
            switchTeleconsultation.setChecked(teleEnabledByDoctor);
            switchTeleconsultation.setEnabled(teleEnabledByDoctor);
        }

        // Slots grid: 3 columns looks nice
        slotAdapter = new SlotAdapter(slot -> {
            selectedSlot = slot;
            updateConfirmEnabled();
        });
        if (recyclerSlots != null) {
            recyclerSlots.setLayoutManager(new GridLayoutManager(requireContext(), 3));
            recyclerSlots.setAdapter(slotAdapter);
        }

        // Day tabs selection
        if (tabsDays != null) {
            tabsDays.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    int position = tab.getPosition();
                    onDayTabSelected(position);
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {
                    // no-op
                }

                @Override
                public void onTabReselected(TabLayout.Tab tab) {
                    int position = tab.getPosition();
                    onDayTabSelected(position);
                }
            });
        }

        // Confirm button
        if (buttonConfirm != null) {
            buttonConfirm.setOnClickListener(v -> onConfirmClicked());
        }

        updateConfirmEnabled();

        // Load weekly calendar from backend
        if (doctorId > 0L) {
            loadWeeklyCalendar();
        } else {
            Toast.makeText(
                    requireContext(),
                    getString(R.string.doctor_public_error_missing_id),
                    Toast.LENGTH_SHORT
            ).show();
            dismissAllowingStateLoss();
        }
    }

    // -------------------------------------------------------------------------
    // Weekly calendar loading
    // -------------------------------------------------------------------------

    private void loadWeeklyCalendar() {
        showCalendarLoading(true);
        days.clear();
        selectedDay = null;
        selectedSlot = null;
        updateConfirmEnabled();

        String weekStartIso = null;
        if (showingNextWeek) {
            weekStartIso = computeNextWeekStartIso();
        }

        appointmentRepository.getDoctorWeeklyCalendar(
                doctorId,
                weekStartIso,
                new AppointmentRepository.WeeklyCalendarCallback() {
                    @Override
                    public void onSuccess(@NonNull WeeklyCalendarResponse calendarResponse) {
                        if (!isAdded()) return;
                        weeklyCalendar = calendarResponse;
                        applyWeeklyCalendar(calendarResponse);
                    }

                    @Override
                    public void onError(@Nullable Throwable throwable,
                                        @Nullable Integer httpCode,
                                        @Nullable String errorBody) {
                        if (!isAdded()) return;
                        showCalendarLoading(false);
                        showSlotsEmpty(true);
                        Toast.makeText(
                                requireContext(),
                                getString(R.string.doctor_book_error_calendar_generic),
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );
    }

    private void applyWeeklyCalendar(@NonNull WeeklyCalendarResponse calendarResponse) {
        days.clear();

        // Keep all days; we filter past slots per-slot, not per-day.
        if (calendarResponse.getDays() != null) {
            for (DailySchedule day : calendarResponse.getDays()) {
                if (day == null) continue;
                days.add(day);
            }
        }

        if (tabsDays != null) {
            tabsDays.removeAllTabs();
        }

        if (days.isEmpty()) {
            showCalendarLoading(false);
            showSlotsEmpty(true);
            slotAdapter.submitList(new ArrayList<>());
            return;
        }

        // Build tabs: one per day (24/11, 25/11, ...)
        for (DailySchedule day : days) {
            String date = day.getDate(); // "yyyy-MM-dd"
            String label = formatDayLabel(date);
            if (tabsDays != null) {
                TabLayout.Tab tab = tabsDays.newTab().setText(label);
                tabsDays.addTab(tab, false);
            }
        }

        showCalendarLoading(false);

        // Select first day by default
        if (tabsDays != null && tabsDays.getTabCount() > 0) {
            TabLayout.Tab first = tabsDays.getTabAt(0);
            if (first != null) {
                first.select();
            }
        }
    }

    private void onDayTabSelected(int position) {
        if (position < 0 || position >= days.size()) {
            return;
        }
        selectedDay = days.get(position);
        selectedSlot = null;
        bindSlotsForSelectedDay();
    }

    private void bindSlotsForSelectedDay() {
        if (selectedDay == null) {
            showSlotsEmpty(true);
            slotAdapter.submitList(new ArrayList<>());
            updateConfirmEnabled();
            return;
        }

        List<Slot> allSlots = selectedDay.getSlots();
        List<Slot> availableSlots = new ArrayList<>();
        if (allSlots != null) {
            for (Slot s : allSlots) {
                if (s == null) continue;
                if (!s.isAvailable()) continue;

                String dateIso = selectedDay.getDate();
                String time = s.getTime();

                // Filter out past slots (including past days and times earlier today)
                if (isSlotInPast(dateIso, time)) {
                    continue;
                }

                availableSlots.add(s);
            }
        }

        if (availableSlots.isEmpty()) {
            showSlotsEmpty(true);
            slotAdapter.submitList(new ArrayList<>());
        } else {
            showSlotsEmpty(false);
            slotAdapter.submitList(availableSlots);
        }

        selectedSlot = null;
        updateConfirmEnabled();
    }

    private void showCalendarLoading(boolean loading) {
        if (progressCalendar != null) {
            progressCalendar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
        if (recyclerSlots != null) {
            recyclerSlots.setVisibility(loading ? View.INVISIBLE : View.VISIBLE);
        }
    }

    private void showSlotsEmpty(boolean empty) {
        if (textSlotsEmpty != null) {
            textSlotsEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        }
        if (recyclerSlots != null) {
            recyclerSlots.setVisibility(empty ? View.GONE : View.VISIBLE);
        }
    }

    // -------------------------------------------------------------------------
    // Confirm
    // -------------------------------------------------------------------------

    private void updateConfirmEnabled() {
        if (buttonConfirm == null) return;
        boolean enabled = (selectedDay != null && selectedSlot != null);
        buttonConfirm.setEnabled(enabled);
        buttonConfirm.setAlpha(enabled ? 1f : 0.5f);
    }

    private void onConfirmClicked() {
        if (!isAdded()) return;

        if (selectedDay == null || selectedSlot == null) {
            Toast.makeText(
                    requireContext(),
                    getString(R.string.doctor_book_select_slot_first),
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        String dateIso = selectedDay.getDate(); // "yyyy-MM-dd"
        String time = selectedSlot.getTime();   // "HH:mm"

        if (TextUtils.isEmpty(dateIso) || TextUtils.isEmpty(time)) {
            Toast.makeText(
                    requireContext(),
                    getString(R.string.doctor_book_error_create_generic),
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        // Final safety: no booking in the past
        if (isSlotInPast(dateIso, time)) {
            Toast.makeText(
                    requireContext(),
                    getString(R.string.doctor_book_error_create_generic),
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        String startAt = dateIso + "T" + time + ":00";
        String endAt = computeEndTimeIso(dateIso, time);

        String reason = null;
        if (inputReason != null) {
            CharSequence cs = inputReason.getText();
            if (cs != null && cs.toString().trim().length() > 0) {
                reason = cs.toString().trim();
            }
        }

        boolean tele = switchTeleconsultation != null
                && switchTeleconsultation.isChecked()
                && teleEnabledByDoctor;

        AppointmentCreateRequest request = new AppointmentCreateRequest();
        request.setDoctorId(doctorId);
        request.setStartAt(startAt);
        request.setEndAt(endAt);
        request.setReason(reason);
        request.setTeleconsultation(tele);

        setConfirmLoading(true);

        appointmentRepository.createAppointment(
                request,
                new AppointmentRepository.CreateAppointmentCallback() {
                    @Override
                    public void onSuccess(@NonNull tn.esprit.domain.appointment.Appointment appointment) {
                        if (!isAdded()) return;
                        setConfirmLoading(false);
                        Toast.makeText(
                                requireContext(),
                                getString(R.string.doctor_book_success),
                                Toast.LENGTH_SHORT
                        ).show();
                        navigateToPatientAppointments();
                    }

                    @Override
                    public void onError(@Nullable Throwable throwable,
                                        @Nullable Integer httpCode,
                                        @Nullable String errorBody) {
                        if (!isAdded()) return;
                        setConfirmLoading(false);
                        Toast.makeText(
                                requireContext(),
                                getString(R.string.doctor_book_error_create_generic),
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );
    }

    private void setConfirmLoading(boolean loading) {
        if (buttonConfirm != null) {
            buttonConfirm.setEnabled(!loading);
        }
        if (progressConfirm != null) {
            progressConfirm.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * Navigate to the patient appointments screen after a successful booking.
     * Uses the parent fragment's NavController (DoctorPublicProfileFragment).
     */
    private void navigateToPatientAppointments() {
        if (!isAdded()) {
            dismissAllowingStateLoss();
            return;
        }

        try {
            Fragment parent = getParentFragment();
            if (parent != null) {
                NavController navController = NavHostFragment.findNavController(parent);
                navController.navigate(R.id.patientAppointmentsFragment);
            }
        } catch (Exception ignored) {
            // If navigation fails for any reason, just fall back to dismissing.
        }

        dismissAllowingStateLoss();
    }

    // -------------------------------------------------------------------------
    // Utility
    // -------------------------------------------------------------------------

    /**
     * Very small helper to compute end time as +30 minutes from start.
     * (Matches backend slot size assumption.)
     */
    @NonNull
    private String computeEndTimeIso(@NonNull String dateIso, @NonNull String startTime) {
        try {
            String[] parts = startTime.split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);
            minute += 30;
            if (minute >= 60) {
                minute -= 60;
                hour += 1;
            }
            String endTime = String.format(Locale.US, "%02d:%02d", hour, minute);
            return dateIso + "T" + endTime + ":00";
        } catch (Exception e) {
            // Fallback: just reuse start time if parsing fails
            return dateIso + "T" + startTime + ":00";
        }
    }

    /**
     * Builds a short label like "24/11" from "yyyy-MM-dd".
     */
    @NonNull
    private String formatDayLabel(@Nullable String isoDate) {
        if (isoDate == null || isoDate.length() < 10) {
            return isoDate != null ? isoDate : "";
        }
        String day = isoDate.substring(8, 10);
        String month = isoDate.substring(5, 7);
        return day + "/" + month;
    }

    /**
     * Returns true if the given day/time is strictly before "now".
     * Handles both past days and past times on today.
     */
    private boolean isSlotInPast(@Nullable String dateIso, @Nullable String timeHHmm) {
        if (TextUtils.isEmpty(dateIso) || TextUtils.isEmpty(timeHHmm)) {
            return false;
        }
        try {
            String iso = dateIso + "T" + timeHHmm + ":00";
            SimpleDateFormat parser =
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            Date slotDate = parser.parse(iso);
            if (slotDate == null) return false;
            Date now = new Date();
            return slotDate.before(now);
        } catch (ParseException e) {
            return false;
        }
    }

    /**
     * Compute the Monday of next week in "yyyy-MM-dd" (device local calendar).
     */
    @Nullable
    private String computeNextWeekStartIso() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        // Set to Monday of this week, then add 1 week
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        cal.add(Calendar.WEEK_OF_YEAR, 1);

        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return fmt.format(cal.getTime());
    }

    private void updateWeekToggleUi() {
        if (chipWeekCurrent != null) {
            chipWeekCurrent.setAlpha(showingNextWeek ? 0.6f : 1f);
        }
        if (chipWeekNext != null) {
            chipWeekNext.setAlpha(showingNextWeek ? 1f : 0.6f);
        }
    }
}
