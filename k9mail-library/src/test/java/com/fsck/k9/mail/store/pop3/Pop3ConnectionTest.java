package com.fsck.k9.mail.store.pop3;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.AuthenticationFailedException;
import com.fsck.k9.mail.CertificateValidationException;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.filter.Base64;
import com.fsck.k9.mail.ssl.TrustedSocketFactory;
import javax.net.ssl.SSLException;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class Pop3ConnectionTest {

    private static final String host = "server";
    private static final int port = 12345;
    private static String username = "user";
    private static String password = "password";
    private static final String INITIAL_RESPONSE = "+OK POP3 server greeting\r\n";
    private static final String AUTH = "AUTH\r\n";
    private static final String AUTH_HANDLE_RESPONSE = "+OK Listing of supported mechanisms follows\r\n" +
            "PLAIN\r\n" +
            "CRAM-MD5\r\n" +
            "EXTERNAL\r\n" +
            ".\r\n";
    private static final String AUTH_NO_AUTH_PLAIN_RESPONSE = "+OK Listing of supported mechanisms follows\r\n" +
            "CRAM-MD5\r\n" +
            "EXTERNAL\r\n" +
            ".\r\n";
    private static final String CAPA = "CAPA\r\n";
    private static final String CAPA_RESPONSE = "+OK Listing of supported mechanisms follows\r\n" +
            "PLAIN\r\n" +
            "CRAM-MD5\r\n" +
            "EXTERNAL\r\n" +
            ".\r\n";
    private static final String CAPA_NO_AUTH_PLAIN_RESPONSE = "+OK Listing of supported mechanisms follows\r\n" +
            "CRAM-MD5\r\n" +
            "EXTERNAL\r\n" +
            ".\r\n";
    private static final String LOGIN_AUTHENTICATED_RESPONSE = "+OK\r\n" + "+OK\r\n";
    private static final String LOGIN = "USER "+username+"\r\n" + "PASS "+password+"\r\n";
    private static final String AUTH_PLAIN_WITH_LOGIN = "AUTH PLAIN\r\n" +
            new String(Base64.encodeBase64(("\000"+username+"\000"+password).getBytes())) + "\r\n";
    private static final String AUTH_PLAIN_AUTHENTICATED_RESPONSE = "+OK\r\n" + "+OK\r\n";
    private static final String AUTH_PLAIN_FAILED_RESPONSE = "+OK\r\n" + "Plain authentication failure";
    private static final String STAT = "STAT\r\n";
    private static final String STAT_RESPONSE = "+OK 20 0\r\n";
    private static final String UIDL_UNSUPPORTED_RESPONSE = "-ERR UIDL unsupported\r\n";
    private static final String UIDL_SUPPORTED_RESPONSE = "+OK UIDL supported\r\n";

    private TrustedSocketFactory mockTrustedSocketFactory;
    private Socket mockSocket;
    private ByteArrayOutputStream outputStream;

    @Before
    public void before() throws Exception {
        mockTrustedSocketFactory = mock(TrustedSocketFactory.class);
        mockSocket = mock(Socket.class);
        outputStream = new ByteArrayOutputStream();
        when(mockTrustedSocketFactory.createSocket(null, "server", 12345, null)).thenReturn(mockSocket);
        when(mockSocket.getOutputStream()).thenReturn(outputStream);
        when(mockSocket.isConnected()).thenReturn(true);

    }

    @Test(expected = CertificateValidationException.class)
    public void whenTrustedSocketFactoryThrowsSSLCertificateException_throwCertificateValidationException() throws Exception {
        when(mockTrustedSocketFactory.createSocket(null, "server", 12345, null)).thenThrow(
                new SSLException(new CertificateException()));

        new Pop3Connection(
                host, port, ConnectionSecurity.SSL_TLS_REQUIRED, AuthType.PLAIN, null,
                username, password, mockTrustedSocketFactory);
    }

    @Test(expected = MessagingException.class)
    public void whenTrustedSocketFactoryThrowsCertificateException_throwMessagingException() throws Exception {
        when(mockTrustedSocketFactory.createSocket(null, "server", 12345, null)).thenThrow(
                new SSLException(""));

        new Pop3Connection(
                host, port, ConnectionSecurity.SSL_TLS_REQUIRED, AuthType.PLAIN, null,
                username, password, mockTrustedSocketFactory);
    }

    @Test(expected = MessagingException.class)
    public void whenTrustedSocketFactoryThrowsGeneralSecurityException_throwMessagingException() throws Exception {
        when(mockTrustedSocketFactory.createSocket(null, "server", 12345, null)).thenThrow(
                new NoSuchAlgorithmException(""));

        new Pop3Connection(
                host, port, ConnectionSecurity.SSL_TLS_REQUIRED, AuthType.PLAIN, null,
                username, password, mockTrustedSocketFactory);
    }

    @Test(expected = MessagingException.class)
    public void whenTrustedSocketFactoryThrowsIOException_throwMessagingException() throws Exception {
        when(mockTrustedSocketFactory.createSocket(null, "server", 12345, null)).thenThrow(
                new IOException(""));

        new Pop3Connection(
                host, port, ConnectionSecurity.SSL_TLS_REQUIRED, AuthType.PLAIN, null,
                username, password, mockTrustedSocketFactory);
    }

    @Test(expected = MessagingException.class)
    public void whenSocketNotConnected_throwsMessagingException() throws Exception {
        when(mockSocket.isConnected()).thenReturn(false);

        new Pop3Connection(
                host, port, ConnectionSecurity.SSL_TLS_REQUIRED, AuthType.PLAIN, null,
                username, password, mockTrustedSocketFactory);
    }

    @Test
    public void withAuthTypePlainAndPlainAuthCapability_performsPlainAuth() throws Exception {
        String response = INITIAL_RESPONSE +
                AUTH_HANDLE_RESPONSE +
                CAPA_RESPONSE +
                AUTH_PLAIN_AUTHENTICATED_RESPONSE;

        when(mockSocket.getInputStream()).thenReturn(new ByteArrayInputStream(response.getBytes()));

        new Pop3Connection(
                host, port, ConnectionSecurity.SSL_TLS_REQUIRED, AuthType.PLAIN, null,
                username, password, mockTrustedSocketFactory);

        assertEquals(AUTH +
                CAPA +
                AUTH_PLAIN_WITH_LOGIN, new String(outputStream.toByteArray()));
    }

    @Test(expected = AuthenticationFailedException.class)
    public void withAuthTypePlainAndPlainAuthCapabilityAndInvalidPasswordResponse_throwsException() throws Exception {
        String response = INITIAL_RESPONSE +
                AUTH_HANDLE_RESPONSE +
                CAPA_RESPONSE +
                AUTH_PLAIN_FAILED_RESPONSE;

        when(mockSocket.getInputStream()).thenReturn(new ByteArrayInputStream(response.getBytes()));

        new Pop3Connection(
                host, port, ConnectionSecurity.SSL_TLS_REQUIRED, AuthType.PLAIN, null,
                username, password, mockTrustedSocketFactory);
    }

    @Test
    public void withAuthTypePlainAndNoPlainAuthCapability_performsLogin() throws Exception {
        String response = INITIAL_RESPONSE +
                AUTH_NO_AUTH_PLAIN_RESPONSE +
                CAPA_NO_AUTH_PLAIN_RESPONSE +
                LOGIN_AUTHENTICATED_RESPONSE +
                STAT_RESPONSE;

        when(mockSocket.getInputStream()).thenReturn(new ByteArrayInputStream(response.getBytes()));

        new Pop3Connection(
                host, port, ConnectionSecurity.SSL_TLS_REQUIRED, AuthType.PLAIN, null,
                username, password, mockTrustedSocketFactory);

        assertEquals(AUTH +
                CAPA +
                LOGIN, new String(outputStream.toByteArray()));
    }
}
