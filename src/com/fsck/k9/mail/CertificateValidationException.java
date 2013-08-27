
package com.fsck.k9.mail;

import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class CertificateValidationException extends MessagingException {
    public static final long serialVersionUID = -1;
    private X509Certificate[] mCertChain;

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

        if (throwable instanceof CertificateChainException) {
            mCertChain = ((CertificateChainException) throwable).getCertChain();
        }
        return throwable != null;
    }

    /**
     * If the cause of this {@link CertificateValidationException} was a
     * {@link CertificateChainException}, then the offending chain is available
     * for return.
     * 
     * @return An {@link X509Certificate X509Certificate[]} containing the Cert.
     *         chain, or else null.
     */
    public X509Certificate[] getCertChain() {
        needsUserAttention();
        return mCertChain;
    }
}