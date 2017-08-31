package com.fsck.k9.mail.oauth;


import com.fsck.k9.mail.AuthenticationFailedException;
import com.fsck.k9.mail.OAuth2NeedUserPromptException;

import java.util.HashMap;
import java.util.Map;


public abstract class OAuth2AuthorizationCodeFlowTokenProvider {
    /**
     * A default timeout value to use when fetching tokens.
     */
    public static int OAUTH2_TIMEOUT = 30000;

    private Map<String,String> authTokens = new HashMap<>();

    public void exchangeCode(String email, String code) throws AuthenticationFailedException {
        SpecificOAuth2TokenProvider specificProvider = getSpecificProviderFromEmail(email);
        Tokens tokens = specificProvider.exchangeCode(email, code);

        authTokens.put(email, tokens.accessToken);
        saveRefreshToken(email, tokens.refreshToken);
    }

    protected abstract void saveRefreshToken(String email, String refreshToken);

    /**
     * Fetch a token. No guarantees are provided for validity.
     * @param email Username
     * @return Token string
     * @throws AuthenticationFailedException throw when error occurs
     * @throws OAuth2NeedUserPromptException throw it when user haven't allow us to login
     */
    public String getToken(String email, long timeoutMillis)
            throws AuthenticationFailedException, OAuth2NeedUserPromptException {
        if (!authTokens.containsKey(email)) {
            String refreshToken = getRefreshToken(email);
            if (refreshToken != null) {
                try {
                    refreshToken(email, refreshToken);
                } catch (Exception e) {
                    throw new AuthenticationFailedException(e.getMessage());
                }
            } else {
                showAuthDialog(email);
                throw new OAuth2NeedUserPromptException();
            }
        }
        return authTokens.get(email);
    }

    protected abstract void showAuthDialog(String email);

    /**
     * get refresh token got before
     * @param username username (usually email address)
     * @return refresh token
     */
    protected abstract String getRefreshToken(String username);

    /**
     * refresh access token with refresh token
     * @param email email address
     * @param refreshToken refresh token got before
     * @throws AuthenticationFailedException throws it when error occurs
     */
    private void refreshToken(String email, String refreshToken) throws AuthenticationFailedException {
        SpecificOAuth2TokenProvider provider = getSpecificProviderFromEmail(email);
        String newToken = provider.refreshToken(email, refreshToken);
        authTokens.put(email, newToken);
    }

    /**
     * Invalidate the token for this email.
     *
     * <p>
     * Note that the token should always be invalidated on credential failure. However invalidating a token every
     * single time is not recommended.
     * <p>
     * Invalidating a token and then failure with a new token should be treated as a permanent failure.
     */
    public void invalidateAccessToken(String email) {
        authTokens.remove(email);
    }

    public abstract void invalidateRefreshToken(String username);

    protected abstract SpecificOAuth2TokenProvider getSpecificProviderFromEmail(String email);

    public static class Tokens {
        String accessToken;
        String refreshToken;

        public Tokens(String accessToken, String refreshToken) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }
    }
}
