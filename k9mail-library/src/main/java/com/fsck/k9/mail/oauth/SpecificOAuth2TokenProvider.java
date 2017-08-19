package com.fsck.k9.mail.oauth;

import com.fsck.k9.mail.AuthenticationFailedException;

public interface SpecificOAuth2TokenProvider {
    OAuth2TokenProvider.Tokens exchangeCode(String username, String code) throws AuthenticationFailedException;

    /**
     * refresh access token with refresh token
     * @param username username (usually email address)
     * @param refreshToken refresh token got before
     * @return new access token
     * @throws AuthenticationFailedException throws it when error occurs
     */
    String refreshToken(String username, String refreshToken) throws AuthenticationFailedException;

    void showAuthDialog();
}
