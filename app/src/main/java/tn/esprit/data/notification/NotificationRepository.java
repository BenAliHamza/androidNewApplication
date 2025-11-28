package tn.esprit.data.notification;

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
import tn.esprit.data.remote.notification.NotificationApiService;
import tn.esprit.domain.auth.AuthTokens;
import tn.esprit.domain.notification.NotificationItem;

/**
 * Repository for loading and updating notifications for the current user.
 *
 * Uses:
 *  - GET /api/notifications/me
 *  - POST /api/notifications/{id}/read
 */
public class NotificationRepository {

    private final AuthLocalDataSource authLocalDataSource;
    private final NotificationApiService notificationApiService;

    public NotificationRepository(Context context) {
        Context appContext = context.getApplicationContext();
        this.authLocalDataSource = new AuthLocalDataSource(appContext);
        this.notificationApiService = ApiClient.createService(NotificationApiService.class);
    }

    // -------------------------------------------------------------------------
    // Callbacks
    // -------------------------------------------------------------------------

    public interface LoadNotificationsCallback {
        void onSuccess(List<NotificationItem> notifications);

        void onError(@Nullable Throwable throwable,
                     @Nullable Integer httpCode,
                     @Nullable String errorBody);
    }

    public interface MarkAsReadCallback {
        void onSuccess();

        void onError(@Nullable Throwable throwable,
                     @Nullable Integer httpCode,
                     @Nullable String errorBody);
    }

    // -------------------------------------------------------------------------
    // API calls
    // -------------------------------------------------------------------------

    public void getMyNotifications(LoadNotificationsCallback callback) {
        AuthTokens tokens = authLocalDataSource.getTokens();
        if (tokens == null || tokens.getAccessToken() == null) {
            if (callback != null) {
                callback.onError(null, 401, "Not authenticated");
            }
            return;
        }

        String authHeader = buildAuthHeader(tokens);

        notificationApiService.getMyNotifications(authHeader)
                .enqueue(new Callback<ListResponseDto<NotificationItem>>() {
                    @Override
                    public void onResponse(
                            Call<ListResponseDto<NotificationItem>> call,
                            Response<ListResponseDto<NotificationItem>> response
                    ) {
                        if (callback == null) return;

                        if (!response.isSuccessful()) {
                            callback.onError(
                                    null,
                                    response.code(),
                                    safeErrorBody(response.errorBody())
                            );
                            return;
                        }

                        ListResponseDto<NotificationItem> body = response.body();
                        if (body == null || body.getItems() == null) {
                            callback.onSuccess(Collections.emptyList());
                        } else {
                            callback.onSuccess(body.getItems());
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<ListResponseDto<NotificationItem>> call,
                            Throwable t
                    ) {
                        if (callback != null) {
                            callback.onError(t, null, null);
                        }
                    }
                });
    }

    public void markAsRead(long notificationId, MarkAsReadCallback callback) {
        AuthTokens tokens = authLocalDataSource.getTokens();
        if (tokens == null || tokens.getAccessToken() == null) {
            if (callback != null) {
                callback.onError(null, 401, "Not authenticated");
            }
            return;
        }

        String authHeader = buildAuthHeader(tokens);

        notificationApiService.markAsRead(authHeader, notificationId)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(
                            Call<Void> call,
                            Response<Void> response
                    ) {
                        if (callback == null) return;

                        if (!response.isSuccessful()) {
                            callback.onError(
                                    null,
                                    response.code(),
                                    safeErrorBody(response.errorBody())
                            );
                        } else {
                            callback.onSuccess();
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        if (callback != null) {
                            callback.onError(t, null, null);
                        }
                    }
                });
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String buildAuthHeader(AuthTokens tokens) {
        String type = tokens.getTokenType() != null ? tokens.getTokenType() : "Bearer";
        return type + " " + tokens.getAccessToken();
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
