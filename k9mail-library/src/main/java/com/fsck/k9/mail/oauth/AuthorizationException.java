package com.fsck.k9.mail.oauth;

class AuthorizationException extends Exception {
    public AuthorizationException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public AuthorizationException(String detailMessage) {
        super(detailMessage);
    }
}
