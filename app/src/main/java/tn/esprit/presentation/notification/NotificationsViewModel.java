package tn.esprit.presentation.notification;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import tn.esprit.R;
import tn.esprit.data.notification.NotificationRepository;
import tn.esprit.data.notification.NotificationSocketManager;
import tn.esprit.domain.notification.NotificationItem;

/**
 * ViewModel backing the notifications list screen.
 *
 * Responsibilities:
 *  - Load notifications via REST
 *  - Expose loading + error state
 *  - Expose unread count
 *  - Listen to WebSocket pushes and refresh list
 *  - Mark individual notifications as read and refresh list
 *
 * IMPORTANT:
 *  - Intended to be scoped to MainActivity (activity scope), then
 *    shared by NotificationListFragment using requireActivity().
 */
public class NotificationsViewModel extends AndroidViewModel {

    private final NotificationRepository repository;
    private final NotificationSocketManager socketManager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final MutableLiveData<Boolean> loading =
            new MutableLiveData<>(false);
    private final MutableLiveData<List<NotificationItem>> notifications =
            new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<String> errorMessage =
            new MutableLiveData<>(null);
    private final MutableLiveData<Integer> unreadCount =
            new MutableLiveData<>(0);

    @Nullable
    private Long currentUserId = null;

    public NotificationsViewModel(@NonNull Application application) {
        super(application);
        repository = new NotificationRepository(application.getApplicationContext());
        socketManager = NotificationSocketManager.getInstance(application.getApplicationContext());
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<List<NotificationItem>> getNotifications() {
        return notifications;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Integer> getUnreadCount() {
        return unreadCount;
    }

    public void clearError() {
        errorMessage.setValue(null);
    }

    // -------------------------------------------------------------------------
    // WebSocket wiring
    // -------------------------------------------------------------------------

    /**
     * Called once we know the authenticated user id.
     * This will:
     *  - connect the WebSocket
     *  - subscribe to /topic/users/{userId}/appointments
     *  - on each MESSAGE, reload notifications via REST
     */
    public void setCurrentUserId(long userId) {
        if (userId <= 0L) return;

        // If same user id as before, avoid re-setting everything
        if (currentUserId != null && currentUserId == userId) {
            return;
        }

        currentUserId = userId;

        // Listener: whenever we receive a pushed notification, reload list from backend.
        socketManager.setListener(item -> {
            // This callback is on a background thread (OkHttp).
            // We just trigger a reload on main thread.
            mainHandler.post(this::loadNotifications);
        });

        // Connect STOMP over WebSocket for this user
        socketManager.connect(userId);

        // Also do an initial load
        loadNotifications();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Clean up socket to avoid leaks
        socketManager.setListener(null);
        socketManager.disconnect();
    }

    // -------------------------------------------------------------------------
    // Load notifications
    // -------------------------------------------------------------------------

    public void loadNotifications() {
        // Use postValue so this is safe from any thread (incl. WebSocket callback)
        loading.postValue(true);
        errorMessage.postValue(null);

        repository.getMyNotifications(new NotificationRepository.LoadNotificationsCallback() {
            @Override
            public void onSuccess(List<NotificationItem> list) {
                loading.postValue(false);

                List<NotificationItem> sorted = sortByCreatedAtDesc(list);
                notifications.postValue(sorted);
                unreadCount.postValue(computeUnread(sorted));
            }

            @Override
            public void onError(@Nullable Throwable throwable,
                                @Nullable Integer httpCode,
                                @Nullable String errorBody) {
                loading.postValue(false);

                String msg = getApplication().getString(R.string.notifications_error_generic);
                errorMessage.postValue(msg);
            }
        });
    }

    // -------------------------------------------------------------------------
    // Mark as read
    // -------------------------------------------------------------------------

    public void markAsRead(@NonNull NotificationItem item) {
        Long id = item.getId();
        if (id == null || id <= 0L) {
            return;
        }

        repository.markAsRead(id, new NotificationRepository.MarkAsReadCallback() {
            @Override
            public void onSuccess() {
                // Simplest and safest: reload the list from backend
                loadNotifications();
            }

            @Override
            public void onError(@Nullable Throwable throwable,
                                @Nullable Integer httpCode,
                                @Nullable String errorBody) {
                String msg = getApplication().getString(R.string.notifications_error_mark_read);
                errorMessage.postValue(msg);
            }
        });
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private List<NotificationItem> sortByCreatedAtDesc(@Nullable List<NotificationItem> list) {
        if (list == null) return Collections.emptyList();

        List<NotificationItem> copy = new ArrayList<>(list);
        // LocalDateTime ISO strings sort lexicographically
        Collections.sort(copy, new Comparator<NotificationItem>() {
            @Override
            public int compare(NotificationItem o1, NotificationItem o2) {
                String d1 = o1 != null && o1.getCreatedAt() != null ? o1.getCreatedAt() : "";
                String d2 = o2 != null && o2.getCreatedAt() != null ? o2.getCreatedAt() : "";
                // Descending: newest first
                return d2.compareTo(d1);
            }
        });
        return copy;
    }

    private int computeUnread(@Nullable List<NotificationItem> list) {
        if (list == null) return 0;
        int count = 0;
        for (NotificationItem n : list) {
            if (n != null && !n.isRead()) {
                count++;
            }
        }
        return count;
    }
}
