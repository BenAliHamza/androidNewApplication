package tn.esprit.data.doctor;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import tn.esprit.data.auth.AuthLocalDataSource;
import tn.esprit.data.remote.ApiClient;
import tn.esprit.data.remote.doctor.DoctorStatsApiService;
import tn.esprit.domain.doctor.DoctorHomeStats;

public class DoctorStatsRepository {

    private final AuthLocalDataSource authLocalDataSource;
    private final DoctorStatsApiService api;

    public interface StatsCallback {
        void onSuccess(@NonNull DoctorHomeStats stats);
        void onError(@Nullable Throwable throwable,
                     @Nullable Integer code,
                     @Nullable String errorBody);
    }

    public DoctorStatsRepository(@NonNull Context ctx) {
        authLocalDataSource = new AuthLocalDataSource(ctx);
        api = ApiClient.createService(DoctorStatsApiService.class);
    }

    @Nullable
    private String authHeader() {
        if (authLocalDataSource.getTokens() == null) return null;
        if (authLocalDataSource.getTokens().getAccessToken() == null) return null;
        return "Bearer " + authLocalDataSource.getTokens().getAccessToken();
    }

    public void loadStats(@NonNull StatsCallback callback) {
        String h = authHeader();
        Call<DoctorHomeStats> call = api.getDoctorStats(h);

        call.enqueue(new Callback<DoctorHomeStats>() {
            @Override
            public void onResponse(
                    @NonNull Call<DoctorHomeStats> call,
                    @NonNull Response<DoctorHomeStats> response
            ) {
                DoctorHomeStats body = response.body();
                if (!response.isSuccessful() || body == null) {
                    callback.onError(null, response.code(), safe(response.errorBody()));
                    return;
                }
                callback.onSuccess(body);
            }

            @Override
            public void onFailure(
                    @NonNull Call<DoctorHomeStats> call,
                    @NonNull Throwable t
            ) {
                callback.onError(t, null, null);
            }
        });
    }

    @Nullable
    private String safe(@Nullable ResponseBody body) {
        if (body == null) return null;
        try { return body.string(); } catch (IOException e) { return null; }
    }
}
