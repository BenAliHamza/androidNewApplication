package tn.esprit.data.profile;

import android.content.Context;

import androidx.annotation.Nullable;

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
 * Central place to load and update the current user's profile:
 *
 *  - GET /me                             → User
 *  - GET /api/doctors/me                 → DoctorProfile (when role = DOCTOR)
 *  - GET /patients/me                    → PatientProfile (when role = PATIENT)
 *  - PUT /api/doctors/me                 → update doctor profile
 *  - PUT /patients/me                    → update patient profile
 *  - PUT /me                             → update base user info
 *  - POST /users/me/profile-image        → upload avatar image
 *
 * All calls use the AuthLocalDataSource to fetch the current access token.
 */
public class ProfileRepository {

    private final Context appContext;
    private final AuthLocalDataSource authLocalDataSource;

    private final UserApiService userApiService;
    private final DoctorApiService doctorApiService;
    private final PatientApiService patientApiService;
    private final UserAccountApiService userAccountApiService;
    private final UserImageApiService userImageApiService;

    public ProfileRepository(Context context) {
        this.appContext = context.getApplicationContext();
        this.authLocalDataSource = new AuthLocalDataSource(appContext);

        this.userApiService = ApiClient.createService(UserApiService.class);
        this.doctorApiService = ApiClient.createService(DoctorApiService.class);
        this.patientApiService = ApiClient.createService(PatientApiService.class);
        this.userAccountApiService = ApiClient.createService(UserAccountApiService.class);
        this.userImageApiService = ApiClient.createService(UserImageApiService.class);
    }

    // ------------------------------------------------------------
    // Callbacks
    // ------------------------------------------------------------

    public interface ProfileCallback {
        void onSuccess(User user,
                       @Nullable DoctorProfile doctorProfile,
                       @Nullable PatientProfile patientProfile);

        void onError(Throwable throwable,
                     @Nullable Integer httpCode,
                     @Nullable String errorBody);
    }

    public interface DoctorProfileUpdateCallback {
        void onSuccess(DoctorProfile updatedProfile);

        void onError(Throwable throwable,
                     @Nullable Integer httpCode,
                     @Nullable String errorBody);
    }

    public interface PatientProfileUpdateCallback {
        void onSuccess(PatientProfile updatedProfile);

        void onError(Throwable throwable,
                     @Nullable Integer httpCode,
                     @Nullable String errorBody);
    }

    public interface BaseUserUpdateCallback {
        void onSuccess(User updatedUser);

        void onError(Throwable throwable,
                     @Nullable Integer httpCode,
                     @Nullable String errorBody);
    }

    public interface ProfileImageUpdateCallback {
        void onSuccess(User updatedUser);

        void onError(Throwable throwable,
                     @Nullable Integer httpCode,
                     @Nullable String errorBody);
    }

    // ------------------------------------------------------------
    // Load full profile (User + Doctor/Patient depending on role)
    // ------------------------------------------------------------

