package tn.esprit.presentation.notification;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.Collections;
import java.util.List;

import tn.esprit.data.notification.NotificationRepository;
import tn.esprit.domain.notification.NotificationItem;

/**
 * ViewModel for the notifications list screen.
 *
 * Responsibilities:
 *  - Load notifications from backend
 *  - Expose loading / error / list / unread count
 *  - Mark notifications as read
 */
public class NotificationListViewModel extends AndroidViewModel {

    private final NotificationRepository repository;

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<List<NotificationItem>> notifications =
            new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>(null);
    private final MutableLiveData<Integer> unreadCount = new MutableLiveData<>(0);

    public NotificationListViewModel(@NonNull Application application) {
        super(application);
        repository = new NotificationRepository(application.getApplicationContext());
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
    // Public actions
    // -------------------------------------------------------------------------

    /**
     * Load current user's notifications from backend.
     */
    public void loadNotifications() {
        loading.setValue(true);
        errorMessage.setValue(null);

        repository.getMyNotifications(new NotificationRepository.LoadNotificationsCallback() {
            @Override
            public void onSuccess(@NonNull List<NotificationItem> list) {
                loading.postValue(false);
                notifications.postValue(list);
                unreadCount.postValue(calculateUnreadCount(list));
            }

            @Override
            public void onError(@Nullable Throwable throwable,
                                @Nullable Integer httpCode,
                                @Nullable String errorBody) {
                loading.postValue(false);

                String msg = "Failed to load notifications.";
                if (httpCode != null) {
                    msg = msg + " Code: " + httpCode;
                }
                if (errorBody != null && !errorBody.isEmpty()) {
                    msg = msg + " " + errorBody;
                }

                errorMessage.postValue(msg);
            }
        });
    }

    /**
     * Mark a single notification as read.
     * After backend confirms, reloads notifications to stay in sync.
     */
    public void markAsRead(@NonNull NotificationItem notification) {
        Long id = notification.getId();
        if (id == null || id <= 0L) {
            return;
        }

        repository.markAsRead(id, new NotificationRepository.MarkAsReadCallback() {
            @Override
            public void onSuccess() {
                // Reload to update 'read' state + unread counter
                loadNotifications();
            }

            @Override
            public void onError(@Nullable Throwable throwable,
                                @Nullable Integer httpCode,
                                @Nullable String errorBody) {
                String msg = "Failed to mark notification as read.";
                if (httpCode != null) {
                    msg = msg + " Code: " + httpCode;
                }
                if (errorBody != null && !errorBody.isEmpty()) {
                    msg = msg + " " + errorBody;
                }
                errorMessage.postValue(msg);
            }
        });
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private int calculateUnreadCount(@NonNull List<NotificationItem> list) {
        int count = 0;
        for (NotificationItem n : list) {
            if (n == null) continue;
            if (!n.isRead()) {
                count++;
            }
        }
        return count;
    }
}
