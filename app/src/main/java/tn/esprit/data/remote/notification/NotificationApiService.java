package tn.esprit.data.remote.notification;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

import tn.esprit.data.remote.common.ListResponseDto;
import tn.esprit.domain.notification.NotificationItem;

/**
 * REST API for notifications.
 *
 * Backend endpoints:
 *  - GET /api/notifications/me
 *  - POST /api/notifications/{id}/read
 */
public interface NotificationApiService {

    @GET("/api/notifications/me")
    Call<ListResponseDto<NotificationItem>> getMyNotifications(
            @Header("Authorization") String authHeader
    );

    @POST("/api/notifications/{id}/read")
    Call<Void> markAsRead(
            @Header("Authorization") String authHeader,
            @Path("id") Long notificationId
    );
}
