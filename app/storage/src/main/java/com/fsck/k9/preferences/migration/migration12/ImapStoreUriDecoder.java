package com.fsck.k9.preferences.migration.migration12;


import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.ServerSettings;

import static com.fsck.k9.mail.helper.UrlEncodingHelper.decodeUtf8;


public class ImapStoreUriDecoder {
    private static final int DEFAULT_PORT = 143;
    private static final int DEFAULT_TLS_PORT = 993;

    /**
     * Decodes an ImapStore URI.
     *
     * <p>Possible forms:</p>
     * <pre>
     * imap://auth:user:password@server:port ConnectionSecurity.NONE
     * imap+tls+://auth:user:password@server:port ConnectionSecurity.STARTTLS_REQUIRED
     * imap+ssl+://auth:user:password@server:port ConnectionSecurity.SSL_TLS_REQUIRED
     * </pre>
     *
     * NOTE: this method expects the userinfo part of the URI to be encoded twice, due to a bug in the URI creation
     * code.
     *
     * @param uri the store uri.
     */
    public static ServerSettings decode(String uri) {
        String host;
        int port;
        ConnectionSecurity connectionSecurity;
        AuthType authenticationType = AuthType.PLAIN;
        String username = "";
        String password = null;
        String clientCertificateAlias = null;
        String pathPrefix = null;
        boolean autoDetectNamespace = true;

        URI imapUri;
        try {
            imapUri = new URI(uri);
        } catch (URISyntaxException use) {
            throw new IllegalArgumentException("Invalid ImapStore URI", use);
        }

        String scheme = imapUri.getScheme();
        /*
         * Currently available schemes are:
         * imap
         * imap+tls+
         * imap+ssl+
         *
         * The following are obsolete schemes that may be found in pre-existing
         * settings from earlier versions or that may be found when imported. We
         * continue to recognize them and re-map them appropriately:
         * imap+tls
         * imap+ssl
         */
        if (scheme.equals("imap")) {
            connectionSecurity = ConnectionSecurity.NONE;
            port = DEFAULT_PORT;
        } else if (scheme.startsWith("imap+tls")) {
            connectionSecurity = ConnectionSecurity.STARTTLS_REQUIRED;
            port = DEFAULT_PORT;
        } else if (scheme.startsWith("imap+ssl")) {
            connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED;
            port = DEFAULT_TLS_PORT;
        } else {
            throw new IllegalArgumentException("Unsupported protocol (" + scheme + ")");
        }

        host = imapUri.getHost();

        if (imapUri.getPort() != -1) {
            port = imapUri.getPort();
        }

        if (imapUri.getUserInfo() != null) {
            String userinfo = imapUri.getUserInfo();
            String[] userInfoParts = userinfo.split(":");

            if (userinfo.endsWith(":")) {
                // Last field (password/certAlias) is empty.
                // For imports e.g.: PLAIN:username: or username:
                // Or XOAUTH2 where it's a valid config - XOAUTH:username:
                if (userInfoParts.length > 1) {
                    authenticationType = AuthType.valueOf(userInfoParts[0]);
                    username = decodeUtf8(userInfoParts[1]);
                } else {
                    username = decodeUtf8(userInfoParts[0]);
                }
            } else if (userInfoParts.length == 2) {
                // Old/standard style of encoding - PLAIN auth only:
                // username:password
                username = decodeUtf8(userInfoParts[0]);
                password = decodeUtf8(userInfoParts[1]);
            } else if (userInfoParts.length == 3) {
                // Standard encoding
                // PLAIN:username:password
                // EXTERNAL:username:certAlias
                authenticationType = AuthType.valueOf(userInfoParts[0]);
                username = decodeUtf8(userInfoParts[1]);

                if (AuthType.EXTERNAL == authenticationType) {
                    clientCertificateAlias = decodeUtf8(userInfoParts[2]);
                } else {
                    password = decodeUtf8(userInfoParts[2]);
                }
            }
        }

        String path = imapUri.getPath();
        if (path != null && path.length() > 1) {
            // Strip off the leading "/"
            String cleanPath = path.substring(1);

            if (cleanPath.length() >= 2 && cleanPath.charAt(1) == '|') {
                autoDetectNamespace = cleanPath.charAt(0) == '1';
                if (!autoDetectNamespace) {
                    pathPrefix = cleanPath.substring(2);
                }
            } else {
                if (cleanPath.length() > 0) {
                    pathPrefix = cleanPath;
                    autoDetectNamespace = false;
                }
            }
        }

        Map<String, String> extra = new HashMap<>();
        extra.put("autoDetectNamespace", Boolean.toString(autoDetectNamespace));
        extra.put("pathPrefix", pathPrefix);

        return new ServerSettings("imap", host, port, connectionSecurity, authenticationType, username,
                password, clientCertificateAlias, extra);
    }
}
