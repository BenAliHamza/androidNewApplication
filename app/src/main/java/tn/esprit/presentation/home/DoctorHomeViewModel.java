package tn.esprit.presentation.home;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import tn.esprit.R;
import tn.esprit.data.appointment.AppointmentRepository;
import tn.esprit.domain.appointment.Appointment;
import tn.esprit.domain.appointment.DoctorHomeStats;

/**
 * ViewModel backing the doctor home highlight stats card.
 *
 * Uses:
 *   GET /api/doctors/me/appointments?from=&to=
 * to load this week's appointments, then computes:
 *   - today count
 *   - week count
 *   - number of distinct patients this week
 */
public class DoctorHomeViewModel extends AndroidViewModel {

    private final AppointmentRepository repository;

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<DoctorHomeStats> stats =
            new MutableLiveData<>(DoctorHomeStats.empty());
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    private final SimpleDateFormat isoParserNoMillis =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
    private final SimpleDateFormat isoParserWithMillis =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault());

    private final SimpleDateFormat isoFormatter =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());

    public DoctorHomeViewModel(@NonNull Application application) {
        super(application);
        repository = new AppointmentRepository(application.getApplicationContext());
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<DoctorHomeStats> getStats() {
        return stats;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void clearError() {
        errorMessage.setValue(null);
    }

    /**
     * Load current week appointments for the doctor and compute stats.
     */
    public void loadStats() {
        loading.setValue(true);
        errorMessage.setValue(null);

        WeekRange range = computeCurrentWeekRange();

        repository.getDoctorAppointments(
                range.fromIso,
                range.toIso,
                new AppointmentRepository.LoadAppointmentsCallback() {
                    @Override
                    public void onSuccess(@NonNull List<Appointment> list) {
                        computeAndPublishStats(list);
                        loading.postValue(false);
                    }

                    @Override
                    public void onError(@Nullable Throwable throwable,
                                        @Nullable Integer httpCode,
                                        @Nullable String errorBody) {
                        loading.postValue(false);
                        String msg = getApplication().getString(
                                R.string.doctor_appointments_error_generic
                        );
                        errorMessage.postValue(msg);
                    }
                }
        );
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private void computeAndPublishStats(@Nullable List<Appointment> list) {
        if (list == null) {
            stats.postValue(DoctorHomeStats.empty());
            return;
        }

        Date now = new Date();
        int weekCount = list.size();
        int todayCount = 0;

        Set<Long> uniquePatientIds = new HashSet<>();

        for (Appointment appointment : list) {
            if (appointment == null) continue;

            // Today count
            Date start = parseDate(appointment.getStartAt());
            if (start != null && isSameDay(start, now)) {
                todayCount++;
            }

            // Distinct patients
            Long patientUserId = appointment.getPatientUserId();
            if (patientUserId != null && patientUserId > 0L) {
                uniquePatientIds.add(patientUserId);
            }
        }

        int patientsCount = uniquePatientIds.size();

        DoctorHomeStats newStats = new DoctorHomeStats(
                todayCount,
                weekCount,
                patientsCount
        );
        stats.postValue(newStats);
    }

    private WeekRange computeCurrentWeekRange() {
        Calendar now = Calendar.getInstance();

        // Start: Monday of current week at 00:00:00
        Calendar start = (Calendar) now.clone();
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);

        while (start.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            start.add(Calendar.DAY_OF_MONTH, -1);
        }

        // End: Monday of next week at 00:00:00
        Calendar end = (Calendar) start.clone();
        end.add(Calendar.DAY_OF_MONTH, 7);

        String fromIso = isoFormatter.format(start.getTime());
        String toIso = isoFormatter.format(end.getTime());

        return new WeekRange(fromIso, toIso);
    }

    @Nullable
    private Date parseDate(@Nullable String iso) {
        if (iso == null || iso.trim().isEmpty()) return null;

        try {
            return isoParserNoMillis.parse(iso);
        } catch (ParseException ignored) {
        }

        try {
            return isoParserWithMillis.parse(iso);
        } catch (ParseException ignored) {
        }

        return null;
    }

    private boolean isSameDay(@NonNull Date d1, @NonNull Date d2) {
        Calendar c1 = Calendar.getInstance();
        c1.setTime(d1);
        Calendar c2 = Calendar.getInstance();
        c2.setTime(d2);

        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
                && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR);
    }

    private static class WeekRange {
        final String fromIso;
        final String toIso;

        WeekRange(String fromIso, String toIso) {
            this.fromIso = fromIso;
            this.toIso = toIso;
        }
    }
}
