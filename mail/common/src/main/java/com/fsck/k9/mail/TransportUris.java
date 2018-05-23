package com.fsck.k9.mail;


import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;

import com.fsck.k9.mail.ServerSettings.Type;
import com.fsck.k9.mail.store.webdav.WebDavStore;


public class TransportUris {
    /**
     * Decodes the contents of transport-specific URIs and puts them into a {@link ServerSettings}
     * object.
     *
     * @param uri
     *         the transport-specific URI to decode
     *
     * @return A {@link ServerSettings} object holding the settings contained in the URI.
     */
    public static ServerSettings decodeTransportUri(String uri) {
        if (uri.startsWith("smtp")) {
            return decodeSmtpUri(uri);
        } else if (uri.startsWith("webdav")) {
            return decodeWebDavUri(uri);
        } else {
            throw new IllegalArgumentException("Not a valid transport URI");
        }
    }

    /**
     * Creates a transport URI from the information supplied in the {@link ServerSettings} object.
     *
     * @param server
     *         The {@link ServerSettings} object that holds the server settings.
     *
     * @return A transport URI that holds the same information as the {@code server} parameter.
     */
    public static String createTransportUri(ServerSettings server) {
        if (Type.SMTP == server.type) {
            return createSmtpUri(server);
        } else if (Type.WebDAV == server.type) {
            return createWebDavUri(server);
        } else {
            throw new IllegalArgumentException("Not a valid transport URI");
        }
    }


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
    private static ServerSettings decodeSmtpUri(String uri) {
        String host;
        int port;
        ConnectionSecurity connectionSecurity;
        AuthType authType = null;
        String username = null;
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
            port = ServerSettings.Type.SMTP.defaultPort;
        } else if (scheme.startsWith("smtp+tls")) {
            connectionSecurity = ConnectionSecurity.STARTTLS_REQUIRED;
            port = ServerSettings.Type.SMTP.defaultPort;
        } else if (scheme.startsWith("smtp+ssl")) {
            connectionSecurity = ConnectionSecurity.SSL_TLS_REQUIRED;
            port = ServerSettings.Type.SMTP.defaultTlsPort;
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
                authType = AuthType.PLAIN;
                username = decodeUtf8(userInfoParts[0]);
            } else if (userInfoParts.length == 2) {
                authType = AuthType.PLAIN;
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

        return new ServerSettings(ServerSettings.Type.SMTP, host, port, connectionSecurity,
                authType, username, password, clientCertificateAlias);
    }

    /**
     * Creates a SmtpTransport URI with the supplied settings.
     *
     * @param server
     *         The {@link ServerSettings} object that holds the server settings.
     *
     * @return A SmtpTransport URI that holds the same information as the {@code server} parameter.
     *
     * @see com.fsck.k9.mail.store.StoreConfig#getTransportUri()
     */
    private static String createSmtpUri(ServerSettings server) {
        String userEnc = (server.username != null) ?
                encodeUtf8(server.username) : "";
        String passwordEnc = (server.password != null) ?
                encodeUtf8(server.password) : "";
        String clientCertificateAliasEnc = (server.clientCertificateAlias != null) ?
                encodeUtf8(server.clientCertificateAlias) : "";

        String scheme;
        switch (server.connectionSecurity) {
            case SSL_TLS_REQUIRED:
                scheme = "smtp+ssl+";
                break;
            case STARTTLS_REQUIRED:
                scheme = "smtp+tls+";
                break;
            default:
            case NONE:
                scheme = "smtp";
                break;
        }

        String userInfo;
        AuthType authType = server.authenticationType;
        // NOTE: authType is append at last item, in contrast to ImapStore and Pop3Store!
        if (authType != null) {
            if (AuthType.EXTERNAL == authType) {
                userInfo = userEnc + ":" + clientCertificateAliasEnc + ":" + authType.name();
            } else {
                userInfo = userEnc + ":" + passwordEnc + ":" + authType.name();
            }
        } else {
            userInfo = userEnc + ":" + passwordEnc;
        }
        try {
            return new URI(scheme, userInfo, server.host, server.port, null, null,
                    null).toString();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Can't create SmtpTransport URI", e);
        }
    }

    /**
     * Decodes a WebDavTransport URI.
     *
     * <p>
     * <b>Note:</b> Everything related to sending messages via WebDAV is handled by
     * {@link WebDavStore}. So the transport URI is the same as the store URI.
     * </p>
     */
    private static ServerSettings decodeWebDavUri(String uri) {
        return WebDavStore.decodeUri(uri);
    }

    /**
     * Creates a WebDavTransport URI.
     *
     * <p>
     * <b>Note:</b> Everything related to sending messages via WebDAV is handled by
     * {@link WebDavStore}. So the transport URI is the same as the store URI.
     * </p>
     */
    private static String createWebDavUri(ServerSettings server) {
        return WebDavStore.createUri(server);
    }

    private static String encodeUtf8(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 not found");
        }
    }

    private static String decodeUtf8(String s) {
        try {
            return URLDecoder.decode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 not found");
        }
    }
}
