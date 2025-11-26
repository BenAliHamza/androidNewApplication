package tn.esprit.data.medication;

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
import tn.esprit.data.remote.common.ListResponseDto;
import tn.esprit.data.remote.medication.PrescriptionApiService;
import tn.esprit.domain.auth.AuthTokens;
import tn.esprit.domain.medication.Prescription;
import tn.esprit.domain.medication.PrescriptionCreateRequest;
import tn.esprit.domain.medication.PrescriptionLine;

/**
 * Repository for prescription-related operations.
 *
 * Scope:
 *  - Doctor:
 *      - list prescriptions for a given patient
 *      - create prescription for a patient
 *      - delete own prescription
 *  - Patient:
 *      - list own prescriptions
 *      - update reminder for a line
 */
public class PrescriptionRepository {

    private final AuthLocalDataSource authLocalDataSource;
    private final PrescriptionApiService apiService;

    public PrescriptionRepository(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        this.authLocalDataSource = new AuthLocalDataSource(appContext);
        this.apiService = ApiClient.createService(PrescriptionApiService.class);
    }

    // ---------------------------------------------------------------------
    // Callbacks
    // ---------------------------------------------------------------------

    public interface PrescriptionsCallback {
        void onSuccess(List<Prescription> prescriptions);

        void onError(@Nullable Throwable throwable,
                     @Nullable Integer httpCode,
                     @Nullable String errorBody);
    }

    public interface CreatePrescriptionCallback {
        void onSuccess(Prescription prescription);

        void onError(@Nullable Throwable throwable,
                     @Nullable Integer httpCode,
                     @Nullable String errorBody);
    }

    public interface DeletePrescriptionCallback {
        void onSuccess();

        void onError(@Nullable Throwable throwable,
                     @Nullable Integer httpCode,
                     @Nullable String errorBody);
    }

    public interface UpdateReminderCallback {
        void onSuccess(@NonNull PrescriptionLine updatedLine);

        void onError(@Nullable Throwable throwable,
                     @Nullable Integer httpCode,
                     @Nullable String errorBody);
    }

    // ---------------------------------------------------------------------
    // Doctor: list prescriptions for a patient
    // ---------------------------------------------------------------------

    /**
     * GET /api/doctors/me/patients/{patientUserId}/prescriptions
     *
     * @param patientUserId patient User.id
     * @param activeOnly    pass true to only see active prescriptions, or null for all
     */
    public void getPrescriptionsForPatientAsDoctor(long patientUserId,
                                                   @Nullable Boolean activeOnly,
                                                   @NonNull PrescriptionsCallback callback) {
        String authHeader = buildAuthHeaderIfAvailable();

        Call<ListResponseDto<Prescription>> call =
                apiService.getPrescriptionsForPatientAsDoctor(
                        authHeader,
                        patientUserId,
                        activeOnly
                );

        call.enqueue(new Callback<ListResponseDto<Prescription>>() {
            @Override
            public void onResponse(
                    @NonNull Call<ListResponseDto<Prescription>> call,
                    @NonNull Response<ListResponseDto<Prescription>> response
            ) {
                if (!response.isSuccessful()) {
                    callback.onError(null, response.code(), safeErrorBody(response.errorBody()));
                    return;
                }

                ListResponseDto<Prescription> body = response.body();
                List<Prescription> items =
                        (body != null && body.getItems() != null)
                                ? body.getItems()
                                : Collections.emptyList();

                callback.onSuccess(items);
            }

            @Override
            public void onFailure(
                    @NonNull Call<ListResponseDto<Prescription>> call,
                    @NonNull Throwable t
            ) {
                callback.onError(t, null, null);
            }
        });
    }

    // ---------------------------------------------------------------------
    // Doctor: create prescription for a patient
    // ---------------------------------------------------------------------

