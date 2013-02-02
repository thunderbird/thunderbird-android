
package com.fsck.k9.mail;

import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateException;

public class CertificateValidationException extends MessagingException {
    public static final long serialVersionUID = -1;

    public CertificateValidationException(String message) {
        super(message);
    }

    public CertificateValidationException(final String message, Throwable throwable) {
        super(message, throwable);
    }

    public boolean needsUserAttention() {
        Throwable throwable = getCause();

        /* user attention is required if the certificate was deemed invalid */
        while (throwable != null
                && !(throwable instanceof CertPathValidatorException)
                && !(throwable instanceof CertificateException)) {
            throwable = throwable.getCause();
        }

        return throwable != null;
    }
}