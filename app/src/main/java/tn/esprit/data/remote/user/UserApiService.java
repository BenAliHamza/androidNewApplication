package tn.esprit.data.remote.user;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import tn.esprit.domain.user.User;

/**
 * Retrofit API for general user operations.
 *
 * We directly return the domain User model to avoid extra mapping.
 */
public interface UserApiService {

    /**
     * GET /me
     * Authorization: Bearer <access_token>
     */
    @GET("me")
    Call<User> getCurrentUser(@Header("Authorization") String authorization);
}
