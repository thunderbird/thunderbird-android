
package com.android.email.mail;

public class NoSuchProviderException extends MessagingException {
    public static final long serialVersionUID = -1;

    public NoSuchProviderException(String message) {
        super(message);
    }

    public NoSuchProviderException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
