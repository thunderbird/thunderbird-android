package com.fsck.k9.mail;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * A {@link CertificateException} extension that provides access to
 * the pertinent certificate chain.
 *
 */
public class CertificateChainException extends CertificateException {

    private static final long serialVersionUID = 1103894512106650107L;
    private X509Certificate[] mCertChain;

    public CertificateChainException(String msg, X509Certificate[] chain, Throwable cause) {
        super(msg, cause);
        setCertChain(chain);
    }

    public void setCertChain(X509Certificate[] chain) {
        mCertChain = chain;
    }
    public X509Certificate[] getCertChain() {
        return mCertChain;
    }
}
