package tn.esprit.data.doctor;

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
import tn.esprit.data.remote.doctor.DoctorApiService;
import tn.esprit.domain.auth.AuthTokens;
import tn.esprit.domain.patient.PatientProfile;

/**
 * Repository for doctor-specific patient operations:
 *  - list patients of current doctor
 *  - load a single patient by user id
 *  - remove a patient from the doctor.
 */
public class DoctorPatientsRepository {

    private final AuthLocalDataSource authLocalDataSource;
    private final DoctorApiService doctorApiService;

    public DoctorPatientsRepository(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        this.authLocalDataSource = new AuthLocalDataSource(appContext);
        this.doctorApiService = ApiClient.createService(DoctorApiService.class);
    }

    // ------------------------------------------------------------------------
    // Load patients (list)
    // ------------------------------------------------------------------------

    public interface LoadPatientsCallback {
        void onSuccess(List<PatientProfile> patients);

        void onError(@Nullable Throwable throwable,
                     @Nullable Integer httpCode,
                     @Nullable String errorBody);
    }

    /**
     * GET /api/doctors/me/patients
     * Uses ListResponse wrapper from backend.
     */
    public void getMyPatients(@NonNull LoadPatientsCallback callback) {
        String authHeader = buildAuthHeaderIfAvailable();

        Call<ListResponseDto<PatientProfile>> call =
                doctorApiService.getMyPatients(authHeader);

        call.enqueue(new Callback<ListResponseDto<PatientProfile>>() {
            @Override
            public void onResponse(
                    @NonNull Call<ListResponseDto<PatientProfile>> call,
                    @NonNull Response<ListResponseDto<PatientProfile>> response
            ) {
                if (!response.isSuccessful()) {
                    callback.onError(null, response.code(), safeErrorBody(response.errorBody()));
                    return;
                }

                ListResponseDto<PatientProfile> body = response.body();
                List<PatientProfile> items =
                        (body != null && body.getItems() != null)
                                ? body.getItems()
                                : Collections.emptyList();

                callback.onSuccess(items);
            }

            @Override
            public void onFailure(
                    @NonNull Call<ListResponseDto<PatientProfile>> call,
                    @NonNull Throwable t
            ) {
                callback.onError(t, null, null);
            }
        });
    }

    // ------------------------------------------------------------------------
    // Load single patient by user id
    // ------------------------------------------------------------------------

    public interface LoadPatientCallback {
        void onSuccess(PatientProfile patient);

        void onError(@Nullable Throwable throwable,
                     @Nullable Integer httpCode,
                     @Nullable String errorBody);
    }

    /**
     * GET /api/doctors/me/patients/{patientUserId}
     *
     * @param patientUserId the User.id of the patient
     */
    public void getMyPatient(long patientUserId,
                             @NonNull LoadPatientCallback callback) {
        String authHeader = buildAuthHeaderIfAvailable();

        Call<PatientProfile> call =
                doctorApiService.getMyPatientByUserId(authHeader, patientUserId);

        call.enqueue(new Callback<PatientProfile>() {
            @Override
            public void onResponse(
                    @NonNull Call<PatientProfile> call,
                    @NonNull Response<PatientProfile> response
            ) {
                if (!response.isSuccessful()) {
                    callback.onError(null, response.code(), safeErrorBody(response.errorBody()));
                    return;
                }

                PatientProfile body = response.body();
                if (body == null) {
                    callback.onError(null, response.code(), "Empty body");
                    return;
                }

                callback.onSuccess(body);
            }

            @Override
            public void onFailure(
                    @NonNull Call<PatientProfile> call,
                    @NonNull Throwable t
            ) {
                callback.onError(t, null, null);
            }
        });
    }

    // ------------------------------------------------------------------------
    // Remove patient
    // ------------------------------------------------------------------------

    public interface RemovePatientCallback {
        void onSuccess();

        void onError(@Nullable Throwable throwable,
                     @Nullable Integer httpCode,
                     @Nullable String errorBody);
    }

    /**
     * DELETE /api/doctors/me/patients/{patientUserId}
     */
    public void removePatient(long patientUserId,
                              @NonNull RemovePatientCallback callback) {
        String authHeader = buildAuthHeaderIfAvailable();

        Call<Void> call = doctorApiService.removePatientFromMe(authHeader, patientUserId);

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
            public void onFailure(
                    @NonNull Call<Void> call,
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
