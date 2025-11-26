package tn.esprit.data.doctor;

import android.content.Context;

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
import tn.esprit.domain.doctor.DoctorPublicProfile;
import tn.esprit.domain.doctor.DoctorSearchFilters;
import tn.esprit.domain.doctor.DoctorSearchResult;

/**
 * Repository for doctor directory operations:
 *  - searching doctors
 *  - loading public doctor profile for landing screen.
 *
 * No mapping layer: Retrofit parses backend DTOs directly into domain models.
 */
public class DoctorDirectoryRepository {

    private final AuthLocalDataSource authLocalDataSource;
    private final DoctorApiService doctorApiService;

    public DoctorDirectoryRepository(Context context) {
        Context appContext = context.getApplicationContext();
        this.authLocalDataSource = new AuthLocalDataSource(appContext);
        this.doctorApiService = ApiClient.createService(DoctorApiService.class);
    }

    // ---------- Search ----------

    public interface SearchCallback {
        void onSuccess(List<DoctorSearchResult> results);

        /**
         * @param throwable underlying exception (may be null on HTTP error)
         * @param httpCode  HTTP status code if available, otherwise null
         * @param errorBody raw error body if available, otherwise null
         */
        void onError(@Nullable Throwable throwable,
                     @Nullable Integer httpCode,
                     @Nullable String errorBody);
    }

    public void searchDoctors(DoctorSearchFilters filters, SearchCallback callback) {
        if (filters == null) {
            filters = new DoctorSearchFilters();
        }

        String authHeader = buildAuthHeaderIfAvailable();

        Call<ListResponseDto<DoctorSearchResult>> call = doctorApiService.searchDoctors(
                authHeader,
                filters.getQuery(),
                filters.getSpecialtyId(),
                filters.getCity(),
                filters.getCountry(),
                filters.getTeleconsultationEnabled(),
                filters.getAcceptingNewPatients()
        );

        call.enqueue(new Callback<ListResponseDto<DoctorSearchResult>>() {
            @Override
            public void onResponse(Call<ListResponseDto<DoctorSearchResult>> call,
                                   Response<ListResponseDto<DoctorSearchResult>> response) {
                if (!response.isSuccessful()) {
                    String errorBody = safeErrorBody(response.errorBody());
                    callback.onError(null, response.code(), errorBody);
                    return;
                }

                ListResponseDto<DoctorSearchResult> body = response.body();
                List<DoctorSearchResult> items =
                        (body != null && body.getItems() != null)
                                ? body.getItems()
                                : Collections.emptyList();

                callback.onSuccess(items);
            }

            @Override
            public void onFailure(Call<ListResponseDto<DoctorSearchResult>> call, Throwable t) {
                callback.onError(t, null, null);
            }
        });
    }

    // ---------- Public profile ----------

    public interface PublicProfileCallback {
        void onSuccess(DoctorPublicProfile profile);

        void onError(@Nullable Throwable throwable,
                     @Nullable Integer httpCode,
                     @Nullable String errorBody);
    }

    public void getDoctorPublicProfile(long doctorId, PublicProfileCallback callback) {
        String authHeader = buildAuthHeaderIfAvailable();

        Call<DoctorPublicProfile> call =
                doctorApiService.getDoctorPublicProfile(authHeader, doctorId);

        call.enqueue(new Callback<DoctorPublicProfile>() {
            @Override
            public void onResponse(Call<DoctorPublicProfile> call,
                                   Response<DoctorPublicProfile> response) {
                if (!response.isSuccessful()) {
                    String errorBody = safeErrorBody(response.errorBody());
                    callback.onError(null, response.code(), errorBody);
                    return;
                }

                DoctorPublicProfile body = response.body();
                if (body == null) {
                    // Keep contract: never pass null on success
                    callback.onError(null, response.code(), "Empty body");
                    return;
                }

                callback.onSuccess(body);
            }

            @Override
            public void onFailure(Call<DoctorPublicProfile> call, Throwable t) {
                callback.onError(t, null, null);
            }
        });
    }

    // ---------- Helpers ----------

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
