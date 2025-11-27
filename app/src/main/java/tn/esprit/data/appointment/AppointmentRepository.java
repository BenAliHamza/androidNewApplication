package tn.esprit.data.appointment;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import tn.esprit.data.auth.AuthLocalDataSource;
import tn.esprit.data.remote.ApiClient;
import tn.esprit.data.remote.appointment.AppointmentApiService;
import tn.esprit.data.remote.common.ListResponseDto;
import tn.esprit.domain.appointment.Appointment;
import tn.esprit.domain.appointment.AppointmentCreateRequest;
import tn.esprit.domain.appointment.AppointmentStatusUpdateRequest;
import tn.esprit.domain.appointment.WeeklyCalendarResponse;
import tn.esprit.domain.auth.AuthTokens;
import tn.esprit.domain.doctor.DoctorHomeStats;

/**
 * Repository for appointment operations (patient + doctor).
 */
public class AppointmentRepository {

    private final AuthLocalDataSource authLocalDataSource;
    private final AppointmentApiService appointmentApiService;

    public AppointmentRepository(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        this.authLocalDataSource = new AuthLocalDataSource(appContext);
        this.appointmentApiService = ApiClient.createService(AppointmentApiService.class);
    }

    // ------------------------------------------------------------------------
    // Callbacks
    // ------------------------------------------------------------------------

    public interface LoadAppointmentsCallback {
        void onSuccess(@NonNull List<Appointment> list);

        void onError(@Nullable Throwable throwable,
                     @Nullable Integer httpCode,
                     @Nullable String errorBody);
    }

    public interface CreateAppointmentCallback {
        void onSuccess(@NonNull Appointment appointment);

        void onError(@Nullable Throwable throwable,
                     @Nullable Integer httpCode,
                     @Nullable String errorBody);
    }

    public interface CancelAppointmentCallback {
        void onSuccess();

        void onError(@Nullable Throwable throwable,
                     @Nullable Integer httpCode,
                     @Nullable String errorBody);
    }

    public interface UpdateStatusCallback {
        void onSuccess(@NonNull Appointment updated);

        void onError(@Nullable Throwable throwable,
                     @Nullable Integer httpCode,
                     @Nullable String errorBody);
    }

    public interface RescheduleAppointmentCallback {
        void onSuccess(@NonNull Appointment updated);

        void onError(@Nullable Throwable throwable,
                     @Nullable Integer httpCode,
                     @Nullable String errorBody);
    }

    public interface WeeklyCalendarCallback {
        void onSuccess(@NonNull WeeklyCalendarResponse calendar);

        void onError(@Nullable Throwable throwable,
                     @Nullable Integer httpCode,
                     @Nullable String errorBody);
    }

    // ------------------------------------------------------------------------
    // Patient: My appointments
    // ------------------------------------------------------------------------

    /**
     * Load appointments for the current patient.
     *
     * @param fromIso ISO-8601 date-time string or null
     * @param toIso   ISO-8601 date-time string or null
     */
    public void getMyAppointments(@Nullable String fromIso,
                                  @Nullable String toIso,
                                  @NonNull LoadAppointmentsCallback callback) {
        String authHeader = buildAuthHeaderIfAvailable();

        Call<ListResponseDto<Appointment>> call =
                appointmentApiService.getMyAppointments(authHeader, fromIso, toIso);

        call.enqueue(new Callback<ListResponseDto<Appointment>>() {
            @Override
            public void onResponse(
                    @NonNull Call<ListResponseDto<Appointment>> call,
                    @NonNull Response<ListResponseDto<Appointment>> response
            ) {
                if (!response.isSuccessful()) {
                    callback.onError(null, response.code(), safeErrorBody(response.errorBody()));
                    return;
                }
                ListResponseDto<Appointment> body = response.body();
                List<Appointment> items =
                        (body != null && body.getItems() != null)
                                ? body.getItems()
                                : Collections.emptyList();
                callback.onSuccess(items);
            }

            @Override
            public void onFailure(
                    @NonNull Call<ListResponseDto<Appointment>> call,
                    @NonNull Throwable t
            ) {
                callback.onError(t, null, null);
            }
        });
    }

