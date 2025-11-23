package tn.esprit.data.remote;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;
import tn.esprit.data.remote.appointment.AppointmentApiService;
import android.content.Context;

/**
 * Provides a configured Retrofit instance to talk to the Spring Boot backend.
 * Uses 10.0.2.2 so the Android emulator can reach localhost:8080 on your machine.
 *
 * NOTE: This client is intentionally small and focused on auth for now.
 * Later we can split it into separate files (AuthApi, UserApi, DoctorApi...)
 * to avoid it becoming a god-class.
 */
public class ApiClient {

    private static final String BASE_URL = "http://10.0.2.2:8080/";
    private static Retrofit retrofit;

    private ApiClient() {
        // No instances
    }

    public static AppointmentApiService getAppointmentApiService(Context context) {
        return getRetrofitWithAuth(context).create(AppointmentApiService.class);
    }
    public static <T> T createService(Class<T> serviceClass) {
        return getRetrofit().create(serviceClass);
    }

    private static Retrofit getRetrofit() {
        if (retrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            // Always log BODY for now (you can change to NONE for production)
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    public static AuthApiService getAuthApiService() {
        return getRetrofit().create(AuthApiService.class);
    }

    /**
     * Retrofit API for /auth endpoints.
     */
    public interface AuthApiService {

        @POST("auth/login")
        Call<TokenResponseDto> login(@Body LoginRequestDto request);

        @POST("auth/signup")
        Call<TokenResponseDto> signup(@Body SignupRequestDto request);

        @POST("auth/refresh")
        Call<TokenResponseDto> refresh(@Body RefreshTokenRequestDto request);
    }

    /**
     * Matches backend dto.auth.LoginRequest (email, password).
     */
    public static class LoginRequestDto {
        private String email;
        private String password;

        public LoginRequestDto(String email, String password) {
            this.email = email;
            this.password = password;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    /**
     * Matches backend dto.auth.SignupRequest
     * (firstname, lastname, email, phone, password, role).
     */
    public static class SignupRequestDto {
        private String firstname;
        private String lastname;
        private String email;
        private String phone;
        private String password;
        private String role; // "DOCTOR" or "PATIENT"

        public SignupRequestDto(String firstname,
                                String lastname,
                                String email,
                                String phone,
                                String password,
                                String role) {
            this.firstname = firstname;
            this.lastname = lastname;
            this.email = email;
            this.phone = phone;
            this.password = password;
            this.role = role;
        }

        public String getFirstname() {
            return firstname;
        }

        public void setFirstname(String firstname) {
            this.firstname = firstname;
        }

        public String getLastname() {
            return lastname;
        }

        public void setLastname(String lastname) {
            this.lastname = lastname;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }
    }

    /**
     * Matches backend dto.auth.RefreshTokenRequest (refreshToken).
     */
    public static class RefreshTokenRequestDto {
        private String refreshToken;

        public RefreshTokenRequestDto(String refreshToken) {
            this.refreshToken = refreshToken;
        }

        public String getRefreshToken() {
            return refreshToken;
        }

        public void setRefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
        }
    }

    /**
     * Matches backend dto.auth.TokenResponse
     * (accessToken, refreshToken, tokenType, expiresIn).
     */
    public static class TokenResponseDto {
        private String accessToken;
        private String refreshToken;
        private String tokenType;
        private Long expiresIn;

        public String getAccessToken() {
            return accessToken;
        }

        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }

        public String getRefreshToken() {
            return refreshToken;
        }

        public void setRefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
        }

        public String getTokenType() {
            return tokenType;
        }

        public void setTokenType(String tokenType) {
            this.tokenType = tokenType;
        }

        public Long getExpiresIn() {
            return expiresIn;
        }

        public void setExpiresIn(Long expiresIn) {
            this.expiresIn = expiresIn;
        }
    }
}
