package com.fsck.k9.preferences.migration.migration12;


import java.net.URI;
import java.net.URISyntaxException;

import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.ServerSettings;

import static com.fsck.k9.mail.helper.UrlEncodingHelper.decodeUtf8;


public class Pop3StoreUriDecoder {
    private static final int DEFAULT_PORT = 110;
    private static final int DEFAULT_TLS_PORT = 995;

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
    public static ServerSettings decode(String uri) {
        String host;
        int port;
        ConnectionSecurity connectionSecurity;
        String username = "";
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
            port = DEFAULT_PORT;
        } else if (scheme.startsWith("pop3+tls")) {
            connectionSecurity = ConnectionSecurity.STARTTLS_REQUIRED;
            port = DEFAULT_PORT;
        } else if (scheme.startsWith("pop3+ssl")) {
            connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED;
            port = DEFAULT_TLS_PORT;
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

        return new ServerSettings("pop3", host, port, connectionSecurity, authType, username,
                password, clientCertificateAlias);
    }
}
