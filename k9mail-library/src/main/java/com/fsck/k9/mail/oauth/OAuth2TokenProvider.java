package com.fsck.k9.mail.oauth;


import com.fsck.k9.mail.AuthenticationFailedException;
import com.fsck.k9.mail.OAuth2NeedUserPromptException;


public abstract class OAuth2TokenProvider {
    /**
     * A default timeout value to use when fetching tokens.
     */
    public static int OAUTH2_TIMEOUT = 30000;

    public abstract String getToken(String email, long timeoutMillis) throws AuthenticationFailedException,
            OAuth2NeedUserPromptException;

    public abstract void invalidateToken(String email);
    public abstract void disconnectEmailWithK9(String email);
}
