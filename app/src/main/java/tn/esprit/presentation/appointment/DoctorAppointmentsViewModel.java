package tn.esprit.presentation.appointment;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import tn.esprit.R;
import tn.esprit.data.appointment.AppointmentRepository;
import tn.esprit.domain.appointment.Appointment;
import tn.esprit.domain.appointment.AppointmentStatusUpdateRequest;

/**
 * ViewModel for doctor "My schedule" / appointments screen.
 *
 * Holds three lists:
 *  - todayAppointments
 *  - upcomingAppointments
 *  - pastAppointments
 *
 * Also exposes loading + error/action messages.
 */
public class DoctorAppointmentsViewModel extends AndroidViewModel {

    private final AppointmentRepository repository;

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<List<Appointment>> todayAppointments =
            new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<List<Appointment>> upcomingAppointments =
            new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<List<Appointment>> pastAppointments =
            new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> actionMessage = new MutableLiveData<>();

    public DoctorAppointmentsViewModel(@NonNull Application application) {
        super(application);
        repository = new AppointmentRepository(application.getApplicationContext());
    }

    // -------------------------------------------------------------------------
    // LiveData getters
    // -------------------------------------------------------------------------

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<List<Appointment>> getTodayAppointments() {
        return todayAppointments;
    }

    public LiveData<List<Appointment>> getUpcomingAppointments() {
        return upcomingAppointments;
    }

    public LiveData<List<Appointment>> getPastAppointments() {
        return pastAppointments;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<String> getActionMessage() {
        return actionMessage;
    }

    public void clearMessages() {
        errorMessage.setValue(null);
        actionMessage.setValue(null);
    }

    // -------------------------------------------------------------------------
    // Loading appointments
    // -------------------------------------------------------------------------

    /**
     * Load all appointments for current doctor.
     * For now we don't pass date range; backend returns relevant range.
     */
    public void loadAppointments() {
        loading.setValue(true);

        repository.getDoctorAppointments(
                null,
                null,
                new AppointmentRepository.LoadAppointmentsCallback() {
                    @Override
                    public void onSuccess(@NonNull List<Appointment> list) {
                        loading.postValue(false);
                        splitIntoSections(list);
                    }

                    @Override
                    public void onError(@Nullable Throwable throwable,
                                        @Nullable Integer httpCode,
                                        @Nullable String errorBody) {
                        loading.postValue(false);
                        String msg = getApplication()
                                .getString(R.string.doctor_appointments_error_generic);
                        errorMessage.postValue(msg);
                    }
                }
        );
    }

    private void splitIntoSections(@NonNull List<Appointment> list) {
        if (list.isEmpty()) {
            todayAppointments.postValue(Collections.emptyList());
            upcomingAppointments.postValue(Collections.emptyList());
            pastAppointments.postValue(Collections.emptyList());
            return;
        }

        String todayDate = AppointmentUiHelper.getTodayDatePrefix(); // yyyy-MM-dd

        List<Appointment> today = new ArrayList<>();
        List<Appointment> upcoming = new ArrayList<>();
        List<Appointment> past = new ArrayList<>();

        for (Appointment a : list) {
            if (a == null) continue;
            String start = a.getStartAt();
            String datePart = AppointmentUiHelper.safeDatePrefix(start);

            if (datePart == null) {
                // Unknown -> treat as upcoming
                upcoming.add(a);
                continue;
            }

            int cmp = datePart.compareTo(todayDate);
            if (cmp == 0) {
                today.add(a);
            } else if (cmp < 0) {
                past.add(a);
            } else {
                upcoming.add(a);
            }
        }

        todayAppointments.postValue(AppointmentUiHelper.sortByStart(today));
        upcomingAppointments.postValue(AppointmentUiHelper.sortByStart(upcoming));
        pastAppointments.postValue(AppointmentUiHelper.sortByStart(past));
    }

    // -------------------------------------------------------------------------
    // Accept / reject
    // -------------------------------------------------------------------------

    public void acceptAppointment(@NonNull Appointment appointment) {
        changeStatus(appointment, "ACCEPTED",
                getApplication().getString(R.string.doctor_appointments_action_accept_success),
                getApplication().getString(R.string.doctor_appointments_action_accept_error));
    }

    public void rejectAppointment(@NonNull Appointment appointment) {
        changeStatus(appointment, "REJECTED",
                getApplication().getString(R.string.doctor_appointments_action_reject_success),
                getApplication().getString(R.string.doctor_appointments_action_reject_error));
    }

    private void changeStatus(@NonNull Appointment appointment,
                              @NonNull String newStatus,
                              @NonNull String successMessage,
                              @NonNull String errorMessageText) {
        Long id = appointment.getId();
        if (id == null || id <= 0L) {
            return;
        }

        String currentStatusRaw = appointment.getStatus();
        String currentStatus = currentStatusRaw != null
                ? currentStatusRaw.toUpperCase(Locale.getDefault())
                : "";

        // Only allow status change from PENDING in this v1
        if (!"PENDING".equals(currentStatus)) {
            actionMessage.setValue(getApplication()
                    .getString(R.string.doctor_appointments_action_not_pending));
            return;
        }

        loading.setValue(true);

        AppointmentStatusUpdateRequest request = new AppointmentStatusUpdateRequest();
        request.setStatus(newStatus);

        repository.updateAppointmentStatus(
                id,
                request,
                new AppointmentRepository.UpdateStatusCallback() {
                    @Override
                    public void onSuccess(@NonNull Appointment updated) {
                        loading.postValue(false);
                        actionMessage.postValue(successMessage);
                        // Reload lists to ensure all tabs are in sync
                        loadAppointments();
                    }

                    @Override
                    public void onError(@Nullable Throwable throwable,
                                        @Nullable Integer httpCode,
                                        @Nullable String errorBody) {
                        loading.postValue(false);
                        actionMessage.postValue(errorMessageText);
                    }
                }
        );
    }

    // -------------------------------------------------------------------------
    // (Optional) helper if we ever need "is in past" logic here
    // -------------------------------------------------------------------------

    @SuppressWarnings("unused")
    private boolean isInPast(@NonNull Appointment appointment) {
        Date start = AppointmentUiHelper.parseDate(appointment.getStartAt());
        if (start == null) return false;
        Date now = new Date();
        return start.before(now);
    }
}
