package com.fsck.k9.mail;

import java.security.cert.X509Certificate;
import java.util.List;

import org.jetbrains.annotations.NotNull;


public class CertificateValidationException extends MessagingException {
    private final List<X509Certificate> certificateChain;

    public CertificateValidationException(@NotNull List<X509Certificate> certificateChain, Throwable cause) {
        super(cause);
        this.certificateChain = certificateChain;
    }

    public List<X509Certificate> getCertificateChain() {
        return certificateChain;
    }
}