    // ------------------------------------------------------------------------
    // Doctor: My appointments
    // ------------------------------------------------------------------------

    /**
     * Load appointments for the current doctor.
     *
     * @param fromIso ISO-8601 date-time string or null
     * @param toIso   ISO-8601 date-time string or null
     */
    public void getDoctorAppointments(@Nullable String fromIso,
                                      @Nullable String toIso,
                                      @NonNull LoadAppointmentsCallback callback) {
        String authHeader = buildAuthHeaderIfAvailable();

        Call<ListResponseDto<Appointment>> call =
                appointmentApiService.getDoctorAppointments(authHeader, fromIso, toIso);

        call.enqueue(new Callback<ListResponseDto<Appointment>>() {
            @Override
            public void onResponse(
                    @NonNull Call<ListResponseDto<Appointment>> call,
                    @NonNull Response<ListResponseDto<Appointment>> response
            ) {
                if (!response.isSuccessful()) {
                    callback.onError(null, response.code(), safeErrorBody(response.errorBody()));
                    return;
                }
                ListResponseDto<Appointment> body = response.body();
                List<Appointment> items =
                        (body != null && body.getItems() != null)
                                ? body.getItems()
                                : Collections.emptyList();
                callback.onSuccess(items);
            }

            @Override
            public void onFailure(
                    @NonNull Call<ListResponseDto<Appointment>> call,
                    @NonNull Throwable t
            ) {
                callback.onError(t, null, null);
            }
        });
    }

    // ------------------------------------------------------------------------
    // Doctor: Weekly calendar (for patient booking)
    // ------------------------------------------------------------------------

    /**
     * Load weekly calendar for a specific doctor (patient-facing).
     *
     * @param doctorId    doctor id
     * @param weekStartIso optional ISO date (yyyy-MM-dd) for week start, or null for current week
     */
    public void getDoctorWeeklyCalendar(long doctorId,
                                        @Nullable String weekStartIso,
                                        @NonNull WeeklyCalendarCallback callback) {
        String authHeader = buildAuthHeaderIfAvailable();

        Call<WeeklyCalendarResponse> call =
                appointmentApiService.getDoctorWeeklyCalendar(authHeader, doctorId, weekStartIso);

        call.enqueue(new Callback<WeeklyCalendarResponse>() {
            @Override
            public void onResponse(
                    @NonNull Call<WeeklyCalendarResponse> call,
                    @NonNull Response<WeeklyCalendarResponse> response
            ) {
                WeeklyCalendarResponse body = response.body();
                if (!response.isSuccessful() || body == null) {
                    callback.onError(null, response.code(), safeErrorBody(response.errorBody()));
                    return;
                }
                callback.onSuccess(body);
            }

            @Override
            public void onFailure(
                    @NonNull Call<WeeklyCalendarResponse> call,
                    @NonNull Throwable t
            ) {
                callback.onError(t, null, null);
            }
        });
    }

    // ------------------------------------------------------------------------
    // Create / cancel / status / reschedule
    // ------------------------------------------------------------------------

    public void createAppointment(@NonNull AppointmentCreateRequest request,
                                  @NonNull CreateAppointmentCallback callback) {
        String authHeader = buildAuthHeaderIfAvailable();

        Call<Appointment> call = appointmentApiService.createAppointment(authHeader, request);
        call.enqueue(new Callback<Appointment>() {
            @Override
            public void onResponse(
                    @NonNull Call<Appointment> call,
                    @NonNull Response<Appointment> response
            ) {
                Appointment body = response.body();
                if (!response.isSuccessful() || body == null) {
                    callback.onError(null, response.code(), safeErrorBody(response.errorBody()));
                    return;
                }
                callback.onSuccess(body);
            }

            @Override
            public void onFailure(@NonNull Call<Appointment> call, @NonNull Throwable t) {
                callback.onError(t, null, null);
            }
        });
    }

