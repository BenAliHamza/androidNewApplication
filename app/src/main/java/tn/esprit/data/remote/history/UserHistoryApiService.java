package tn.esprit.data.remote.history;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import tn.esprit.domain.history.UserHistoryEntry;

/**
 * Retrofit API for user history.
 *
 * Backend:
 *   GET /api/history
 *   Authorization: Bearer <token>
 *
 * Currently no query params are used on the Android side.
 */
public interface UserHistoryApiService {

    @GET("/api/history")
    Call<List<UserHistoryEntry>> getMyHistory(
            @Header("Authorization") String authHeader
    );
}
