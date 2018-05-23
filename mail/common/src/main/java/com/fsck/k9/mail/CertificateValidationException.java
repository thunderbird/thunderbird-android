
package com.fsck.k9.mail;

import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLHandshakeException;

import android.security.KeyChainException;

public class CertificateValidationException extends MessagingException {
    public static final long serialVersionUID = -1;
    private final Reason mReason;
    private X509Certificate[] mCertChain;
    private boolean mNeedsUserAttention = false;
    private String mAlias;

    public enum Reason {
        Unknown, UseMessage, Expired, MissingCapability, RetrievalFailure
    }

    public CertificateValidationException(String message) {
        this(message, Reason.UseMessage, null);
    }

    public CertificateValidationException(Reason reason) {
        this(null, reason, null);
    }

    public CertificateValidationException(String message, Reason reason, String alias) {
        super(message);
        /*
         * Instances created without a Throwable parameter as a cause are
         * presumed to need user attention.
         */
        mNeedsUserAttention = true;
        mReason = reason;
        mAlias = alias;
    }

    public CertificateValidationException(final String message, Throwable throwable) {
        super(message, throwable);
        mReason = Reason.Unknown;
        scanForCause();
    }

    public String getAlias() {
        return mAlias;
    }

    public Reason getReason() {
        return mReason;
    }

    private void scanForCause() {
        Throwable throwable = getCause();

        /*
         * User attention is required if the server certificate was deemed
         * invalid or if there was a problem with a client certificate.
         *
         * A CertificateException is known to be thrown by the default
         * X509TrustManager.checkServerTrusted() if the server certificate
         * doesn't validate. The cause of the CertificateException will be a
         * CertPathValidatorException. However, it's unlikely those exceptions
         * will be encountered here, because they are caught in
         * SecureX509TrustManager.checkServerTrusted(), which throws a
         * CertificateChainException instead (an extension of
         * CertificateException).
         *
         * A CertificateChainException will likely result in (or, be the cause
         * of) an SSLHandshakeException (an extension of SSLException).
         *
         * The various mail protocol handlers (IMAP, POP3, ...) will catch an
         * SSLException and throw a CertificateValidationException (this class)
         * with the SSLException as the cause. (They may also throw a
         * CertificateValidationException when STARTTLS is not available, just
         * for the purpose of triggering a user notification.)
         *
         * SSLHandshakeException is also known to occur if the *client*
         * certificate was not accepted by the server (unknown CA, certificate
         * expired, etc.). In this case, the SSLHandshakeException will not have
         * a CertificateChainException as a cause.
         *
         * KeyChainException is known to occur if the device has no client
         * certificate that's associated with the alias stored in the server
         * settings.
         */
        while (throwable != null
                && !(throwable instanceof CertPathValidatorException)
                && !(throwable instanceof CertificateException)
                && !(throwable instanceof KeyChainException)
                && !(throwable instanceof SSLHandshakeException)) {
            throwable = throwable.getCause();
        }

        if (throwable != null) {
            mNeedsUserAttention = true;

            // See if there is a server certificate chain attached to the SSLHandshakeException
            if (throwable instanceof SSLHandshakeException) {
                while (throwable != null && !(throwable instanceof CertificateChainException)) {
                  throwable = throwable.getCause();
                }
            }

            if (throwable != null && throwable instanceof CertificateChainException) {
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
