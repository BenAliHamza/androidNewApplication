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
import tn.esprit.data.remote.appointment.DoctorScheduleApiService;
import tn.esprit.data.remote.common.ListResponseDto;
import tn.esprit.domain.appointment.DoctorSchedule;
import tn.esprit.domain.auth.AuthTokens;

/**
 * Repository for doctor weekly schedule operations.
 * - getMySchedule
 * - updateMySchedule
 */
public class DoctorScheduleRepository {

    private final AuthLocalDataSource authLocalDataSource;
    private final DoctorScheduleApiService apiService;

    public DoctorScheduleRepository(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        this.authLocalDataSource = new AuthLocalDataSource(appContext);
        this.apiService = ApiClient.createService(DoctorScheduleApiService.class);
    }

    // ------------------------------------------------------------------------
    // Load schedule
    // ------------------------------------------------------------------------

    public interface LoadScheduleCallback {
        void onSuccess(List<DoctorSchedule> schedule);

        void onError(@Nullable Throwable throwable,
                     @Nullable Integer httpCode,
                     @Nullable String errorBody);
    }

    /**
     * GET /api/doctors/me/schedule
     */
    public void getMySchedule(@NonNull LoadScheduleCallback callback) {
        String authHeader = buildAuthHeaderIfAvailable();

        Call<ListResponseDto<DoctorSchedule>> call =
                apiService.getMySchedule(authHeader);

        call.enqueue(new Callback<ListResponseDto<DoctorSchedule>>() {
            @Override
            public void onResponse(
                    @NonNull Call<ListResponseDto<DoctorSchedule>> call,
                    @NonNull Response<ListResponseDto<DoctorSchedule>> response
            ) {
                if (!response.isSuccessful()) {
                    callback.onError(null, response.code(), safeErrorBody(response.errorBody()));
                    return;
                }

                ListResponseDto<DoctorSchedule> body = response.body();
                List<DoctorSchedule> items =
                        (body != null && body.getItems() != null)
                                ? body.getItems()
                                : Collections.emptyList();

                callback.onSuccess(items);
            }

            @Override
            public void onFailure(
                    @NonNull Call<ListResponseDto<DoctorSchedule>> call,
                    @NonNull Throwable t
            ) {
                callback.onError(t, null, null);
            }
        });
    }

    // ------------------------------------------------------------------------
    // Update schedule
    // ------------------------------------------------------------------------

    public interface ScheduleCallback {
        void onSuccess(List<DoctorSchedule> updatedSchedule);

        void onError(@Nullable Throwable throwable,
                     @Nullable Integer httpCode,
                     @Nullable String errorBody);
    }

    /**
     * PUT /api/doctors/me/schedule
     */
    public void updateMySchedule(@NonNull List<DoctorSchedule> entries,
                                 @NonNull ScheduleCallback callback) {
        String authHeader = buildAuthHeaderIfAvailable();

        Call<ListResponseDto<DoctorSchedule>> call =
                apiService.updateMySchedule(authHeader, entries);

        call.enqueue(new Callback<ListResponseDto<DoctorSchedule>>() {
            @Override
            public void onResponse(
                    @NonNull Call<ListResponseDto<DoctorSchedule>> call,
                    @NonNull Response<ListResponseDto<DoctorSchedule>> response
            ) {
                if (!response.isSuccessful()) {
                    callback.onError(null, response.code(), safeErrorBody(response.errorBody()));
                    return;
                }

                ListResponseDto<DoctorSchedule> body = response.body();
                List<DoctorSchedule> items =
                        (body != null && body.getItems() != null)
                                ? body.getItems()
                                : Collections.emptyList();

                callback.onSuccess(items);
            }

            @Override
            public void onFailure(
                    @NonNull Call<ListResponseDto<DoctorSchedule>> call,
                    @NonNull Throwable t
            ) {
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
}
