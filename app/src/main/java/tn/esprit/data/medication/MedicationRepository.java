package tn.esprit.data.medication;

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
import tn.esprit.data.remote.medication.MedicationApiService;
import tn.esprit.domain.medication.Medication;

/**
 * Repository for medication catalog operations.
 *
 * Current scope:
 *  - Load list of medications (optionally filtered by query) for doctor flows.
 */
public class MedicationRepository {

    private final MedicationApiService apiService;

    public MedicationRepository(@NonNull Context context) {
        // Context kept for future caching if needed
        this.apiService = ApiClient.createService(MedicationApiService.class);
    }

    // ---------------------------------------------------------------------
    // Load medications
    // ---------------------------------------------------------------------

    public interface MedicationsCallback {
        void onSuccess(List<Medication> medications);

        void onError(@Nullable Throwable throwable,
                     @Nullable Integer httpCode,
                     @Nullable String errorBody);
    }

    /**
     * Retrieve list of medications.
     *
     * @param query optional filter; null for full list.
     */
    public void getMedications(@Nullable String query,
                               @NonNull MedicationsCallback callback) {

        Call<ListResponseDto<Medication>> call = apiService.getMedications(query);

        call.enqueue(new Callback<ListResponseDto<Medication>>() {
            @Override
            public void onResponse(
                    @NonNull Call<ListResponseDto<Medication>> call,
                    @NonNull Response<ListResponseDto<Medication>> response
            ) {
                if (!response.isSuccessful()) {
                    callback.onError(null, response.code(), safeErrorBody(response.errorBody()));
                    return;
                }

                ListResponseDto<Medication> body = response.body();
                List<Medication> items =
                        (body != null && body.getItems() != null)
                                ? body.getItems()
                                : Collections.emptyList();

                callback.onSuccess(items);
            }

            @Override
            public void onFailure(
                    @NonNull Call<ListResponseDto<Medication>> call,
                    @NonNull Throwable t
            ) {
                callback.onError(t, null, null);
            }
        });
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

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
