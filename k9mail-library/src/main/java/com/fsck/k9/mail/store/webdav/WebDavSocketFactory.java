package com.fsck.k9.mail.store.webdav;

import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.scheme.LayeredSocketFactory;
import org.apache.http.params.HttpParams;

import com.fsck.k9.mail.ssl.TrustedSocketFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

/**
 * Provides a factory for creating WebDAV capable sockets.
 *
 * <h3>Design Goals</h3>
 * The WebDAV SocketFactory has two objectives:
 *
 * 1. Implement the (deprecated) SocketFactory API
 * 2. Use our TrustedSocketFactory implementation
 *
 * To do this we implemented a layered socket factory. In this approach we need two socket factories.
 * We can't just use SchemeSocketFactory as we want to be able to use custom self-trusted
 * certificates on the fly.
 *
 * When asked to create a socket, we call the TrustedSocketFactory to provide us with a socket
 * that uses the SSLContext we want. We do the hostname and certificate verification here.
 *
 * When asked to connect a socket using the API we use the Apache SSLSocketFactory.
 * We don't need to do any hostname verification here because we'll be using a socket that
 * has already been created by us. If we do strict verification here,
 * we can't use our self-trusted certificates.
 *
 * <h4>Developer Notes</h4>
 *
 * The Apache SSLSocketFactory we create internally must NEVER be used to create a Socket,
 * otherwise we can leak sockets that do no hostname verification at all.
 *
 * We thus have the following layering model:
 *
 * org.apache.http.conn.ssl.SSLSocketFactory
 * TrustedSocketFactory.SSLSocketFactory
 *
 * Which produces an SSLSocket (with accepted self-signed alias if necessary)
 * that has the relevant connection timeouts, is bound locally and has HTTP params
 * associated with it.
 *
 * <h4>Deprecated API Usage</h4>
 *
 * We are using the deprecated API because the alternative is hand-rolling the
 * entire HTTP stack support plus WebDAV. While this would be nicer it's an awful lot of work.
 *
 * The DefaultHttpClient and associated API is only really deprecated for use as a normal HTTP GET/POST
 * stack (the {@link java.net.HttpURLConnection}/{@link javax.net.ssl.HttpsURLConnection} classes
 * are designed for that).
 */
public class WebDavSocketFactory implements LayeredSocketFactory {
    //Optional alias for referencing user-trusted certificate.
    private final String mCertificateAlias;
    private final String mDefaultHost;
    private final int mDefaultPort;
    // For creating secure sockets (with optional user-trusted certificate)
    private TrustedSocketFactory mTrustedSocketFactory;
    // For connecting existing sockets with the correct HTTP params and local binding
    private org.apache.http.conn.ssl.SSLSocketFactory mSchemeSocketFactory;

    /**
     * Create a new socket factory, using the given trusted socket factory and alias.
     */
    public WebDavSocketFactory(TrustedSocketFactory trustedSocketFactory,
                               org.apache.http.conn.ssl.SSLSocketFactory internalSocketFactory,
                               String defaultHost,
                               int defaultPort,
                               String certificateAlias) {
        mTrustedSocketFactory = trustedSocketFactory;
        mSchemeSocketFactory = internalSocketFactory;
        mSchemeSocketFactory.setHostnameVerifier(
                org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

        mCertificateAlias = certificateAlias;
        mDefaultHost = defaultHost;
        mDefaultPort = defaultPort;
    }

    @Override
    public Socket createSocket() throws IOException {
        return createSocket(null, mDefaultHost, mDefaultPort);
    }

    @Override
    public Socket createSocket(final Socket socket, final String host, final int port,
                               final boolean autoClose) throws IOException {
        if (!autoClose)
            throw new IOException("We don't support non-auto close sockets");
        return createSocket(socket, host, port);
    }

    private Socket createSocket(
            final Socket socket,
            final String host,
            final int port) throws IOException {
        try {
            return mTrustedSocketFactory.createSocket(
                    socket,
                    host,
                    port,
                    mCertificateAlias);
        } catch (Exception e) {
            throw new IOException("Exception creating trusted socket", e);
        }
    }

    @Override
    public Socket connectSocket(Socket sock, String host, int port,
                                InetAddress localAddress, int localPort, HttpParams params)
            throws IOException, ConnectTimeoutException {
        if (sock == null) {
            //We don't want to delegate creation - we just want the Scheme to wrap a secure socket.
            sock = createSocket(null, host, port);
        }
        return mSchemeSocketFactory.connectSocket(sock, host, port, localAddress, localPort, params);
    }

    @Override
    public boolean isSecure(Socket sock) throws IllegalArgumentException {
        return mSchemeSocketFactory.isSecure(sock) && mTrustedSocketFactory.isSecure(sock);
    }
}
