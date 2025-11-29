package tn.esprit.data.history;

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
import tn.esprit.data.remote.history.UserHistoryApiService;
import tn.esprit.domain.auth.AuthTokens;
import tn.esprit.domain.history.UserHistoryEntry;

/**
 * Repository to fetch the current user's history entries from backend.
 *
 * Uses:
 *   GET /api/history
 */
public class UserHistoryRepository {

    private final AuthLocalDataSource authLocalDataSource;
    private final UserHistoryApiService apiService;

    public UserHistoryRepository(Context context) {
        Context appContext = context.getApplicationContext();
        this.authLocalDataSource = new AuthLocalDataSource(appContext);
        this.apiService = ApiClient.createService(UserHistoryApiService.class);
    }

    public interface LoadCallback {
        void onSuccess(List<UserHistoryEntry> items);

        /**
         * @param throwable underlying exception (may be null on HTTP error)
         * @param httpCode  HTTP status code if available, otherwise null
         * @param errorBody raw error body if available, otherwise null
         */
        void onError(@Nullable Throwable throwable,
                     @Nullable Integer httpCode,
                     @Nullable String errorBody);
    }

    /**
     * Loads the authenticated user's history (newest first).
     */
    public void loadHistory(LoadCallback callback) {
        if (callback == null) return;

        AuthTokens tokens = authLocalDataSource.getTokens();
        if (tokens == null || tokens.getAccessToken() == null) {
            callback.onError(null, 401, "Not authenticated");
            return;
        }

        String authHeader = "Bearer " + tokens.getAccessToken();

        Call<List<UserHistoryEntry>> call = apiService.getMyHistory(authHeader);
        call.enqueue(new Callback<List<UserHistoryEntry>>() {
            @Override
            public void onResponse(Call<List<UserHistoryEntry>> call,
                                   Response<List<UserHistoryEntry>> response) {
                if (!response.isSuccessful()) {
                    callback.onError(null, response.code(), safeErrorBody(response.errorBody()));
                    return;
                }

                List<UserHistoryEntry> body = response.body();
                if (body == null) {
                    callback.onSuccess(Collections.emptyList());
                } else {
                    callback.onSuccess(body);
                }
            }

            @Override
            public void onFailure(Call<List<UserHistoryEntry>> call, Throwable t) {
                callback.onError(t, null, null);
            }
        });
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
