
package com.android.email.mail;

public class MessagingException extends Exception {
    public static final long serialVersionUID = -1;
    
    public MessagingException(String message) {
        super(message);
    }

    public MessagingException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
