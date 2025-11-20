package tn.esprit.data.auth;

import android.content.Context;
import android.content.SharedPreferences;

import tn.esprit.domain.auth.AuthTokens;

/**
 * Local persistence for auth tokens using SharedPreferences.
 */
public class AuthLocalDataSource {

    private static final String PREFS_NAME = "auth_prefs";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_TOKEN_TYPE = "token_type";
    private static final String KEY_EXPIRES_IN = "expires_in";

    private final SharedPreferences preferences;

    public AuthLocalDataSource(Context context) {
        this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveTokens(AuthTokens tokens) {
        if (tokens == null) return;
        preferences.edit()
                .putString(KEY_ACCESS_TOKEN, tokens.getAccessToken())
                .putString(KEY_REFRESH_TOKEN, tokens.getRefreshToken())
                .putString(KEY_TOKEN_TYPE, tokens.getTokenType())
                .putLong(KEY_EXPIRES_IN, tokens.getExpiresIn() != null ? tokens.getExpiresIn() : -1L)
                .apply();
    }

    public AuthTokens getTokens() {
        String accessToken = preferences.getString(KEY_ACCESS_TOKEN, null);
        String refreshToken = preferences.getString(KEY_REFRESH_TOKEN, null);
        String tokenType = preferences.getString(KEY_TOKEN_TYPE, null);
        long expiresIn = preferences.getLong(KEY_EXPIRES_IN, -1L);

        if (accessToken == null || refreshToken == null || tokenType == null) {
            return null;
        }

        Long expiresInValue = expiresIn >= 0 ? expiresIn : null;
        return new AuthTokens(accessToken, refreshToken, tokenType, expiresInValue);
    }

    public void clearTokens() {
        preferences.edit().clear().apply();
    }
}
