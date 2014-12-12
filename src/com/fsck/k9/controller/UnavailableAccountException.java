package com.fsck.k9.controller;

/**
 * An {@link com.fsck.k9.Account} is not
 * {@link com.fsck.k9.Account#isAvailable(android.content.Context)}.<br/>
 * The operation may be retried later.
 */
public class UnavailableAccountException extends RuntimeException {

    /**
     *
     */
    private static final long serialVersionUID = -1827283277120501465L;

    public UnavailableAccountException() {
        super("please try again later");
    }

    /**
     * @param detailMessage
     * @param throwable
     */
    public UnavailableAccountException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    /**
     * @param detailMessage
     */
    public UnavailableAccountException(String detailMessage) {
        super(detailMessage);
    }

    /**
     * @param throwable
     */
    public UnavailableAccountException(Throwable throwable) {
        super(throwable);
    }
}
