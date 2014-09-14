
package com.fsck.k9.mail;

public class AuthenticationFailedException extends MessagingException {
    public static final long serialVersionUID = -1;

    public AuthenticationFailedException(String message) {
        super(message);
    }

    public AuthenticationFailedException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