    public void cancelAppointment(long id,
                                  @NonNull CancelAppointmentCallback callback) {
        String authHeader = buildAuthHeaderIfAvailable();

        Call<Void> call = appointmentApiService.cancelAppointment(authHeader, id);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(
                    @NonNull Call<Void> call,
                    @NonNull Response<Void> response
            ) {
                if (response.isSuccessful()) {
                    callback.onSuccess();
                } else {
                    callback.onError(null, response.code(), safeErrorBody(response.errorBody()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                callback.onError(t, null, null);
            }
        });
    }

    public void updateAppointmentStatus(long id,
                                        @NonNull AppointmentStatusUpdateRequest request,
                                        @NonNull UpdateStatusCallback callback) {
        String authHeader = buildAuthHeaderIfAvailable();

        Call<Appointment> call = appointmentApiService.updateAppointmentStatus(authHeader, id, request);
        call.enqueue(new Callback<Appointment>() {
            @Override
            public void onResponse(
                    @NonNull Call<Appointment> call,
                    @NonNull Response<Appointment> response
            ) {
                Appointment body = response.body();
                if (!response.isSuccessful() || body == null) {
                    callback.onError(null, response.code(), safeErrorBody(response.errorBody()));
                    return;
                }
                callback.onSuccess(body);
            }

            @Override
            public void onFailure(@NonNull Call<Appointment> call, @NonNull Throwable t) {
                callback.onError(t, null, null);
            }
        });
    }

    public void rescheduleAppointment(long id,
                                      @NonNull AppointmentCreateRequest request,
                                      @NonNull RescheduleAppointmentCallback callback) {
        String authHeader = buildAuthHeaderIfAvailable();

        Call<Appointment> call = appointmentApiService.rescheduleAppointment(authHeader, id, request);
        call.enqueue(new Callback<Appointment>() {
            @Override
            public void onResponse(
                    @NonNull Call<Appointment> call,
                    @NonNull Response<Appointment> response
            ) {
                Appointment body = response.body();
                if (!response.isSuccessful() || body == null) {
                    callback.onError(null, response.code(), safeErrorBody(response.errorBody()));
                    return;
                }
                callback.onSuccess(body);
            }

            @Override
            public void onFailure(@NonNull Call<Appointment> call, @NonNull Throwable t) {
                callback.onError(t, null, null);
            }
        });
    }

    // ------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------

    @Nullable
    private String buildAuthHeaderIfAvailable() {
        AuthTokens tokens = authLocalDataSource.getTokens();
        if (tokens == null || tokens.getAccessToken() == null) {
            return null;
        }
        return "Bearer " + tokens.getAccessToken();
    }

    @Nullable
    private String safeErrorBody(@Nullable ResponseBody body) {
        if (body == null) return null;
        try {
            return body.string();
        } catch (IOException e) {
            return null;
        }
    }
    public interface HomeStatsCallback {
        void onSuccess(@NonNull DoctorHomeStats stats);
        void onError(@Nullable Throwable throwable,
                     @Nullable Integer httpCode,
                     @Nullable String errorBody);
    }

    public void getDoctorHomeStats(@NonNull HomeStatsCallback callback) {
        String authHeader = buildAuthHeaderIfAvailable();
        Call<DoctorHomeStats> call = appointmentApiService.getDoctorHomeStats(authHeader);

        call.enqueue(new Callback<DoctorHomeStats>() {
            @Override
            public void onResponse(@NonNull Call<DoctorHomeStats> call,
                                   @NonNull Response<DoctorHomeStats> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    callback.onError(null, response.code(), safeErrorBody(response.errorBody()));
                    return;
                }
                callback.onSuccess(response.body());
            }

            @Override
            public void onFailure(@NonNull Call<DoctorHomeStats> call, @NonNull Throwable t) {
                callback.onError(t, null, null);
            }
        });
    }

}
