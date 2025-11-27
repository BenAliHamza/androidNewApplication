package tn.esprit.presentation.appointment;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import tn.esprit.R;
import tn.esprit.data.appointment.AppointmentRepository;
import tn.esprit.domain.appointment.Appointment;

/**
 * ViewModel for the patient "My appointments" screen.
 */
public class PatientAppointmentsViewModel extends AndroidViewModel {

    private final AppointmentRepository repository;

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<List<Appointment>> appointments = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> actionMessage = new MutableLiveData<>();

    public PatientAppointmentsViewModel(@NonNull Application application) {
        super(application);
        repository = new AppointmentRepository(application.getApplicationContext());
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<List<Appointment>> getAppointments() {
        return appointments;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<String> getActionMessage() {
        return actionMessage;
    }

    public void clearError() {
        errorMessage.setValue(null);
    }

    /**
     * Clear both error and action messages.
     */
    public void clearMessages() {
        errorMessage.setValue(null);
        actionMessage.setValue(null);
    }

    /**
     * Load current patient's appointments (optionally in a date range).
     * For now we pass null range -> backend decides.
     */
    public void loadAppointments() {
        loading.setValue(true);

        repository.getMyAppointments(
                null,
                null,
                new AppointmentRepository.LoadAppointmentsCallback() {
                    @Override
                    public void onSuccess(@NonNull List<Appointment> list) {
                        loading.postValue(false);
                        appointments.postValue(sortByStart(list));
                    }

                    @Override
                    public void onError(@Nullable Throwable throwable,
                                        @Nullable Integer httpCode,
                                        @Nullable String errorBody) {
                        loading.postValue(false);
                        String msg = getApplication().getString(R.string.patient_appointments_error_generic);
                        errorMessage.postValue(msg);
                    }
                }
        );
    }

    /**
     * Patient cancels an appointment.
     * - Only allowed for future appointments (not in the past).
     */
    public void cancelAppointment(@NonNull Appointment appointment) {
        Long id = appointment.getId();
        if (id == null || id <= 0L) {
            return;
        }

        if (isInPast(appointment)) {
            actionMessage.setValue("You cannot cancel a past appointment.");
            return;
        }

        loading.setValue(true);

        repository.cancelAppointment(id, new AppointmentRepository.CancelAppointmentCallback() {
            @Override
            public void onSuccess() {
                loading.postValue(false);
                actionMessage.postValue("Appointment cancelled.");
                // Reload list so UI stays in sync
                loadAppointments();
            }

            @Override
            public void onError(@Nullable Throwable throwable,
                                @Nullable Integer httpCode,
                                @Nullable String errorBody) {
                loading.postValue(false);
                actionMessage.postValue("Failed to cancel appointment.");
            }
        });
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private List<Appointment> sortByStart(List<Appointment> list) {
        if (list == null) return Collections.emptyList();
        List<Appointment> copy = new ArrayList<>(list);
        // ISO-8601 date strings are lexicographically sortable
        Collections.sort(copy, new Comparator<Appointment>() {
            @Override
            public int compare(Appointment o1, Appointment o2) {
                String d1 = o1 != null && o1.getStartAt() != null ? o1.getStartAt() : "";
                String d2 = o2 != null && o2.getStartAt() != null ? o2.getStartAt() : "";
                return d1.compareTo(d2);
            }
        });
        return copy;
    }

    private boolean isInPast(@NonNull Appointment appointment) {
        Date start = parseDate(appointment.getStartAt());
        if (start == null) return false;
        Date now = new Date();
        return start.before(now);
    }

    @Nullable
    private Date parseDate(@Nullable String iso) {
        if (iso == null || iso.trim().isEmpty()) return null;

        String[] patterns = new String[]{
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss.SSS"
        };

        for (String pattern : patterns) {
            try {
                SimpleDateFormat parser = new SimpleDateFormat(pattern, Locale.getDefault());
                return parser.parse(iso);
            } catch (ParseException ignore) {
            }
        }
        return null;
    }
}
