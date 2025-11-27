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
import java.util.Locale;

import tn.esprit.data.appointment.AppointmentRepository;
import tn.esprit.domain.appointment.Appointment;
import tn.esprit.domain.appointment.AppointmentCreateRequest;
import tn.esprit.domain.appointment.WeeklyCalendarResponse;

public class BookAppointmentViewModel extends AndroidViewModel {

    private final AppointmentRepository repository;

    private final MutableLiveData<Boolean> loadingCalendar = new MutableLiveData<>(false);
    private final MutableLiveData<WeeklyCalendarResponse> calendar = new MutableLiveData<>();
    private final MutableLiveData<Boolean> bookingInProgress = new MutableLiveData<>(false);
    private final MutableLiveData<Appointment> bookingSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public BookAppointmentViewModel(@NonNull Application application) {
        super(application);
        repository = new AppointmentRepository(application.getApplicationContext());
    }

    public LiveData<Boolean> getLoadingCalendar() {
        return loadingCalendar;
    }

    public LiveData<WeeklyCalendarResponse> getCalendar() {
        return calendar;
    }

    public LiveData<Boolean> getBookingInProgress() {
        return bookingInProgress;
    }

    public LiveData<Appointment> getBookingSuccess() {
        return bookingSuccess;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void loadWeeklyCalendar(long doctorId, @Nullable String weekStartIso) {
        loadingCalendar.setValue(true);
        repository.getDoctorWeeklyCalendar(doctorId, weekStartIso,
                new AppointmentRepository.WeeklyCalendarCallback() {
                    @Override
                    public void onSuccess(@NonNull WeeklyCalendarResponse calendarResponse) {
                        loadingCalendar.postValue(false);
                        calendar.postValue(calendarResponse);
                    }

                    @Override
                    public void onError(@Nullable Throwable throwable,
                                        @Nullable Integer httpCode,
                                        @Nullable String errorBody) {
                        loadingCalendar.postValue(false);
                        errorMessage.postValue(errorBody != null ? errorBody : "error_calendar");
                    }
                });
    }

    /**
     * dateIso: "yyyy-MM-dd"
     * time: "HH:mm"
     */
    public void bookAppointment(long doctorId,
                                @NonNull String dateIso,
                                @NonNull String time,
                                @Nullable String reason,
                                @Nullable Boolean teleconsultation) {

        // startAt: combine date + time
        String startAt = dateIso + "T" + time + ":00";
        // endAt: +30 minutes (simple default)
        String endAt = addMinutes(startAt, 30);

        AppointmentCreateRequest request = new AppointmentCreateRequest();
        request.setDoctorId(doctorId);
        request.setStartAt(startAt);
        request.setEndAt(endAt);
        request.setReason(reason);
        request.setTeleconsultation(teleconsultation);

        bookingInProgress.setValue(true);
        repository.createAppointment(request, new AppointmentRepository.CreateAppointmentCallback() {
            @Override
            public void onSuccess(@NonNull Appointment appointment) {
                bookingInProgress.postValue(false);
                bookingSuccess.postValue(appointment);
            }

            @Override
            public void onError(@Nullable Throwable throwable,
                                @Nullable Integer httpCode,
                                @Nullable String errorBody) {
                bookingInProgress.postValue(false);
                errorMessage.postValue(errorBody != null ? errorBody : "error_create");
            }
        });
    }

    private String addMinutes(@NonNull String startIso, int minutes) {
        SimpleDateFormat parser =
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
        try {
            Date date = parser.parse(startIso);
            if (date == null) return startIso;

            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            cal.add(Calendar.MINUTE, minutes);
            return parser.format(cal.getTime());
        } catch (ParseException e) {
            return startIso;
        }
    }
}
