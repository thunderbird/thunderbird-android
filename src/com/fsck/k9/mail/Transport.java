
package com.fsck.k9.mail;

import com.fsck.k9.Account;
import com.fsck.k9.mail.transport.SmtpTransport;
import com.fsck.k9.mail.transport.WebDavTransport;

public abstract class Transport {
    protected static final int SOCKET_CONNECT_TIMEOUT = 10000;

    // RFC 1047
    protected static final int SOCKET_READ_TIMEOUT = 300000;

    public synchronized static Transport getInstance(Account account) throws MessagingException {
        String uri = account.getTransportUri();
        if (uri.startsWith("smtp")) {
            return new SmtpTransport(account);
        } else if (uri.startsWith("webdav")) {
            return new WebDavTransport(account);
        } else {
            throw new MessagingException("Unable to locate an applicable Transport for " + uri);
        }
    }

    /**
     * Decodes the contents of transport-specific URIs and puts them into a {@link ServerSettings}
     * object.
     *
     * @param uri
     *         the transport-specific URI to decode
     *
     * @return A {@link ServerSettings} object holding the settings contained in the URI.
     *
     * @see SmtpTransport#decodeUri(String)
     * @see WebDavTransport#decodeUri(String)
     */
    public static ServerSettings decodeTransportUri(String uri) {
        if (uri.startsWith("smtp")) {
            return SmtpTransport.decodeUri(uri);
        } else if (uri.startsWith("webdav")) {
            return WebDavTransport.decodeUri(uri);
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
     *
     * @see SmtpTransport#createUri(ServerSettings)
     * @see WebDavTransport#createUri(ServerSettings)
     */
    public static String createTransportUri(ServerSettings server) {
        if (SmtpTransport.TRANSPORT_TYPE.equals(server.type)) {
            return SmtpTransport.createUri(server);
        } else if (WebDavTransport.TRANSPORT_TYPE.equals(server.type)) {
            return WebDavTransport.createUri(server);
        } else {
            throw new IllegalArgumentException("Not a valid transport URI");
        }
    }


    public abstract void open() throws MessagingException;

    public abstract void sendMessage(Message message) throws MessagingException;

    public abstract void close();
}
