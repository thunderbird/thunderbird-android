package com.fsck.k9.mailstore;

import com.fsck.k9.mail.MessagingException;

public class UnavailableStorageException extends MessagingException {

    private static final long serialVersionUID = 1348267375054620792L;

    public UnavailableStorageException(String message) {
        // consider this exception as a permanent failure by default
        this(message, true);
    }

    private UnavailableStorageException(String message, boolean perm) {
        super(message, perm);
    }

    public UnavailableStorageException(String message, Throwable throwable) {
        // consider this exception as permanent failure by default
        this(message, true, throwable);
    }

    private UnavailableStorageException(String message, boolean perm, Throwable throwable) {
        super(message, perm, throwable);
    }

}
