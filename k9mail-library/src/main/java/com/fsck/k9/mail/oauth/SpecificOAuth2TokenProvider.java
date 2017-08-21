package com.fsck.k9.mail.oauth;

import com.fsck.k9.mail.AuthenticationFailedException;

public abstract class SpecificOAuth2TokenProvider {
    public abstract OAuth2AuthorizationCodeFlowTokenProvider.Tokens exchangeCode(String username, String code) throws AuthenticationFailedException;

    public abstract String refreshToken(String username, String refreshToken) throws AuthenticationFailedException;

    public abstract void showAuthDialog();
}
