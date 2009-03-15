package com.android.email.mail;

public class ProtocolException extends Exception {
    public static final long serialVersionUID = -1;
    
    public ProtocolException(String message) {
        super(message);
    }

    public ProtocolException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
