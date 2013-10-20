package com.fsck.k9.mail.transport;

import com.fsck.k9.mail.store.TrustManagerFactory;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.scheme.LayeredSocketFactory;
import org.apache.http.params.HttpParams;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;

public class TrustedSocketFactory implements LayeredSocketFactory {
    private SSLSocketFactory mSocketFactory;
    private org.apache.http.conn.ssl.SSLSocketFactory mSchemeSocketFactory;

	protected static final String ENABLED_CIPHERS[];

    static {
        String preferredCiphers[] = {
            "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
            "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
            "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
            "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA",
            "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
            "TLS_DHE_RSA_WITH_AES_256_CBC_SHA",
            "TLS_DHE_DSS_WITH_AES_128_CBC_SHA",
            "TLS_ECDHE_RSA_WITH_RC4_128_SHA",
            "TLS_ECDHE_ECDSA_WITH_RC4_128_SHA",
            "TLS_RSA_WITH_AES_128_CBC_SHA",
            "TLS_RSA_WITH_AES_256_CBC_SHA",
            "SSL_RSA_WITH_3DES_EDE_CBC_SHA",
            "SSL_RSA_WITH_RC4_128_SHA",
            "SSL_RSA_WITH_RC4_128_MD5",
        };

        String[] supportedCiphers = null;

        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, null, new SecureRandom());
            SSLSocketFactory sf = sslContext.getSocketFactory();
            supportedCiphers = sf.getSupportedCipherSuites();
        } catch (KeyManagementException kme) {
            kme.printStackTrace();
        } catch (NoSuchAlgorithmException nsae) {
            nsae.printStackTrace();
        }

        ENABLED_CIPHERS = supportedCiphers == null ? null :
            filterBySupport(preferredCiphers, supportedCiphers);
    }

    protected static String[] filterBySupport(String[] preferred, String[] supported) {
        List<String> enabled = new ArrayList<String>();
        Set<String> available = new HashSet<String>();
        Collections.addAll(available, supported);

        for (String item : preferred) {
            if (available.contains(item)) enabled.add(item);
        }
        return enabled.toArray(new String[enabled.size()]);
    }

    protected static final String ENABLED_PROTOCOLS[] = {
        "TLSv1.2", "TLSv1.1", "TLSv1"
    };

    public TrustedSocketFactory(String host, boolean secure) throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[] {
                            TrustManagerFactory.get(host, secure)
                        }, new SecureRandom());
        mSocketFactory = sslContext.getSocketFactory();
        mSchemeSocketFactory = org.apache.http.conn.ssl.SSLSocketFactory.getSocketFactory();
        mSchemeSocketFactory.setHostnameVerifier(
            org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
    }

    public Socket connectSocket(Socket sock, String host, int port,
                                InetAddress localAddress, int localPort, HttpParams params)
    throws IOException, UnknownHostException, ConnectTimeoutException {
        return mSchemeSocketFactory.connectSocket(sock, host, port, localAddress, localPort, params);
    }

    public Socket createSocket() throws IOException {
        return mSocketFactory.createSocket();
    }

    public boolean isSecure(Socket sock) throws IllegalArgumentException {
        return mSchemeSocketFactory.isSecure(sock);
    }

    public static void hardenSocket(SSLSocket sock) {
        if (ENABLED_CIPHERS != null) {
            sock.setEnabledCipherSuites(ENABLED_CIPHERS);
        }
        sock.setEnabledProtocols(ENABLED_PROTOCOLS);
    }

    public Socket createSocket(
        final Socket socket,
        final String host,
        final int port,
        final boolean autoClose
    ) throws IOException, UnknownHostException {
        SSLSocket sslSocket = (SSLSocket) mSocketFactory.createSocket(
                                  socket,
                                  host,
                                  port,
                                  autoClose
                              );
        //hostnameVerifier.verify(host, sslSocket);
        // verifyHostName() didn't blowup - good!
        hardenSocket(sslSocket);
        return sslSocket;
    }
}
