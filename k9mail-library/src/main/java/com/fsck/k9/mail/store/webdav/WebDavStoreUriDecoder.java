package com.fsck.k9.mail.store.webdav;


import com.fsck.k9.mail.ConnectionSecurity;

import java.net.URI;
import java.net.URISyntaxException;

import static com.fsck.k9.mail.helper.UrlEncodingHelper.decodeUtf8;

public class WebDavStoreUriDecoder {

    /**
     * Decodes a WebDavStore URI.
     * <p/>
     * <p>Possible forms:</p>
     * <pre>
     * webdav://user:password@server:port ConnectionSecurity.NONE
     * webdav+ssl+://user:password@server:port ConnectionSecurity.SSL_TLS_REQUIRED
     * </pre>
     */
    public static WebDavStoreSettings decode(String uri) {
        String host;
        int port;
        ConnectionSecurity connectionSecurity;
        String username = null;
        String password = null;
        String alias = null;
        String path = null;
        String authPath = null;
        String mailboxPath = null;


        URI webDavUri;
        try {
            webDavUri = new URI(uri);
        } catch (URISyntaxException use) {
            throw new IllegalArgumentException("Invalid WebDavStore URI", use);
        }

        String scheme = webDavUri.getScheme();
        /*
         * Currently available schemes are:
         * webdav
         * webdav+ssl+
         *
         * The following are obsolete schemes that may be found in pre-existing
         * settings from earlier versions or that may be found when imported. We
         * continue to recognize them and re-map them appropriately:
         * webdav+tls
         * webdav+tls+
         * webdav+ssl
         */
        if (scheme.equals("webdav")) {
            connectionSecurity = ConnectionSecurity.NONE;
        } else if (scheme.startsWith("webdav+")) {
            connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED;
        } else {
            throw new IllegalArgumentException("Unsupported protocol (" + scheme + ")");
        }

        host = webDavUri.getHost();
        if (host.startsWith("http")) {
            String[] hostParts = host.split("://", 2);
            if (hostParts.length > 1) {
                host = hostParts[1];
            }
        }

        port = webDavUri.getPort();

        String userInfo = webDavUri.getUserInfo();
        if (userInfo != null) {
            String[] userInfoParts = userInfo.split(":");
            username = decodeUtf8(userInfoParts[0]);
            String userParts[] = username.split("\\\\", 2);

            if (userParts.length > 1) {
                alias = userParts[1];
            } else {
                alias = username;
            }
            if (userInfoParts.length > 1) {
                password = decodeUtf8(userInfoParts[1]);
            }
        }

        String[] pathParts = webDavUri.getPath().split("\\|");
        for (int i = 0, count = pathParts.length; i < count; i++) {
            if (i == 0) {
                if (pathParts[0] != null &&
                        pathParts[0].length() > 1) {
                    path = pathParts[0];
                }
            } else if (i == 1) {
                if (pathParts[1] != null &&
                        pathParts[1].length() > 1) {
                    authPath = pathParts[1];
                }
            } else if (i == 2) {
                if (pathParts[2] != null &&
                        pathParts[2].length() > 1) {
                    mailboxPath = pathParts[2];
                }
            }
        }

        return new WebDavStoreSettings(host, port, connectionSecurity, null, username, password,
                null, alias, path, authPath, mailboxPath);
    }
}
