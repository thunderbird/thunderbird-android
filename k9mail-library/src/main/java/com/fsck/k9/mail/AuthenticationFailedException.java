
package com.fsck.k9.mail;

public class AuthenticationFailedException extends MessagingException {
    public static final long serialVersionUID = -1;
    public static final String OAUTH2_ERROR_INVALID_REFRESH_TOKEN = "oauth2-invalid refresh token";
    public static final String OAUTH2_ERROR_UNKNOWN = "oauth2-unknown";

    public AuthenticationFailedException(String message) {
        super(message);
    }

    public AuthenticationFailedException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
