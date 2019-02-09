package com.fsck.k9.mail.store.pop3;


import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;

import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.ServerSettings;
import com.fsck.k9.mail.ssl.TrustedSocketFactory;
import com.fsck.k9.mail.store.RemoteStore;
import com.fsck.k9.mail.store.StoreConfig;


public class Pop3Store extends RemoteStore {
    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final String clientCertificateAlias;
    private final AuthType authType;
    private final ConnectionSecurity connectionSecurity;

    private Map<String, Pop3Folder> mFolders = new HashMap<>();

    public Pop3Store(ServerSettings serverSettings, StoreConfig storeConfig, TrustedSocketFactory socketFactory) {
        super(storeConfig, socketFactory);

        host = serverSettings.host;
        port = serverSettings.port;
        connectionSecurity = serverSettings.connectionSecurity;
        username = serverSettings.username;
        password = serverSettings.password;
        clientCertificateAlias = serverSettings.clientCertificateAlias;
        authType = serverSettings.authenticationType;
    }

    @Override
    @NonNull
    public Pop3Folder getFolder(String name) {
        Pop3Folder folder = mFolders.get(name);
        if (folder == null) {
            folder = new Pop3Folder(this, name);
            mFolders.put(folder.getServerId(), folder);
        }
        return folder;
    }

    @Override
    public List<Pop3Folder> getPersonalNamespaces() {
        List<Pop3Folder> folders = new LinkedList<>();
        folders.add(getFolder(Pop3Folder.INBOX));
        return folders;
    }

    @Override
    public void checkSettings() throws MessagingException {
        Pop3Folder folder = new Pop3Folder(this, Pop3Folder.INBOX);
        try {
            folder.open(Folder.OPEN_MODE_RW);
            folder.requestUidl();
        }
        finally {
            folder.close();
        }
    }

    @Override
    public boolean isSeenFlagSupported() {
        return false;
    }


    StoreConfig getConfig() {
        return mStoreConfig;
    }

    public Pop3Connection createConnection() throws MessagingException {
        return new Pop3Connection(new StorePop3Settings(), mTrustedSocketFactory);
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
