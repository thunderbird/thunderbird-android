
package com.fsck.k9.mail.store.pop3;

import android.support.annotation.NonNull;

import com.fsck.k9.mail.*;
import com.fsck.k9.mail.ServerSettings.Type;
import com.fsck.k9.mail.ssl.TrustedSocketFactory;
import com.fsck.k9.mail.store.RemoteStore;
import com.fsck.k9.mail.store.StoreConfig;

import java.net.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.fsck.k9.mail.helper.UrlEncodingHelper.decodeUtf8;
import static com.fsck.k9.mail.helper.UrlEncodingHelper.encodeUtf8;


public class Pop3Store extends RemoteStore {

    /**
     * Decodes a Pop3Store URI.
     *
     * <p>Possible forms:</p>
     * <pre>
     * pop3://authType:user:password@server:port
     *      ConnectionSecurity.NONE
     * pop3+tls+://authType:user:password@server:port
     *      ConnectionSecurity.STARTTLS_REQUIRED
     * pop3+ssl+://authType:user:password@server:port
     *      ConnectionSecurity.SSL_TLS_REQUIRED
     * </pre>
     * 
     * e.g.
     * <pre>pop3://PLAIN:admin:pass123@example.org:12345</pre>
     */
    public static ServerSettings decodeUri(String uri) {
        String host;
        int port;
        ConnectionSecurity connectionSecurity;
        String username = null;
        String password = null;
        String clientCertificateAlias = null;

        URI pop3Uri;
        try {
            pop3Uri = new URI(uri);
        } catch (URISyntaxException use) {
            throw new IllegalArgumentException("Invalid Pop3Store URI", use);
        }

        String scheme = pop3Uri.getScheme();
        /*
         * Currently available schemes are:
         * pop3
         * pop3+tls+
         * pop3+ssl+
         *
         * The following are obsolete schemes that may be found in pre-existing
         * settings from earlier versions or that may be found when imported. We
         * continue to recognize them and re-map them appropriately:
         * pop3+tls
         * pop3+ssl
         */
        if (scheme.equals("pop3")) {
            connectionSecurity = ConnectionSecurity.NONE;
            port = Type.POP3.defaultPort;
        } else if (scheme.startsWith("pop3+tls")) {
            connectionSecurity = ConnectionSecurity.STARTTLS_REQUIRED;
            port = Type.POP3.defaultPort;
        } else if (scheme.startsWith("pop3+ssl")) {
            connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED;
            port = Type.POP3.defaultTlsPort;
        } else {
            throw new IllegalArgumentException("Unsupported protocol (" + scheme + ")");
        }

        host = pop3Uri.getHost();

        if (pop3Uri.getPort() != -1) {
            port = pop3Uri.getPort();
        }

        AuthType authType = AuthType.PLAIN;
        if (pop3Uri.getUserInfo() != null) {
            int userIndex = 0, passwordIndex = 1;
            String userinfo = pop3Uri.getUserInfo();
            String[] userInfoParts = userinfo.split(":");
            if (userInfoParts.length > 2 || userinfo.endsWith(":") ) {
                // If 'userinfo' ends with ":" the password is empty. This can only happen
                // after an account was imported (so authType and username are present).
                userIndex++;
                passwordIndex++;
                authType = AuthType.valueOf(userInfoParts[0]);
            }
            username = decodeUtf8(userInfoParts[userIndex]);
            if (userInfoParts.length > passwordIndex) {
                if (authType == AuthType.EXTERNAL) {
                    clientCertificateAlias = decodeUtf8(userInfoParts[passwordIndex]);
                } else {
                    password = decodeUtf8(userInfoParts[passwordIndex]);
                }
            }
        }

        return new ServerSettings(ServerSettings.Type.POP3, host, port, connectionSecurity, authType, username,
                password, clientCertificateAlias);
    }

    /**
     * Creates a Pop3Store URI with the supplied settings.
     *
     * @param server
     *         The {@link ServerSettings} object that holds the server settings.
     *
     * @return A Pop3Store URI that holds the same information as the {@code server} parameter.
     *
     * @see StoreConfig#getStoreUri()
     * @see Pop3Store#decodeUri(String)
     */
    public static String createUri(ServerSettings server) {
        String userEnc = encodeUtf8(server.username);
        String passwordEnc = (server.password != null) ?
                    encodeUtf8(server.password) : "";
        String clientCertificateAliasEnc = (server.clientCertificateAlias != null) ?
                    encodeUtf8(server.clientCertificateAlias) : "";

        String scheme;
        switch (server.connectionSecurity) {
            case SSL_TLS_REQUIRED:
                scheme = "pop3+ssl+";
                break;
            case STARTTLS_REQUIRED:
                scheme = "pop3+tls+";
                break;
            default:
            case NONE:
                scheme = "pop3";
                break;
        }

        AuthType authType = server.authenticationType;
        String userInfo;
        if (AuthType.EXTERNAL == authType) {
            userInfo = authType.name() + ":" + userEnc + ":" + clientCertificateAliasEnc;
        } else {
            userInfo = authType.name() + ":" + userEnc + ":" + passwordEnc;
        }

        try {
            return new URI(scheme, userInfo, server.host, server.port, null, null,
                    null).toString();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Can't create Pop3Store URI", e);
        }
    }


    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final String clientCertificateAlias;
    private final AuthType authType;
    private final ConnectionSecurity connectionSecurity;

    private Map<String, Pop3Folder> mFolders = new HashMap<String, Pop3Folder>();

    public Pop3Store(StoreConfig storeConfig, TrustedSocketFactory socketFactory) throws MessagingException {
        super(storeConfig, socketFactory);

        ServerSettings settings;
        try {
            settings = decodeUri(storeConfig.getStoreUri());
        } catch (IllegalArgumentException e) {
            throw new MessagingException("Error while decoding store URI", e);
        }

        host = settings.host;
        port = settings.port;
        connectionSecurity = settings.connectionSecurity;
        username = settings.username;
        password = settings.password;
        clientCertificateAlias = settings.clientCertificateAlias;
        authType = settings.authenticationType;
    }

    @Override
    @NonNull
    public Pop3Folder getFolder(String name) {
        Pop3Folder folder = mFolders.get(name);
        if (folder == null) {
            folder = new Pop3Folder(this, name);
            mFolders.put(folder.getId(), folder);
        }
        return folder;
    }

    @Override
    @NonNull public List <Pop3Folder> getFolders(boolean forceListAll) throws MessagingException {
        List<Pop3Folder> folders = new LinkedList<>();
        folders.add(getFolder(mStoreConfig.getInboxFolderId()));
        return folders;
    }

    @NonNull
    @Override
    public List<? extends Folder> getSubFolders(String parentFolderId, boolean forceListAll) throws MessagingException {
        return new ArrayList<>();
    }

    @Override
    public void checkSettings() throws MessagingException {
        Pop3Folder folder = new Pop3Folder(this, mStoreConfig.getInboxFolderId());
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
