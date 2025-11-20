package tn.esprit.domain.auth;

/**
 * Domain model for the auth token bundle returned by the backend.
 * Presentation + data layers should talk in terms of this model.
 */
public class AuthTokens {

    private final String accessToken;
    private final String refreshToken;
    private final String tokenType;
    private final Long expiresIn;

    public AuthTokens(String accessToken, String refreshToken, String tokenType, Long expiresIn) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }
}
