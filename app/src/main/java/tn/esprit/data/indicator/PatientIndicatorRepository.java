package tn.esprit.data.indicator;

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
import tn.esprit.data.remote.ApiClient;
import tn.esprit.data.remote.common.ListResponseDto;
import tn.esprit.data.remote.indicator.IndicatorApiService;
import tn.esprit.data.remote.indicator.IndicatorApiService.PatientIndicatorCreateRequestDto;
import tn.esprit.domain.indicator.IndicatorType;
import tn.esprit.domain.indicator.PatientIndicator;

/**
 * Repository for patient indicators.
 *
 * NOTE: This repository does NOT know how to get auth tokens.
 * Callers should pass a ready-made Authorization header (e.g. "Bearer ..."),
 * or null for now (backend will then respond 401).
 */
public class PatientIndicatorRepository {

    private final IndicatorApiService apiService;

    public PatientIndicatorRepository(@NonNull Context context) {
        // Context is currently unused, but kept for consistency with other repos
        this.apiService = ApiClient.createService(IndicatorApiService.class);
    }

    // ------------------------------------------------------------------------
    // Indicator types
    // ------------------------------------------------------------------------

    public interface IndicatorTypesCallback {
        void onSuccess(List<IndicatorType> types);

        void onError(@Nullable Throwable throwable,
                     @Nullable Integer httpCode,
                     @Nullable String errorBody);
    }

    /**
     * Retrieve catalog of indicator types.
     *
     * GET /indicator-types
     * No auth header for now.
     */
    public void getIndicatorTypes(@NonNull IndicatorTypesCallback callback) {
        Call<ListResponseDto<IndicatorType>> call = apiService.getIndicatorTypes();

        call.enqueue(new Callback<ListResponseDto<IndicatorType>>() {
            @Override
            public void onResponse(
                    @NonNull Call<ListResponseDto<IndicatorType>> call,
                    @NonNull Response<ListResponseDto<IndicatorType>> response
            ) {
                if (response.isSuccessful()) {
                    ListResponseDto<IndicatorType> body = response.body();
                    List<IndicatorType> items = (body != null && body.getItems() != null)
                            ? body.getItems()
                            : Collections.emptyList();
                    callback.onSuccess(items);
                } else {
                    String errorText = null;
                    ResponseBody errorBody = response.errorBody();
                    if (errorBody != null) {
                        try {
                            errorText = errorBody.string();
                        } catch (IOException ignored) {
                        }
                    }
                    callback.onError(null, response.code(), errorText);
                }
            }

            @Override
            public void onFailure(
                    @NonNull Call<ListResponseDto<IndicatorType>> call,
                    @NonNull Throwable t
            ) {
                callback.onError(t, null, null);
            }
        });
    }

    // ------------------------------------------------------------------------
    // List indicators for current patient
    // ------------------------------------------------------------------------

    public interface IndicatorsCallback {
        void onSuccess(List<PatientIndicator> indicators);

        void onError(@Nullable Throwable throwable,
                     @Nullable Integer httpCode,
                     @Nullable String errorBody);
    }

    /**
     * Retrieve indicators for current patient.
     *
     * Any of indicatorTypeId, fromIso, toIso can be null.
     */
    public void getMyIndicators(@Nullable String authorizationHeader,
                                @Nullable Long indicatorTypeId,
                                @Nullable String fromIso,
                                @Nullable String toIso,
                                @NonNull IndicatorsCallback callback) {

        Call<ListResponseDto<PatientIndicator>> call =
                apiService.getMyIndicators(authorizationHeader, indicatorTypeId, fromIso, toIso);

        call.enqueue(new Callback<ListResponseDto<PatientIndicator>>() {
            @Override
            public void onResponse(
                    @NonNull Call<ListResponseDto<PatientIndicator>> call,
                    @NonNull Response<ListResponseDto<PatientIndicator>> response
            ) {
                if (response.isSuccessful()) {
                    ListResponseDto<PatientIndicator> body = response.body();
                    List<PatientIndicator> items = (body != null && body.getItems() != null)
                            ? body.getItems()
                            : Collections.emptyList();
                    callback.onSuccess(items);
                } else {
                    String errorText = null;
                    ResponseBody errorBody = response.errorBody();
                    if (errorBody != null) {
                        try {
                            errorText = errorBody.string();
                        } catch (IOException ignored) {
                        }
                    }
                    callback.onError(null, response.code(), errorText);
                }
            }

            @Override
            public void onFailure(
                    @NonNull Call<ListResponseDto<PatientIndicator>> call,
                    @NonNull Throwable t
            ) {
                callback.onError(t, null, null);
            }
        });
    }

    // ------------------------------------------------------------------------
    // Add new indicator for current patient
    // ------------------------------------------------------------------------

    public interface AddIndicatorCallback {
        void onSuccess(PatientIndicator created);

        void onError(@Nullable Throwable throwable,
                     @Nullable Integer httpCode,
                     @Nullable String errorBody);
    }

    /**
     * Adds a new indicator measurement for the current patient.
     *
     * @param authorizationHeader e.g. "Bearer <token>" (can be null for now)
     * @param request             create request DTO
     */
    public void addMyIndicator(@Nullable String authorizationHeader,
                               @NonNull PatientIndicatorCreateRequestDto request,
                               @NonNull AddIndicatorCallback callback) {

        Call<PatientIndicator> call = apiService.addMyIndicator(authorizationHeader, request);

        call.enqueue(new Callback<PatientIndicator>() {
            @Override
            public void onResponse(
                    @NonNull Call<PatientIndicator> call,
                    @NonNull Response<PatientIndicator> response
            ) {
                if (response.isSuccessful()) {
                    PatientIndicator body = response.body();
                    callback.onSuccess(body);
                } else {
                    String errorText = null;
                    ResponseBody errorBody = response.errorBody();
                    if (errorBody != null) {
                        try {
                            errorText = errorBody.string();
                        } catch (IOException ignored) {
                        }
                    }
                    callback.onError(null, response.code(), errorText);
                }
            }

            @Override
            public void onFailure(
                    @NonNull Call<PatientIndicator> call,
                    @NonNull Throwable t
            ) {
                callback.onError(t, null, null);
            }
        });
    }

    // ------------------------------------------------------------------------
    // Delete indicator for current patient
    // ------------------------------------------------------------------------

    public interface DeleteIndicatorCallback {
        void onSuccess();

        void onError(@Nullable Throwable throwable,
                     @Nullable Integer httpCode,
                     @Nullable String errorBody);
    }

    /**
     * Deletes a single indicator of the current patient.
     *
     * @param authorizationHeader e.g. "Bearer <token>" (can be null for now)
     * @param indicatorId         id to delete (required)
     */
    public void deleteMyIndicator(@Nullable String authorizationHeader,
                                  @NonNull Long indicatorId,
                                  @NonNull DeleteIndicatorCallback callback) {

        Call<Void> call = apiService.deleteMyIndicator(authorizationHeader, indicatorId);

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(
                    @NonNull Call<Void> call,
                    @NonNull Response<Void> response
            ) {
                if (response.isSuccessful()) {
                    callback.onSuccess();
                } else {
                    String errorText = null;
                    ResponseBody errorBody = response.errorBody();
                    if (errorBody != null) {
                        try {
                            errorText = errorBody.string();
                        } catch (IOException ignored) {
                        }
                    }
                    callback.onError(null, response.code(), errorText);
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
}
