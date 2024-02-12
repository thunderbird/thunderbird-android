package com.fsck.k9.mail.store.pop3;


import java.util.HashMap;
import java.util.Map;

import com.fsck.k9.logging.Timber;
import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.ServerSettings;
import com.fsck.k9.mail.ssl.TrustedSocketFactory;
import org.jetbrains.annotations.NotNull;


public class Pop3Store {
    private final TrustedSocketFactory trustedSocketFactory;
    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final String clientCertificateAlias;
    private final AuthType authType;
    private final ConnectionSecurity connectionSecurity;

    private Map<String, Pop3Folder> mFolders = new HashMap<>();

    public Pop3Store(ServerSettings serverSettings, TrustedSocketFactory socketFactory) {
        if (!serverSettings.type.equals("pop3")) {
            throw new IllegalArgumentException("Expected POP3 ServerSettings");
        }

        trustedSocketFactory = socketFactory;
        host = serverSettings.host;
        port = serverSettings.port;
        connectionSecurity = serverSettings.connectionSecurity;
        username = serverSettings.username;
        password = serverSettings.password;
        clientCertificateAlias = serverSettings.clientCertificateAlias;
        authType = serverSettings.authenticationType;
    }

    @NotNull
    public Pop3Folder getFolder(String name) {
        Pop3Folder folder = mFolders.get(name);
        if (folder == null) {
            folder = new Pop3Folder(this, name);
            mFolders.put(folder.getServerId(), folder);
        }
        return folder;
    }

    public void checkSettings() throws MessagingException {
        Pop3Folder folder = new Pop3Folder(this, Pop3Folder.INBOX);
        try {
            folder.open();
            folder.requestUidl();
        } catch (Exception e) {
            Timber.e(e, "Error while checking server settings");
            throw e;
        } finally {
            folder.close();
        }
    }

    public Pop3Connection createConnection() throws MessagingException {
        return new Pop3Connection(new StorePop3Settings(), trustedSocketFactory);
    }

    private class StorePop3Settings implements Pop3Settings {
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
            return clientCertificateAlias;
        }
    }

}
