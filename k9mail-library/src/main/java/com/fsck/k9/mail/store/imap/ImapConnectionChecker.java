package com.fsck.k9.mail.store.imap;


import java.io.IOException;

import android.net.ConnectivityManager;
import android.support.annotation.NonNull;

import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.CertificateValidationException;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.NetworkType;
import com.fsck.k9.mail.ssl.TrustedSocketFactory;
import timber.log.Timber;


public class ImapConnectionChecker {
    private final TrustedSocketFactory socketFactory;
    private final ConnectivityManager connectivityManager;

    public ImapConnectionChecker(TrustedSocketFactory socketFactory, ConnectivityManager connectivityManager) {
        this.socketFactory = socketFactory;
        this.connectivityManager = connectivityManager;
    }

    public boolean attemptConnect(String host, int port, ConnectionSecurity connectionSecurity,
            String username, String password, AuthType authType) throws IOException, CertificateValidationException {
        Timber.d("Attempting IMAP auth as %s (%s)", username, authType.toString());
        ImapSettings settings = getImapSettings(host, port, connectionSecurity, username, password, authType);

        ImapConnection imapConnection =
                new ImapConnection(settings, socketFactory, connectivityManager, null);

        try {
            imapConnection.open();
            return imapConnection.isConnected();
        } catch (CertificateValidationException e) {
            throw e;
        } catch (MessagingException e) {
            Timber.d(e, "IMAP auth failed");
            return false;
        }
    }

    @NonNull
    private ImapSettings getImapSettings(final String host, final int port, final ConnectionSecurity connectionSecurity,
            final String username, final String password, final AuthType authType) {
        return new ImapSettings() {
            @Override
            public String getHost() {
                return host;
            }

            @Override
            public int getPort() {
                return port;
            }

            @Override
            public ConnectionSecurity getConnectionSecurity() {
                return connectionSecurity;
            }

            @Override
            public AuthType getAuthType() {
                return authType;
            }

            @Override
            public String getUsername() {
                return username;
            }

            @Override
            public String getPassword() {
                return password;
            }

            @Override
            public String getClientCertificateAlias() {
                return null;
            }

            @Override
            public boolean useCompression(NetworkType type) {
                return false;
            }

            @Override
            public String getPathPrefix() {
                return "/";
            }

            @Override
            public void setPathPrefix(String prefix) {

            }

            @Override
            public String getPathDelimiter() {
                return null;
            }

            @Override
            public void setPathDelimiter(String delimiter) {

            }

            @Override
            public String getCombinedPrefix() {
                return null;
            }

            @Override
            public void setCombinedPrefix(String prefix) {

            }
        };
    }
}
