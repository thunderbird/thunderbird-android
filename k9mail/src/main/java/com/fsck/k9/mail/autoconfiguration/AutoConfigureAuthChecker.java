package com.fsck.k9.mail.autoconfiguration;


import java.io.IOException;
import java.security.cert.CertificateException;

import android.net.ConnectivityManager;

import com.fsck.k9.activity.setup.AccountSetupUris;
import com.fsck.k9.helper.EmailHelper;
import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.CertificateValidationException;
import com.fsck.k9.mail.autoconfiguration.AutoConfigure.AuthInfo;
import com.fsck.k9.mail.autoconfiguration.AutoConfigure.ProviderInfo;
import com.fsck.k9.mail.ssl.DefaultTrustedSocketFactory;
import com.fsck.k9.mail.ssl.LocalKeyStore;
import com.fsck.k9.mail.store.imap.ImapConnectionChecker;
import com.fsck.k9.mail.transport.smtp.SmtpConnectionChecker;
import timber.log.Timber;


public class AutoConfigureAuthChecker {
    private final ImapConnectionChecker imapConnectionChecker;
    private final SmtpConnectionChecker smtpConnectionChecker;

    public AutoConfigureAuthChecker(
            ConnectivityManager connectivityManager, DefaultTrustedSocketFactory socketFactory) {
        imapConnectionChecker = new ImapConnectionChecker(socketFactory, connectivityManager);
        smtpConnectionChecker = new SmtpConnectionChecker(socketFactory, connectivityManager);
    }

    public AuthInfo checkAuthInfo(ProviderInfo providerInfo, String email, String password) {
        AuthInfo authInfo = AuthInfo.createEmpty();

        try {
            try {
                authInfo = checkStoreAuthInfo(providerInfo, authInfo, password, email);
            } catch (CertificateValidationException ce) {
                // TODO ask user

                Timber.d("Adding cert to local storage for %s", providerInfo.incomingHost);
                LocalKeyStore localKeyStore = LocalKeyStore.getInstance();
                localKeyStore.addCertificate(providerInfo.incomingHost, providerInfo.incomingPort, ce.getCertChain()[0]);

                authInfo = checkStoreAuthInfo(providerInfo, authInfo, password, email);
            }
        } catch (CertificateException | CertificateValidationException | IOException e) {
            return authInfo.withIncomingError();
        }

        try {
            try {
                authInfo = checkTransportAuthInfo(providerInfo, authInfo, password, email);
            } catch (CertificateValidationException ce) {
                // TODO ask user

                Timber.d("Adding cert to local storage for %s", providerInfo.outgoingHost);
                LocalKeyStore localKeyStore = LocalKeyStore.getInstance();
                localKeyStore.addCertificate(providerInfo.outgoingHost, providerInfo.outgoingPort, ce.getCertChain()[0]);

                authInfo = checkTransportAuthInfo(providerInfo, authInfo, password, email);
            }
        } catch (CertificateException | CertificateValidationException | IOException e) {
            return authInfo.withIncomingError();
        }

        return authInfo;
    }

    private AuthInfo checkTransportAuthInfo(
            ProviderInfo providerInfo, AuthInfo authInfo, String password, String email)
            throws IOException, CertificateValidationException {
        Timber.d("Attempting store auth for %s", providerInfo.incomingHost);

        String localPart = EmailHelper.getLocalPartFromEmailAddress(email);

        AuthInfo candidateAuthInfo;

        candidateAuthInfo = authInfo.withOutgoingAuth(AuthType.CRAM_MD5, email, password);
        if (attemptTransportConnectWithAuthType(providerInfo, candidateAuthInfo)) {
            return candidateAuthInfo;
        }

        candidateAuthInfo = authInfo.withOutgoingAuth(AuthType.CRAM_MD5, localPart, password);
        if (attemptTransportConnectWithAuthType(providerInfo, candidateAuthInfo)) {
            return candidateAuthInfo;
        }

        candidateAuthInfo = authInfo.withOutgoingAuth(AuthType.PLAIN, email, password);
        if (attemptTransportConnectWithAuthType(providerInfo, candidateAuthInfo)) {
            return candidateAuthInfo;
        }

        candidateAuthInfo = authInfo.withOutgoingAuth(AuthType.PLAIN, localPart, password);
        if (attemptTransportConnectWithAuthType(providerInfo, candidateAuthInfo)) {
            return candidateAuthInfo;
        }

        return authInfo;
    }

    private AuthInfo checkStoreAuthInfo(ProviderInfo providerInfo, AuthInfo authInfo, String password, String email)
            throws IOException, CertificateValidationException {
        Timber.d("Attempting store auth for %s", providerInfo.incomingHost);

        String localPart = EmailHelper.getLocalPartFromEmailAddress(email);

        AuthInfo candidateAuthInfo;

        candidateAuthInfo = authInfo.withIncomingAuth(AuthType.CRAM_MD5, email, password);
        if (attemptStoreConnectWithAuthType(providerInfo, candidateAuthInfo)) {
            return candidateAuthInfo;
        }

        candidateAuthInfo = authInfo.withIncomingAuth(AuthType.CRAM_MD5, localPart, password);
        if (attemptStoreConnectWithAuthType(providerInfo, candidateAuthInfo)) {
            return candidateAuthInfo;
        }

        candidateAuthInfo = authInfo.withIncomingAuth(AuthType.PLAIN, email, password);
        if (attemptStoreConnectWithAuthType(providerInfo, candidateAuthInfo)) {
            return candidateAuthInfo;
        }

        candidateAuthInfo = authInfo.withIncomingAuth(AuthType.PLAIN, localPart, password);
        if (attemptStoreConnectWithAuthType(providerInfo, candidateAuthInfo)) {
            return candidateAuthInfo;
        }

        return authInfo;
    }

    private boolean attemptStoreConnectWithAuthType(ProviderInfo providerInfo, AuthInfo authInfo)
            throws IOException, CertificateValidationException {
        switch (providerInfo.incomingType) {
            case ProviderInfo.INCOMING_TYPE_IMAP:
                return imapConnectionChecker.attemptConnect(providerInfo.incomingHost, providerInfo.incomingPort,
                        providerInfo.incomingSecurity, authInfo.incomingUsername, authInfo.incomingPassword,
                        authInfo.incomingAuthType);
            default:
                throw new UnsupportedOperationException();
        }
    }

    private boolean attemptTransportConnectWithAuthType(ProviderInfo providerInfo, AuthInfo authInfo)
            throws IOException, CertificateValidationException {
        Timber.d("Attempting %s auth %s (%s)", providerInfo.outgoingType, authInfo.outgoingUsername, authInfo.outgoingAuthType.toString());
        switch (providerInfo.outgoingType) {
            case ProviderInfo.OUTGOING_TYPE_SMTP:
                return smtpConnectionChecker.attemptConnect(AccountSetupUris.getTransportUri(providerInfo, authInfo));
            default:
                throw new UnsupportedOperationException();
        }
    }
}
