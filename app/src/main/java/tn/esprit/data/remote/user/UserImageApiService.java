package tn.esprit.data.remote.user;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import tn.esprit.domain.user.User;

/**
 * Retrofit service for user profile image operations.
 *
 * Matches backend:
 *   POST /users/me/profile-image
 *   multipart/form-data, field name = "image"
 *   returns UserDto → mapped to tn.esprit.domain.user.User
 */
public interface UserImageApiService {

    @Multipart
    @POST("/users/me/profile-image")
    Call<User> uploadMyProfileImage(
            @Header("Authorization") String authHeader,
            @Part MultipartBody.Part image
    );
}
