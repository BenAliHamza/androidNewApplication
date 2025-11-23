package tn.esprit.data.profile;

import android.content.Context;

import java.io.IOException;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import tn.esprit.data.auth.AuthLocalDataSource;
import tn.esprit.data.remote.ApiClient;
import tn.esprit.data.remote.doctor.DoctorApiService;
import tn.esprit.data.remote.doctor.DoctorApiService.DoctorPracticeSetupRequestDto;
import tn.esprit.data.remote.doctor.DoctorApiService.DoctorProfileUpdateRequestDto;
import tn.esprit.data.remote.patient.PatientApiService;
import tn.esprit.data.remote.patient.PatientApiService.PatientProfileUpdateRequestDto;
import tn.esprit.data.remote.user.UserAccountApiService;
import tn.esprit.data.remote.user.UserAccountApiService.UserUpdateRequestDto;
import tn.esprit.data.remote.user.UserApiService;
import tn.esprit.data.remote.user.UserImageApiService;
import tn.esprit.domain.auth.AuthTokens;
import tn.esprit.domain.doctor.DoctorProfile;
import tn.esprit.domain.patient.PatientProfile;
import tn.esprit.domain.user.User;

/**
 * Repository responsible for loading the current user's profile
 * (User + DoctorProfile or PatientProfile depending on role).
 *
 * Also exposes update methods for doctor profile, patient profile,
 * base user (/me) information and profile image.
 *
 * Note: we now use the domain models directly as Retrofit response types,
 * so there is no extra mapping layer.
 */
public class ProfileRepository {

    private final AuthLocalDataSource authLocalDataSource;
    private final UserApiService userApiService;
    private final DoctorApiService doctorApiService;
    private final PatientApiService patientApiService;
    private final UserAccountApiService userAccountApiService;
    private final UserImageApiService userImageApiService;

    public ProfileRepository(Context context) {
        Context appContext = context.getApplicationContext();
        this.authLocalDataSource = new AuthLocalDataSource(appContext);
        this.userApiService = ApiClient.createService(UserApiService.class);
        this.doctorApiService = ApiClient.createService(DoctorApiService.class);
        this.patientApiService = ApiClient.createService(PatientApiService.class);
        this.userAccountApiService = ApiClient.createService(UserAccountApiService.class);
        this.userImageApiService = ApiClient.createService(UserImageApiService.class);
    }

    public interface ProfileCallback {
        void onSuccess(User user, DoctorProfile doctorProfile, PatientProfile patientProfile);
        void onError(Throwable throwable, Integer httpCode, String errorBody);
    }

    public interface DoctorProfileUpdateCallback {
        void onSuccess(DoctorProfile updatedProfile);
        void onError(Throwable throwable, Integer httpCode, String errorBody);
    }

    public interface PatientProfileUpdateCallback {
        void onSuccess(PatientProfile updatedProfile);
        void onError(Throwable throwable, Integer httpCode, String errorBody);
    }

    public interface BaseUserUpdateCallback {
        void onSuccess(User updatedUser);
        void onError(Throwable throwable, Integer httpCode, String errorBody);
    }

    public interface ProfileImageUpdateCallback {
        void onSuccess(User updatedUser);
        void onError(Throwable throwable, Integer httpCode, String errorBody);
    }

