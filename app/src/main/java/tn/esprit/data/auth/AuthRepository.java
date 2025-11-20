package tn.esprit.data.auth;

import tn.esprit.data.remote.ApiClient;
import tn.esprit.data.remote.ApiClient.AuthApiService;
import tn.esprit.data.remote.ApiClient.LoginRequestDto;
import tn.esprit.data.remote.ApiClient.RefreshTokenRequestDto;
import tn.esprit.data.remote.ApiClient.SignupRequestDto;
import tn.esprit.data.remote.ApiClient.TokenResponseDto;
import tn.esprit.domain.auth.AuthTokens;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Auth repository bridging presentation/domain with the remote /auth endpoints.
 */
public class AuthRepository {

    private final AuthApiService authApiService;

    public AuthRepository() {
        this.authApiService = ApiClient.getAuthApiService();
    }

    public interface LoginCallback {
        void onSuccess(AuthTokens tokens);
        void onError(Throwable throwable, Integer httpCode, String errorBody);
    }

    public interface SignupCallback {
        void onSuccess(AuthTokens tokens);
        void onError(Throwable throwable, Integer httpCode, String errorBody);
    }

    public interface RefreshCallback {
        void onSuccess(AuthTokens tokens);
        void onError(Throwable throwable, Integer httpCode, String errorBody);
    }

    /**
     * Calls /auth/login with the given credentials.
     */
    public void login(String email, String password, final LoginCallback callback) {
        LoginRequestDto request = new LoginRequestDto(email, password);

        authApiService.login(request).enqueue(new Callback<TokenResponseDto>() {
            @Override
            public void onResponse(Call<TokenResponseDto> call, Response<TokenResponseDto> response) {
                handleTokenResponse(response, new InternalCallback() {
                    @Override
                    public void onSuccess(AuthTokens tokens) {
                        callback.onSuccess(tokens);
                    }

                    @Override
                    public void onError(Throwable throwable, Integer httpCode, String errorBody) {
                        callback.onError(throwable, httpCode, errorBody);
                    }
                });
            }

            @Override
            public void onFailure(Call<TokenResponseDto> call, Throwable t) {
                callback.onError(t, null, null);
            }
        });
    }

    /**
     * Calls /auth/signup with the given registration data.
     */
    public void signup(String firstname,
                       String lastname,
                       String email,
                       String phone,
                       String password,
                       String role,
                       final SignupCallback callback) {

        SignupRequestDto request = new SignupRequestDto(
                firstname,
                lastname,
                email,
                phone,
                password,
                role
        );

        authApiService.signup(request).enqueue(new Callback<TokenResponseDto>() {
            @Override
            public void onResponse(Call<TokenResponseDto> call, Response<TokenResponseDto> response) {
                handleTokenResponse(response, new InternalCallback() {
                    @Override
                    public void onSuccess(AuthTokens tokens) {
                        callback.onSuccess(tokens);
                    }

                    @Override
                    public void onError(Throwable throwable, Integer httpCode, String errorBody) {
                        callback.onError(throwable, httpCode, errorBody);
                    }
                });
            }

            @Override
            public void onFailure(Call<TokenResponseDto> call, Throwable t) {
                callback.onError(t, null, null);
            }
        });
    }

    /**
     * Calls /auth/refresh with the given refresh token.
     */
    public void refreshToken(String refreshToken, final RefreshCallback callback) {
        RefreshTokenRequestDto request = new RefreshTokenRequestDto(refreshToken);

        authApiService.refresh(request).enqueue(new Callback<TokenResponseDto>() {
            @Override
            public void onResponse(Call<TokenResponseDto> call, Response<TokenResponseDto> response) {
                handleTokenResponse(response, new InternalCallback() {
                    @Override
                    public void onSuccess(AuthTokens tokens) {
                        callback.onSuccess(tokens);
                    }

                    @Override
                    public void onError(Throwable throwable, Integer httpCode, String errorBody) {
                        callback.onError(throwable, httpCode, errorBody);
                    }
                });
            }

            @Override
            public void onFailure(Call<TokenResponseDto> call, Throwable t) {
                callback.onError(t, null, null);
            }
        });
    }

    /**
     * Internal helper to convert TokenResponseDto -> AuthTokens and route callbacks.
     */
    private void handleTokenResponse(Response<TokenResponseDto> response, InternalCallback callback) {
        if (!response.isSuccessful()) {
            String errorBody = null;
            try {
                if (response.errorBody() != null) {
                    errorBody = response.errorBody().string();
                }
            } catch (Exception ignored) {
            }
            callback.onError(null, response.code(), errorBody);
            return;
        }

        TokenResponseDto body = response.body();
        if (body == null) {
            callback.onError(
                    new IllegalStateException("Empty response body"),
                    response.code(),
                    null
            );
            return;
        }

        AuthTokens tokens = new AuthTokens(
                body.getAccessToken(),
                body.getRefreshToken(),
                body.getTokenType(),
                body.getExpiresIn()
        );
        callback.onSuccess(tokens);
    }

    private interface InternalCallback {
        void onSuccess(AuthTokens tokens);
        void onError(Throwable throwable, Integer httpCode, String errorBody);
    }
}
