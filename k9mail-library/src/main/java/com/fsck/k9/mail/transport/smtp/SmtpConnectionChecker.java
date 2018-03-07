package com.fsck.k9.mail.transport.smtp;


import java.io.IOException;

import android.net.ConnectivityManager;

import com.fsck.k9.mail.CertificateValidationException;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.ssl.TrustedSocketFactory;
import timber.log.Timber;


public class SmtpConnectionChecker {
    private final TrustedSocketFactory socketFactory;
    private final ConnectivityManager connectivityManager;

    public SmtpConnectionChecker(TrustedSocketFactory socketFactory, ConnectivityManager connectivityManager) {
        this.socketFactory = socketFactory;
        this.connectivityManager = connectivityManager;
    }

    public boolean attemptConnect(String transportUri) throws IOException, CertificateValidationException {
        try {
            SmtpTransport transport = new SmtpTransport(transportUri, socketFactory, null, true);
            transport.open();
            transport.close();
            return true;
        } catch (CertificateValidationException e) {
            throw e;
        } catch (MessagingException e) {
            Timber.d(e, "SMTP auth failed");
            return false;
        }
    }
}