    /**
     * Load current user + (doctor or patient) profile, depending on role.
     */
    public void loadProfile(final ProfileCallback callback) {
        AuthTokens tokens = authLocalDataSource.getTokens();
        if (tokens == null || tokens.getAccessToken() == null) {
            callback.onError(new IllegalStateException("Not authenticated"), null, null);
            return;
        }

        final String authHeader = buildAuthHeader(tokens);

        // 1) Load the base User (/me)
        userApiService.getCurrentUser(authHeader).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call,
                                   Response<User> response) {
                if (!response.isSuccessful()) {
                    String errorBody = extractErrorBody(response);
                    callback.onError(null, response.code(), errorBody);
                    return;
                }

                User user = response.body();
                if (user == null) {
                    callback.onError(new IllegalStateException("Empty user body"),
                            response.code(), null);
                    return;
                }

                String role = user.getRole() != null ? user.getRole() : "";

                // 2) Depending on the role, load doctor or patient profile.
                if ("DOCTOR".equalsIgnoreCase(role)) {
                    loadDoctorProfile(authHeader, user, callback);
                } else if ("PATIENT".equalsIgnoreCase(role)) {
                    loadPatientProfile(authHeader, user, callback);
                } else {
                    // Unknown or admin role -> return only User
                    callback.onSuccess(user, null, null);
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                callback.onError(t, null, null);
            }
        });
    }

    /**
     * Update the current doctor's profile via /api/doctors/me.
     */
    public void updateDoctorProfile(DoctorProfileUpdateRequestDto request,
                                    final DoctorProfileUpdateCallback callback) {
        AuthTokens tokens = authLocalDataSource.getTokens();
        if (tokens == null || tokens.getAccessToken() == null) {
            callback.onError(new IllegalStateException("Not authenticated"), null, null);
            return;
        }

        String authHeader = buildAuthHeader(tokens);

        doctorApiService.updateMyProfile(authHeader, request)
                .enqueue(new Callback<DoctorProfile>() {
                    @Override
                    public void onResponse(Call<DoctorProfile> call,
                                           Response<DoctorProfile> response) {
                        if (!response.isSuccessful()) {
                            String errorBody = extractErrorBody(response);
                            callback.onError(null, response.code(), errorBody);
                            return;
                        }

                        DoctorProfile body = response.body();
                        if (body == null) {
                            callback.onError(
                                    new IllegalStateException("Empty profile body"),
                                    response.code(),
                                    null
                            );
                            return;
                        }

                        callback.onSuccess(body);
                    }

                    @Override
                    public void onFailure(Call<DoctorProfile> call, Throwable t) {
                        callback.onError(t, null, null);
                    }
                });
    }

    /**
     * Update the current patient's profile via /patients/me.
     */
    public void updatePatientProfile(PatientProfileUpdateRequestDto request,
                                     final PatientProfileUpdateCallback callback) {
        AuthTokens tokens = authLocalDataSource.getTokens();
        if (tokens == null || tokens.getAccessToken() == null) {
            callback.onError(new IllegalStateException("Not authenticated"), null, null);
            return;
        }

        String authHeader = buildAuthHeader(tokens);

        patientApiService.updateMyProfile(authHeader, request)
                .enqueue(new Callback<PatientProfile>() {
                    @Override
                    public void onResponse(Call<PatientProfile> call,
                                           Response<PatientProfile> response) {
                        if (!response.isSuccessful()) {
                            String errorBody = extractErrorBody(response);
                            callback.onError(null, response.code(), errorBody);
                            return;
                        }

                        PatientProfile body = response.body();
                        if (body == null) {
                            callback.onError(
                                    new IllegalStateException("Empty patient profile body"),
                                    response.code(),
                                    null
                            );
                            return;
                        }

                        callback.onSuccess(body);
                    }

                    @Override
                    public void onFailure(Call<PatientProfile> call, Throwable t) {
                        callback.onError(t, null, null);
                    }
                });
    }

    /**
     * Update base user information (/me) via UserAccountApiService.
     */
    public void updateBaseUser(UserUpdateRequestDto request,
                               final BaseUserUpdateCallback callback) {
        AuthTokens tokens = authLocalDataSource.getTokens();
        if (tokens == null || tokens.getAccessToken() == null) {
            callback.onError(new IllegalStateException("Not authenticated"), null, null);
            return;
        }

        String authHeader = buildAuthHeader(tokens);

        userAccountApiService.updateCurrentUser(authHeader, request)
                .enqueue(new Callback<User>() {
                    @Override
                    public void onResponse(Call<User> call, Response<User> response) {
                        if (!response.isSuccessful()) {
                            String errorBody = extractErrorBody(response);
                            callback.onError(null, response.code(), errorBody);
                            return;
                        }

                        User body = response.body();
                        if (body == null) {
                            callback.onError(
                                    new IllegalStateException("Empty user body"),
                                    response.code(),
                                    null
                            );
                            return;
                        }

                        callback.onSuccess(body);
                    }

                    @Override
                    public void onFailure(Call<User> call, Throwable t) {
                        callback.onError(t, null, null);
                    }
                });
    }

    /**
     * Upload user profile image via /users/me/profile-image.
     */
    public void uploadProfileImage(MultipartBody.Part imagePart,
                                   final ProfileImageUpdateCallback callback) {
        AuthTokens tokens = authLocalDataSource.getTokens();
        if (tokens == null || tokens.getAccessToken() == null) {
            callback.onError(new IllegalStateException("Not authenticated"), null, null);
            return;
        }

        String authHeader = buildAuthHeader(tokens);

        userImageApiService.uploadMyProfileImage(authHeader, imagePart)
                .enqueue(new Callback<User>() {
                    @Override
                    public void onResponse(Call<User> call, Response<User> response) {
                        if (!response.isSuccessful()) {
                            String errorBody = extractErrorBody(response);
                            callback.onError(null, response.code(), errorBody);
                            return;
                        }

                        User body = response.body();
                        if (body == null) {
                            callback.onError(
                                    new IllegalStateException("Empty user body"),
                                    response.code(),
                                    null
                            );
                            return;
                        }

                        callback.onSuccess(body);
                    }

                    @Override
                    public void onFailure(Call<User> call, Throwable t) {
                        callback.onError(t, null, null);
                    }
                });
    }

    // ----------------- Internals -----------------

    private void loadDoctorProfile(String authHeader,
                                   final User user,
                                   final ProfileCallback callback) {

        doctorApiService.getMyProfile(authHeader)
                .enqueue(new Callback<DoctorProfile>() {
                    @Override
                    public void onResponse(Call<DoctorProfile> call,
                                           Response<DoctorProfile> response) {
                        if (!response.isSuccessful()) {
                            String errorBody = extractErrorBody(response);
                            callback.onError(null, response.code(), errorBody);
                            return;
                        }

                        DoctorProfile dto = response.body();
                        // If null, still return user with null profile
                        callback.onSuccess(user, dto, null);
                    }

                    @Override
                    public void onFailure(Call<DoctorProfile> call, Throwable t) {
                        callback.onError(t, null, null);
                    }
                });
    }

    private void loadPatientProfile(String authHeader,
                                    final User user,
                                    final ProfileCallback callback) {

        patientApiService.getMyProfile(authHeader)
                .enqueue(new Callback<PatientProfile>() {
                    @Override
                    public void onResponse(Call<PatientProfile> call,
                                           Response<PatientProfile> response) {
                        if (!response.isSuccessful()) {
                            String errorBody = extractErrorBody(response);
                            callback.onError(null, response.code(), errorBody);
                            return;
                        }

                        PatientProfile dto = response.body();
                        callback.onSuccess(user, null, dto);
                    }

                    @Override
                    public void onFailure(Call<PatientProfile> call, Throwable t) {
                        callback.onError(t, null, null);
                    }
                });
    }

    private String buildAuthHeader(AuthTokens tokens) {
        String type = tokens.getTokenType() != null ? tokens.getTokenType() : "Bearer";
        return type + " " + tokens.getAccessToken();
    }

    private String extractErrorBody(Response<?> response) {
        try {
            if (response.errorBody() != null) {
                return response.errorBody().string();
            }
        } catch (IOException ignored) {
        }
        return null;
    }
}
