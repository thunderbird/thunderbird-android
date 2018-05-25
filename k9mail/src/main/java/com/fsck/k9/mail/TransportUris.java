package com.fsck.k9.mail;


import com.fsck.k9.mail.ServerSettings.Type;
import com.fsck.k9.mail.store.webdav.WebDavStore;
import com.fsck.k9.mail.transport.smtp.SmtpTransportUriCreator;
import com.fsck.k9.mail.transport.smtp.SmtpTransportUriDecoder;


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
        return SmtpTransportUriDecoder.decodeSmtpUri(uri);
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
        return SmtpTransportUriCreator.createSmtpUri(server);
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
}
