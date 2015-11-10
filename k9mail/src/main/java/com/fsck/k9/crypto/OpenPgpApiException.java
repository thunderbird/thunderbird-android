package com.fsck.k9.crypto;

import org.openintents.openpgp.OpenPgpError;

/**
 * Created by alexandre on 11/9/15.
 */
public class OpenPgpApiException extends Exception {
    public OpenPgpApiException() {
    }

    public OpenPgpApiException(String detailMessage) {
        super(detailMessage);
    }

    public OpenPgpApiException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    public OpenPgpApiException(Throwable throwable) {
        super(throwable);
    }

    public OpenPgpApiException(OpenPgpError error){
        super(error.getMessage());
    }
}