    public void loadProfile(ProfileCallback callback) {
        AuthTokens tokens = authLocalDataSource.getTokens();
        if (tokens == null || tokens.getAccessToken() == null) {
            if (callback != null) {
                callback.onError(null, 401, "Not authenticated");
            }
            return;
        }

        final String authHeader = "Bearer " + tokens.getAccessToken();

        userApiService.getCurrentUser(authHeader).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call,
                                   Response<User> response) {
                if (callback == null) return;

                if (!response.isSuccessful()) {
                    callback.onError(
                            null,
                            response.code(),
                            safeErrorBody(response)
                    );
                    return;
                }

                User user = response.body();
                if (user == null) {
                    callback.onError(
                            null,
                            response.code(),
                            "Empty body from /me"
                    );
                    return;
                }

                String role = user.getRole();
                if (role != null && "DOCTOR".equalsIgnoreCase(role)) {
                    fetchDoctorProfile(user, authHeader, callback);
                } else if (role != null && "PATIENT".equalsIgnoreCase(role)) {
                    fetchPatientProfile(user, authHeader, callback);
                } else {
                    callback.onSuccess(user, null, null);
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                if (callback != null) {
                    callback.onError(t, null, null);
                }
            }
        });
    }

    private void fetchDoctorProfile(User user,
                                    String authHeader,
                                    ProfileCallback callback) {
        doctorApiService.getMyProfile(authHeader).enqueue(new Callback<DoctorProfile>() {
            @Override
            public void onResponse(Call<DoctorProfile> call,
                                   Response<DoctorProfile> response) {
                if (callback == null) return;

                if (!response.isSuccessful()) {
                    // If doctor profile is not yet created, backend may return 404.
                    if (response.code() == 404) {
                        callback.onSuccess(user, null, null);
                    } else {
                        callback.onError(
                                null,
                                response.code(),
                                safeErrorBody(response)
                        );
                    }
                    return;
                }

                callback.onSuccess(user, response.body(), null);
            }

            @Override
            public void onFailure(Call<DoctorProfile> call, Throwable t) {
                if (callback != null) {
                    callback.onError(t, null, null);
                }
            }
        });
    }

    private void fetchPatientProfile(User user,
                                     String authHeader,
                                     ProfileCallback callback) {
        patientApiService.getMyProfile(authHeader).enqueue(new Callback<PatientProfile>() {
            @Override
            public void onResponse(Call<PatientProfile> call,
                                   Response<PatientProfile> response) {
                if (callback == null) return;

                if (!response.isSuccessful()) {
                    // If patient profile is not yet created, backend may return 404.
                    if (response.code() == 404) {
                        callback.onSuccess(user, null, null);
                    } else {
                        callback.onError(
                                null,
                                response.code(),
                                safeErrorBody(response)
                        );
                    }
                    return;
                }

                callback.onSuccess(user, null, response.body());
            }

            @Override
            public void onFailure(Call<PatientProfile> call, Throwable t) {
                if (callback != null) {
                    callback.onError(t, null, null);
                }
            }
        });
    }

    // ------------------------------------------------------------
    // Doctor profile update
    // ------------------------------------------------------------

    public void updateDoctorProfile(DoctorProfileUpdateRequestDto request,
                                    DoctorProfileUpdateCallback callback) {
        AuthTokens tokens = authLocalDataSource.getTokens();
        if (tokens == null || tokens.getAccessToken() == null) {
            if (callback != null) {
                callback.onError(null, 401, "Not authenticated");
            }
            return;
        }
        String authHeader = "Bearer " + tokens.getAccessToken();

        doctorApiService.updateMyProfile(authHeader, request)
                .enqueue(new Callback<DoctorProfile>() {
                    @Override
                    public void onResponse(Call<DoctorProfile> call,
                                           Response<DoctorProfile> response) {
                        if (callback == null) return;

                        if (!response.isSuccessful()) {
                            callback.onError(
                                    null,
                                    response.code(),
                                    safeErrorBody(response)
                            );
                            return;
                        }
                        callback.onSuccess(response.body());
                    }

                    @Override
                    public void onFailure(Call<DoctorProfile> call, Throwable t) {
                        if (callback != null) {
                            callback.onError(t, null, null);
                        }
                    }
                });
    }

    // ------------------------------------------------------------
    // Patient profile update
    // ------------------------------------------------------------

    public void updatePatientProfile(PatientProfileUpdateRequestDto request,
                                     PatientProfileUpdateCallback callback) {
        AuthTokens tokens = authLocalDataSource.getTokens();
        if (tokens == null || tokens.getAccessToken() == null) {
            if (callback != null) {
                callback.onError(null, 401, "Not authenticated");
            }
            return;
        }
        String authHeader = "Bearer " + tokens.getAccessToken();

        patientApiService.updateMyProfile(authHeader, request)
                .enqueue(new Callback<PatientProfile>() {
                    @Override
                    public void onResponse(Call<PatientProfile> call,
                                           Response<PatientProfile> response) {
                        if (callback == null) return;

                        if (!response.isSuccessful()) {
                            callback.onError(
                                    null,
                                    response.code(),
                                    safeErrorBody(response)
                            );
                            return;
                        }
                        callback.onSuccess(response.body());
                    }

                    @Override
                    public void onFailure(Call<PatientProfile> call, Throwable t) {
                        if (callback != null) {
                            callback.onError(t, null, null);
                        }
                    }
                });
    }

    // ------------------------------------------------------------
    // Base user update (/me)
    // ------------------------------------------------------------

    public void updateBaseUser(UserUpdateRequestDto request,
                               BaseUserUpdateCallback callback) {
        AuthTokens tokens = authLocalDataSource.getTokens();
        if (tokens == null || tokens.getAccessToken() == null) {
            if (callback != null) {
                callback.onError(null, 401, "Not authenticated");
            }
            return;
        }
        String authHeader = "Bearer " + tokens.getAccessToken();

        userAccountApiService.updateCurrentUser(authHeader, request)
                .enqueue(new Callback<User>() {
                    @Override
                    public void onResponse(Call<User> call,
                                           Response<User> response) {
                        if (callback == null) return;

                        if (!response.isSuccessful()) {
                            callback.onError(
                                    null,
                                    response.code(),
                                    safeErrorBody(response)
                            );
                            return;
                        }
                        callback.onSuccess(response.body());
                    }

                    @Override
                    public void onFailure(Call<User> call, Throwable t) {
                        if (callback != null) {
                            callback.onError(t, null, null);
                        }
                    }
                });
    }

    // ------------------------------------------------------------
    // Doctor practice setup (onboarding)
    // ------------------------------------------------------------

    public void setupDoctorPractice(DoctorPracticeSetupRequestDto request,
                                    DoctorProfileUpdateCallback callback) {
        AuthTokens tokens = authLocalDataSource.getTokens();
        if (tokens == null || tokens.getAccessToken() == null) {
            if (callback != null) {
                callback.onError(null, 401, "Not authenticated");
            }
            return;
        }
        String authHeader = "Bearer " + tokens.getAccessToken();

        doctorApiService.setupPracticeForCurrentDoctor(authHeader, request)
                .enqueue(new Callback<DoctorProfile>() {
                    @Override
                    public void onResponse(Call<DoctorProfile> call,
                                           Response<DoctorProfile> response) {
                        if (callback == null) return;

                        if (!response.isSuccessful()) {
                            callback.onError(
                                    null,
                                    response.code(),
                                    safeErrorBody(response)
                            );
                            return;
                        }
                        callback.onSuccess(response.body());
                    }

                    @Override
                    public void onFailure(Call<DoctorProfile> call, Throwable t) {
                        if (callback != null) {
                            callback.onError(t, null, null);
                        }
                    }
                });
    }

    // ------------------------------------------------------------
    // Profile image upload
    // ------------------------------------------------------------

    public void uploadProfileImage(MultipartBody.Part imagePart,
                                   ProfileImageUpdateCallback callback) {
        if (imagePart == null) {
            if (callback != null) {
                callback.onError(
                        null,
                        null,
                        "Image part is null"
                );
            }
            return;
        }

        AuthTokens tokens = authLocalDataSource.getTokens();
        if (tokens == null || tokens.getAccessToken() == null) {
            if (callback != null) {
                callback.onError(null, 401, "Not authenticated");
            }
            return;
        }
        String authHeader = "Bearer " + tokens.getAccessToken();

        userImageApiService.uploadMyProfileImage(authHeader, imagePart)
                .enqueue(new Callback<User>() {
                    @Override
                    public void onResponse(Call<User> call,
                                           Response<User> response) {
                        if (callback == null) return;

                        if (!response.isSuccessful()) {
                            callback.onError(
                                    null,
                                    response.code(),
                                    safeErrorBody(response)
                            );
                            return;
                        }
                        callback.onSuccess(response.body());
                    }

                    @Override
                    public void onFailure(Call<User> call, Throwable t) {
                        if (callback != null) {
                            callback.onError(t, null, null);
                        }
                    }
                });
    }

    // ------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------

    @Nullable
    private String safeErrorBody(Response<?> response) {
        try {
            if (response.errorBody() == null) return null;
            return response.errorBody().string();
        } catch (Exception e) {
            return null;
        }
    }
}
