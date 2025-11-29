package tn.esprit.presentation.history;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.Collections;
import java.util.List;

import tn.esprit.data.history.UserHistoryRepository;
import tn.esprit.domain.history.UserHistoryEntry;

/**
 * ViewModel for the user's history screen.
 * Wraps UserHistoryRepository and exposes LiveData for the fragment.
 */
public class UserHistoryViewModel extends AndroidViewModel {

    private final UserHistoryRepository repository;

    private final MutableLiveData<List<UserHistoryEntry>> historyEntries =
            new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<Boolean> loading =
            new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage =
            new MutableLiveData<>(null);

    public UserHistoryViewModel(@NonNull Application application) {
        super(application);
        repository = new UserHistoryRepository(application.getApplicationContext());
    }

    // ---------------------------------------------------------------------
    // LiveData getters
    // ---------------------------------------------------------------------

    /** Preferred name used by the fragment. */
    public LiveData<List<UserHistoryEntry>> getHistoryItems() {
        return historyEntries;
    }

    /** Alias (just in case something else used this name). */
    public LiveData<List<UserHistoryEntry>> getHistoryEntries() {
        return historyEntries;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void clearError() {
        errorMessage.setValue(null);
    }

    // ---------------------------------------------------------------------
    // Loading
    // ---------------------------------------------------------------------

    /**
     * Reloads history from backend (used on initial load + pull-to-refresh).
     */
    public void reloadHistory() {
        loading.setValue(true);
        errorMessage.setValue(null);

        repository.loadHistory(new UserHistoryRepository.LoadCallback() {
            @Override
            public void onSuccess(List<UserHistoryEntry> items) {
                loading.postValue(false);
                historyEntries.postValue(
                        items != null ? items : Collections.emptyList()
                );
            }

            @Override
            public void onError(@Nullable Throwable throwable,
                                @Nullable Integer httpCode,
                                @Nullable String errorBody) {
                loading.postValue(false);
                errorMessage.postValue("Failed to load history.");
            }
        });
    }

    /** Friendly alias for initial load from Fragment. */
    public void loadHistory() {
        reloadHistory();
    }

    /** Alias for SwipeRefreshLayout refresh. */
    public void refreshHistory() {
        reloadHistory();
    }
}
