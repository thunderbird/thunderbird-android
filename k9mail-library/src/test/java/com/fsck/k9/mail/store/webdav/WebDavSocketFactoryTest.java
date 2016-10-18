package com.fsck.k9.mail.store.webdav;

import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.ssl.TrustedSocketFactory;

import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 21)
public class WebDavSocketFactoryTest {
    private static final String DEFAULT_HOST = "defaultHost.com";
    private static final int DEFAULT_PORT = 1000;
    private static final String CERTIFICATE_ALIAS = "certificateAlias";
    private WebDavSocketFactory webDavSocketFactory;
    @Mock
    private TrustedSocketFactory trustedSocketFactory;
    @Mock
    private SSLSocketFactory apacheSocketFactory;
    @Mock
    private SSLSocket trustedSocket;
    @Mock
    private SSLSocket customAddressTrustedSocket;
    @Mock
    private SSLSocket wrappedTrustedSocket;
    @Mock
    private SSLSocket wrappedCustomAddressTrustedSocket;
    @Mock
    private Socket unknownSocket;
    @Mock
    private SSLSession sslSession = mock(SSLSession.class);
    @Mock
    private HttpParams params;

    private ArgumentCaptor<InetSocketAddress> inetSocketAddressCaptor;

    private String otherHost = "otherHost.com";
    private int otherPort = 10001;
    private InetAddress localAddress = mock(InetAddress.class);
    private int localPort = 10001;
    private int connectionTimeout = 1030;


    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        webDavSocketFactory = new WebDavSocketFactory(trustedSocketFactory, apacheSocketFactory,
                DEFAULT_HOST, DEFAULT_PORT, CERTIFICATE_ALIAS);
        when(trustedSocketFactory.createSocket(null, DEFAULT_HOST, DEFAULT_PORT, CERTIFICATE_ALIAS))
                .thenReturn(trustedSocket);

        when(trustedSocketFactory.createSocket(null, otherHost, otherPort, CERTIFICATE_ALIAS))
                .thenReturn(customAddressTrustedSocket);
        when(apacheSocketFactory.connectSocket(trustedSocket, otherHost, otherPort, localAddress, localPort, params))
                .thenReturn(wrappedTrustedSocket);
        when(apacheSocketFactory.connectSocket(customAddressTrustedSocket, otherHost, otherPort,
                localAddress, localPort, params)).thenReturn(wrappedCustomAddressTrustedSocket);
        when(params.getIntParameter(eq(CoreConnectionPNames.CONNECTION_TIMEOUT), anyInt()))
                .thenReturn(connectionTimeout);
        when(customAddressTrustedSocket.getSession()).thenReturn(sslSession);
    }

    @Test
    public void createSocket_usesTrustedSocketFactoryToCreateSocket() throws IOException {
        Socket socket = webDavSocketFactory.createSocket();

        assertSame(trustedSocket, socket);
    }

    @Test
    public void createSocket_withCustomHostPort_usesTrustedSocketFactoryToCreateSocket()
            throws IOException, NoSuchAlgorithmException, MessagingException, KeyManagementException {
        Socket customAddressTrustedSocket = mock(Socket.class);
        String host = "otherHost.com";
        int port = 10001;
        when(trustedSocketFactory.createSocket(null, host, port, CERTIFICATE_ALIAS)).thenReturn(customAddressTrustedSocket);

        Socket socket = webDavSocketFactory.createSocket(null, host, port, true);

        assertSame(customAddressTrustedSocket, socket);
    }


    @Test
    public void connectSocket_withNoSocket_passesTrustedSocketToApacheSocketFactory()
            throws IOException, NoSuchAlgorithmException, MessagingException, KeyManagementException {
        webDavSocketFactory.connectSocket(null, otherHost, otherPort,
                localAddress, localPort, params);

        verify(apacheSocketFactory).connectSocket(customAddressTrustedSocket, otherHost, otherPort,
                localAddress, localPort, params);
    }

    @Test
    public void connectSocket_withNoSocket_providesWrappedCustomSocket()
            throws IOException, NoSuchAlgorithmException, MessagingException, KeyManagementException {
        Socket socket = webDavSocketFactory.connectSocket(null, otherHost, otherPort,
                localAddress, localPort, params);

        assertSame(wrappedCustomAddressTrustedSocket, socket);
    }

    @Test
    public void connectSocket_bindsProvidedSocketToLocalAddressAndUsesHttpParams()
            throws IOException, NoSuchAlgorithmException, MessagingException, KeyManagementException {
        webDavSocketFactory.connectSocket(unknownSocket, otherHost, otherPort, localAddress, localPort, params);

        verify(apacheSocketFactory).connectSocket(unknownSocket, otherHost, otherPort,
                localAddress, localPort, params);
    }

    @Test
    public void isSecure_whenApacheSaysNo_isFalse()
            throws IOException, NoSuchAlgorithmException, MessagingException, KeyManagementException {
        when(apacheSocketFactory.isSecure(unknownSocket)).thenReturn(false);
        when(trustedSocketFactory.isSecure(unknownSocket)).thenReturn(true);

        assertFalse(webDavSocketFactory.isSecure(unknownSocket));
    }

    @Test
    public void isSecure_whenTrustedSaysNo_isFalse()
            throws IOException, NoSuchAlgorithmException, MessagingException, KeyManagementException {
        when(apacheSocketFactory.isSecure(unknownSocket)).thenReturn(true);
        when(trustedSocketFactory.isSecure(unknownSocket)).thenReturn(false);

        assertFalse(webDavSocketFactory.isSecure(unknownSocket));
    }

    @Test
    public void isSecure_whenBothSaySecure_isTrue()
            throws IOException, NoSuchAlgorithmException, MessagingException, KeyManagementException {
        when(apacheSocketFactory.isSecure(unknownSocket)).thenReturn(true);
        when(trustedSocketFactory.isSecure(unknownSocket)).thenReturn(true);

        assertTrue(webDavSocketFactory.isSecure(unknownSocket));
    }

}
