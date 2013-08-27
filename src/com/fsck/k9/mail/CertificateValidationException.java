
package com.fsck.k9.mail;

import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class CertificateValidationException extends MessagingException {
    public static final long serialVersionUID = -1;
    private X509Certificate[] mCertChain;
    private boolean mNeedsUserAttention = false;

    public CertificateValidationException(String message) {
        super(message);
        scanForCause();
    }

    public CertificateValidationException(final String message, Throwable throwable) {
        super(message, throwable);
        scanForCause();
    }

    private void scanForCause() {
        Throwable throwable = getCause();

        /* user attention is required if the certificate was deemed invalid */
        while (throwable != null
                && !(throwable instanceof CertPathValidatorException)
                && !(throwable instanceof CertificateException)) {
            throwable = throwable.getCause();
        }

        if (throwable != null) {
            mNeedsUserAttention = true;
            if (throwable instanceof CertificateChainException) {
                mCertChain = ((CertificateChainException) throwable).getCertChain();
            }
        }
    }

    public boolean needsUserAttention() {
        return mNeedsUserAttention;
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
        return mCertChain;
    }
}