package com.fsck.k9.mail.helpers;

import android.annotation.SuppressLint;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;


@SuppressLint("TrustAllX509TrustManager")
class VeryTrustingTrustManager implements X509TrustManager {
    private final X509Certificate serverCertificate;


    public VeryTrustingTrustManager(X509Certificate serverCertificate) {
        this.serverCertificate = serverCertificate;
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        // Accept all certificates
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        // Accept all certificates
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[] { serverCertificate };
    }
}

