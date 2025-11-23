package tn.esprit.presentation.home;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.List;

import tn.esprit.data.doctor.DoctorDirectoryRepository;
import tn.esprit.domain.doctor.DoctorSearchFilters;
import tn.esprit.domain.doctor.DoctorSearchResult;

public class PatientHomeViewModel extends AndroidViewModel {

    private static final int MAX_RESULTS = 5;

    private final DoctorDirectoryRepository doctorDirectoryRepository;

    private final MutableLiveData<String> query = new MutableLiveData<>("");
    private final MutableLiveData<List<DoctorSearchResult>> results = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> hasSearched = new MutableLiveData<>(false);

    public PatientHomeViewModel(@NonNull Application application) {
        super(application);
        doctorDirectoryRepository = new DoctorDirectoryRepository(application.getApplicationContext());
    }

    public LiveData<String> getQuery() {
        return query;
    }

    public LiveData<List<DoctorSearchResult>> getResults() {
        return results;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Boolean> getHasSearched() {
        return hasSearched;
    }

    /**
     * Called when the search text changes and user stops typing (debounced in Fragment).
     */
    public void searchDoctors(String rawQuery) {
        String trimmed = rawQuery != null ? rawQuery.trim() : "";
        query.setValue(trimmed);

        // Now search as soon as there is at least 1 character.
        if (trimmed.length() < 1) {
            loading.setValue(false);
            errorMessage.setValue(null);
            results.setValue(new ArrayList<>());
            hasSearched.setValue(false);
            return;
        }

        loading.setValue(true);
        errorMessage.setValue(null);

        DoctorSearchFilters filters = DoctorSearchFilters.fromQuery(trimmed);

        doctorDirectoryRepository.searchDoctors(filters, new DoctorDirectoryRepository.SearchCallback() {
            @Override
            public void onSuccess(List<DoctorSearchResult> searchResults) {
                loading.postValue(false);
                hasSearched.postValue(true);

                if (searchResults == null) {
                    results.postValue(new ArrayList<>());
                    return;
                }

                List<DoctorSearchResult> limited;
                if (searchResults.size() > MAX_RESULTS) {
                    limited = new ArrayList<>(searchResults.subList(0, MAX_RESULTS));
                } else {
                    limited = new ArrayList<>(searchResults);
                }

                results.postValue(limited);
            }

            @Override
            public void onError(Throwable throwable, Integer httpCode, String errorBody) {
                loading.postValue(false);
                hasSearched.postValue(true);

                String msg = null;
                if (throwable != null) {
                    msg = "Network error while searching for doctors.";
                } else if (httpCode != null) {
                    msg = "Search failed with code " + httpCode;
                }

                if (errorBody != null && !errorBody.isEmpty()) {
                    if (msg == null) {
                        msg = errorBody;
                    } else {
                        msg = msg + " " + errorBody;
                    }
                }

                errorMessage.postValue(msg);
            }
        });
    }

    public void clearSearch() {
        query.setValue("");
        results.setValue(new ArrayList<>());
        loading.setValue(false);
        errorMessage.setValue(null);
        hasSearched.setValue(false);
    }
}