    /**
     * POST /api/doctors/me/patients/{patientUserId}/prescriptions
     */
    public void createPrescriptionForPatient(long patientUserId,
                                             @NonNull PrescriptionCreateRequest request,
                                             @NonNull CreatePrescriptionCallback callback) {
        String authHeader = buildAuthHeaderIfAvailable();

        Call<Prescription> call =
                apiService.createPrescriptionForPatientAsDoctor(
                        authHeader,
                        patientUserId,
                        request
                );

        call.enqueue(new Callback<Prescription>() {
            @Override
            public void onResponse(
                    @NonNull Call<Prescription> call,
                    @NonNull Response<Prescription> response
            ) {
                if (!response.isSuccessful()) {
                    callback.onError(null, response.code(), safeErrorBody(response.errorBody()));
                    return;
                }
                Prescription body = response.body();
                if (body == null) {
                    callback.onError(null, response.code(), null);
                } else {
                    callback.onSuccess(body);
                }
            }

            @Override
            public void onFailure(
                    @NonNull Call<Prescription> call,
                    @NonNull Throwable t
            ) {
                callback.onError(t, null, null);
            }
        });
    }

    // ---------------------------------------------------------------------
    // Doctor: delete prescription
    // ---------------------------------------------------------------------

    /**
     * DELETE /api/doctors/me/prescriptions/{prescriptionId}
     */
    public void deletePrescriptionForDoctor(long prescriptionId,
                                            @NonNull DeletePrescriptionCallback callback) {
        String authHeader = buildAuthHeaderIfAvailable();

        Call<Void> call =
                apiService.deletePrescriptionAsDoctor(
                        authHeader,
                        prescriptionId
                );

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(
                    @NonNull Call<Void> call,
                    @NonNull Response<Void> response
            ) {
                if (!response.isSuccessful()) {
                    callback.onError(null, response.code(), safeErrorBody(response.errorBody()));
                    return;
                }
                callback.onSuccess();
            }

            @Override
            public void onFailure(
                    @NonNull Call<Void> call,
                    @NonNull Throwable t
            ) {
                callback.onError(t, null, null);
            }
        });
    }

    // ---------------------------------------------------------------------
    // Patient: list own prescriptions
    // ---------------------------------------------------------------------

    /**
     * GET /api/prescriptions/me?activeOnly=true
     */
    public void getMyPrescriptions(@Nullable Boolean activeOnly,
                                   @NonNull PrescriptionsCallback callback) {
        String authHeader = buildAuthHeaderIfAvailable();

        Call<ListResponseDto<Prescription>> call =
                apiService.getMyPrescriptions(
                        authHeader,
                        activeOnly
                );

        call.enqueue(new Callback<ListResponseDto<Prescription>>() {
            @Override
            public void onResponse(
                    @NonNull Call<ListResponseDto<Prescription>> call,
                    @NonNull Response<ListResponseDto<Prescription>> response
            ) {
                if (!response.isSuccessful()) {
                    callback.onError(null, response.code(), safeErrorBody(response.errorBody()));
                    return;
                }

                ListResponseDto<Prescription> body = response.body();
                List<Prescription> items =
                        (body != null && body.getItems() != null)
                                ? body.getItems()
                                : Collections.emptyList();

                callback.onSuccess(items);
            }

            @Override
            public void onFailure(
                    @NonNull Call<ListResponseDto<Prescription>> call,
                    @NonNull Throwable t
            ) {
                callback.onError(t, null, null);
            }
        });
    }

    // ---------------------------------------------------------------------
    // Patient: update reminder for a line
    // ---------------------------------------------------------------------

    /**
     * PATCH /api/prescriptions/me/lines/{lineId}/reminder
     */
    public void updateMyLineReminder(long lineId,
                                     boolean reminderEnabled,
                                     @NonNull UpdateReminderCallback callback) {
        String authHeader = buildAuthHeaderIfAvailable();

        PrescriptionApiService.ReminderUpdateRequestDto body =
                new PrescriptionApiService.ReminderUpdateRequestDto(reminderEnabled);

        Call<PrescriptionLine> call =
                apiService.updateMyLineReminder(
                        authHeader,
                        lineId,
                        body
                );

        call.enqueue(new Callback<PrescriptionLine>() {
            @Override
            public void onResponse(
                    @NonNull Call<PrescriptionLine> call,
                    @NonNull Response<PrescriptionLine> response
            ) {
                if (!response.isSuccessful()) {
                    callback.onError(null, response.code(), safeErrorBody(response.errorBody()));
                    return;
                }

                PrescriptionLine line = response.body();
                if (line == null) {
                    callback.onError(null, response.code(), null);
                } else {
                    callback.onSuccess(line);
                }
            }

            @Override
            public void onFailure(
                    @NonNull Call<PrescriptionLine> call,
                    @NonNull Throwable t
            ) {
                callback.onError(t, null, null);
            }
        });
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

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
