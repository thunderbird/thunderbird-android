package com.fsck.k9.preferences.migration.migration12;


import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;

import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.ServerSettings;


public class SmtpTransportUriDecoder {
    private static final int DEFAULT_PORT = 587;
    private static final int DEFAULT_TLS_PORT = 465;

    /**
     * Decodes a SmtpTransport URI.
     *
     * NOTE: In contrast to ImapStore and Pop3Store, the authType is appended at the end!
     *
     * <p>Possible forms:</p>
     * <pre>
     * smtp://user:password:auth@server:port ConnectionSecurity.NONE
     * smtp+tls+://user:password:auth@server:port ConnectionSecurity.STARTTLS_REQUIRED
     * smtp+ssl+://user:password:auth@server:port ConnectionSecurity.SSL_TLS_REQUIRED
     * </pre>
     */
    public static ServerSettings decodeSmtpUri(String uri) {
        String host;
        int port;
        ConnectionSecurity connectionSecurity;
        AuthType authType = AuthType.PLAIN;
        String username = "";
        String password = null;
        String clientCertificateAlias = null;

        URI smtpUri;
        try {
            smtpUri = new URI(uri);
        } catch (URISyntaxException use) {
            throw new IllegalArgumentException("Invalid SmtpTransport URI", use);
        }

        String scheme = smtpUri.getScheme();
        /*
         * Currently available schemes are:
         * smtp
         * smtp+tls+
         * smtp+ssl+
         *
         * The following are obsolete schemes that may be found in pre-existing
         * settings from earlier versions or that may be found when imported. We
         * continue to recognize them and re-map them appropriately:
         * smtp+tls
         * smtp+ssl
         */
        if (scheme.equals("smtp")) {
            connectionSecurity = ConnectionSecurity.NONE;
            port = DEFAULT_PORT;
        } else if (scheme.startsWith("smtp+tls")) {
            connectionSecurity = ConnectionSecurity.STARTTLS_REQUIRED;
            port = DEFAULT_PORT;
        } else if (scheme.startsWith("smtp+ssl")) {
            connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED;
            port = DEFAULT_TLS_PORT;
        } else {
            throw new IllegalArgumentException("Unsupported protocol (" + scheme + ")");
        }

        host = smtpUri.getHost();

        if (smtpUri.getPort() != -1) {
            port = smtpUri.getPort();
        }

        if (smtpUri.getUserInfo() != null) {
            String[] userInfoParts = smtpUri.getUserInfo().split(":");
            if (userInfoParts.length == 1) {
                username = decodeUtf8(userInfoParts[0]);
            } else if (userInfoParts.length == 2) {
                username = decodeUtf8(userInfoParts[0]);
                password = decodeUtf8(userInfoParts[1]);
            } else if (userInfoParts.length == 3) {
                // NOTE: In SmtpTransport URIs, the authType comes last!
                authType = AuthType.valueOf(userInfoParts[2]);
                username = decodeUtf8(userInfoParts[0]);
                if (authType == AuthType.EXTERNAL) {
                    clientCertificateAlias = decodeUtf8(userInfoParts[1]);
                } else {
                    password = decodeUtf8(userInfoParts[1]);
                }
            }
        }

        return new ServerSettings("smtp", host, port, connectionSecurity,
                authType, username, password, clientCertificateAlias);
    }

    private static String decodeUtf8(String s) {
        try {
            return URLDecoder.decode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 not found");
        }
    }
}
