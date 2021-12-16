package com.fsck.k9.mail.store.pop3;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.AuthenticationFailedException;
import com.fsck.k9.mail.CertificateValidationException;
import com.fsck.k9.mail.CertificateValidationException.Reason;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.filter.Base64;
import com.fsck.k9.mail.helpers.TestTrustedSocketFactory;
import com.fsck.k9.mail.ssl.TrustedSocketFactory;
import javax.net.ssl.SSLException;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;


public class Pop3ConnectionTest {

    private static final String host = "server";
    private static final int port = 12345;
    private static String username = "user";
    private static String password = "password";
    private static final String INITIAL_RESPONSE = "+OK POP3 server greeting\r\n";
    private static final String AUTH = "AUTH\r\n";
    private static final String AUTH_HANDLE_RESPONSE =
            "+OK Listing of supported mechanisms follows\r\n" +
            "PLAIN\r\n" +
            "CRAM-MD5\r\n" +
            "EXTERNAL\r\n" +
            ".\r\n";
    private static final String CAPA =
            "CAPA\r\n";
    private static final String CAPA_RESPONSE =
            "+OK Listing of supported mechanisms follows\r\n" +
            "PLAIN\r\n" +
            "CRAM-MD5\r\n" +
            "EXTERNAL\r\n" +
            ".\r\n";
    private static final String AUTH_PLAIN_WITH_LOGIN = "AUTH PLAIN\r\n" +
            new String(Base64.encodeBase64(("\000"+username+"\000"+password).getBytes())) + "\r\n";
    private static final String AUTH_PLAIN_AUTHENTICATED_RESPONSE = "+OK\r\n" + "+OK\r\n";

    private static final String SUCCESSFUL_PLAIN_AUTH = AUTH + CAPA + AUTH_PLAIN_WITH_LOGIN;
    private static final String SUCCESSFUL_PLAIN_AUTH_RESPONSE =
            INITIAL_RESPONSE +
            AUTH_HANDLE_RESPONSE +
            CAPA_RESPONSE +
            AUTH_PLAIN_AUTHENTICATED_RESPONSE;
/**
    private static final String AUTH_PLAIN_FAILED_RESPONSE = "+OK\r\n" + "Plain authentication failure";
    private static final String STAT = "STAT\r\n";
    private static final String STAT_RESPONSE = "+OK 20 0\r\n";
    private static final String UIDL_UNSUPPORTED_RESPONSE = "-ERR UIDL unsupported\r\n";
    private static final String UIDL_SUPPORTED_RESPONSE = "+OK UIDL supported\r\n";
 **/

    private TrustedSocketFactory mockTrustedSocketFactory;
    private Socket mockSocket;
    private ByteArrayOutputStream outputStreamForMockSocket;
    private SimplePop3Settings settings;
    private TrustedSocketFactory socketFactory;

    @Before
    public void before() throws Exception {
        createCommonSettings();
        createMocks();
        socketFactory = TestTrustedSocketFactory.newInstance();
    }

    private void createCommonSettings() {
        settings = new SimplePop3Settings();
        settings.setUsername(username);
        settings.setPassword(password);
    }

    private void createMocks()
            throws MessagingException, IOException, NoSuchAlgorithmException, KeyManagementException {
        mockTrustedSocketFactory = mock(TrustedSocketFactory.class);
        mockSocket = mock(Socket.class);
        outputStreamForMockSocket = new ByteArrayOutputStream();
        when(mockTrustedSocketFactory.createSocket(null, host, port, null))
                .thenReturn(mockSocket);
        when(mockSocket.getOutputStream()).thenReturn(outputStreamForMockSocket);
        when(mockSocket.isConnected()).thenReturn(true);
    }

    private void addSettingsForValidMockSocket() {
        settings.setHost(host);
        settings.setPort(port);
        settings.setConnectionSecurity(ConnectionSecurity.SSL_TLS_REQUIRED);
    }

    @Test
    public void constructor_doesntCreateSocket() throws Exception {
        addSettingsForValidMockSocket();
        settings.setAuthType(AuthType.PLAIN);

        new Pop3Connection(settings, mockTrustedSocketFactory);

        verifyNoMoreInteractions(mockTrustedSocketFactory);
    }

    //Using MockSocketFactory

    @Test(expected = CertificateValidationException.class)
    public void open_whenTrustedSocketFactoryThrowsSSLCertificateException_throwCertificateValidationException()
            throws Exception {
        when(mockTrustedSocketFactory.createSocket(null, host, port, null)).thenThrow(
                new SSLException(new CertificateException()));
        addSettingsForValidMockSocket();
        settings.setAuthType(AuthType.PLAIN);

        Pop3Connection connection = new Pop3Connection(settings, mockTrustedSocketFactory);
        connection.open();
    }

    @Test(expected = MessagingException.class)
    public void open_whenTrustedSocketFactoryThrowsCertificateException_throwMessagingException() throws Exception {
        when(mockTrustedSocketFactory.createSocket(null, host, port, null)).thenThrow(
                new SSLException(""));
        addSettingsForValidMockSocket();
        settings.setAuthType(AuthType.PLAIN);

        Pop3Connection connection = new Pop3Connection(settings, mockTrustedSocketFactory);
        connection.open();
    }

    @Test(expected = MessagingException.class)
    public void open_whenTrustedSocketFactoryThrowsGeneralSecurityException_throwMessagingException() throws Exception {
        when(mockTrustedSocketFactory.createSocket(null, host, port, null)).thenThrow(
                new NoSuchAlgorithmException(""));
        addSettingsForValidMockSocket();
        settings.setAuthType(AuthType.PLAIN);

        Pop3Connection connection = new Pop3Connection(settings, mockTrustedSocketFactory);
        connection.open();
    }

    @Test(expected = MessagingException.class)
    public void open_whenTrustedSocketFactoryThrowsIOException_throwMessagingException() throws Exception {
        when(mockTrustedSocketFactory.createSocket(null, host, port, null)).thenThrow(
                new IOException(""));
        addSettingsForValidMockSocket();
        settings.setAuthType(AuthType.PLAIN);

        Pop3Connection connection = new Pop3Connection(settings, mockTrustedSocketFactory);
        connection.open();
    }

    @Test(expected = MessagingException.class)
    public void open_whenSocketNotConnected_throwsMessagingException() throws Exception {
        when(mockSocket.isConnected()).thenReturn(false);
        addSettingsForValidMockSocket();
        settings.setAuthType(AuthType.PLAIN);

        Pop3Connection connection = new Pop3Connection(settings, mockTrustedSocketFactory);
        connection.open();
    }

    @Test
    public void open_withTLS_authenticatesOverSocket() throws Exception {
        when(mockSocket.getInputStream()).thenReturn(new ByteArrayInputStream(SUCCESSFUL_PLAIN_AUTH_RESPONSE.getBytes()));
        addSettingsForValidMockSocket();
        settings.setAuthType(AuthType.PLAIN);

        Pop3Connection connection = new Pop3Connection(settings, mockTrustedSocketFactory);
        connection.open();

        assertEquals(SUCCESSFUL_PLAIN_AUTH, new String(outputStreamForMockSocket.toByteArray()));
    }



    //Using both

    @Test(expected = CertificateValidationException.class)
    public void open_withSTLSunavailable_throwsCertificateValidationException() throws Exception {
        MockPop3Server server = setupUnavailableStartTLSConnection();
        settings.setAuthType(AuthType.PLAIN);
        settings.setConnectionSecurity(ConnectionSecurity.STARTTLS_REQUIRED);

        createAndOpenPop3Connection(settings, mockTrustedSocketFactory);
    }

    @Test
    public void open_withSTLSunavailable_doesntCreateSocket() throws Exception {
        MockPop3Server server = setupUnavailableStartTLSConnection();
        settings.setAuthType(AuthType.PLAIN);
        settings.setConnectionSecurity(ConnectionSecurity.STARTTLS_REQUIRED);

        try {
            Pop3Connection connection = new Pop3Connection(settings, mockTrustedSocketFactory);
            connection.open();
        } catch (Exception ignored) {
        }

        verify(mockTrustedSocketFactory, never()).createSocket(any(Socket.class), anyString(),
                anyInt(), anyString());
    }

    @Test(expected = Pop3ErrorResponse.class)
    public void open_withStartTLS_withSTLSerr_throwsException() throws Exception {
        MockPop3Server server = setupFailedStartTLSConnection();

        when(mockTrustedSocketFactory.createSocket(
                any(Socket.class), eq(server.getHost()), eq(server.getPort()), eq((String) null)))
                .thenReturn(mockSocket);
        when(mockSocket.getInputStream()).thenReturn(new ByteArrayInputStream(SUCCESSFUL_PLAIN_AUTH_RESPONSE.getBytes()));

        createAndOpenPop3Connection(settings, mockTrustedSocketFactory);
    }

    @Test
    public void open_withStartTLS_withSTLSerr_doesntCreateSocket() throws Exception {
        MockPop3Server server = setupFailedStartTLSConnection();

        when(mockTrustedSocketFactory.createSocket(
                any(Socket.class), eq(server.getHost()), eq(server.getPort()), eq((String) null)))
                .thenReturn(mockSocket);
        when(mockSocket.getInputStream()).thenReturn(new ByteArrayInputStream(SUCCESSFUL_PLAIN_AUTH_RESPONSE.getBytes()));

        try {
            createAndOpenPop3Connection(settings, mockTrustedSocketFactory);
        } catch (Exception ignored) {
        }

        verify(mockTrustedSocketFactory, never()).createSocket(any(Socket.class), anyString(),
                anyInt(), anyString());
    }

    @Test
    public void open_withStartTLS_usesSocketFactoryToCreateTLSSocket() throws Exception {
        MockPop3Server server = setupStartTLSConnection();
        settings.setAuthType(AuthType.PLAIN);

        when(mockTrustedSocketFactory.createSocket(
                any(Socket.class), eq(server.getHost()), eq(server.getPort()), eq((String) null)))
                .thenReturn(mockSocket);
        when(mockSocket.getInputStream()).thenReturn(new ByteArrayInputStream(SUCCESSFUL_PLAIN_AUTH_RESPONSE.getBytes()));

        createAndOpenPop3Connection(settings, mockTrustedSocketFactory);

        verify(mockTrustedSocketFactory).createSocket(any(Socket.class), eq(server.getHost()),
                eq(server.getPort()), eq((String) null));
    }

    @Test(expected = MessagingException.class)
    public void open_withStartTLS_whenSocketFactoryThrowsException_ThrowsException() throws Exception {
        MockPop3Server server = setupStartTLSConnection();
        settings.setAuthType(AuthType.PLAIN);

        when(mockTrustedSocketFactory.createSocket(
                any(Socket.class), eq(server.getHost()), eq(server.getPort()), eq((String) null)))
                .thenThrow(new IOException());
        when(mockSocket.getInputStream()).thenReturn(new ByteArrayInputStream(SUCCESSFUL_PLAIN_AUTH_RESPONSE.getBytes()));

        createAndOpenPop3Connection(settings, mockTrustedSocketFactory);

        verify(mockTrustedSocketFactory).createSocket(any(Socket.class), eq(server.getHost()),
                eq(server.getPort()), eq((String) null));
    }

    @Test
    public void open_withStartTLS_authenticatesOverSecureSocket() throws Exception {
        MockPop3Server server = setupStartTLSConnection();
        settings.setAuthType(AuthType.PLAIN);

        when(mockTrustedSocketFactory.createSocket(
                any(Socket.class), eq(server.getHost()), eq(server.getPort()), eq((String) null)))
                .thenReturn(mockSocket);
        when(mockSocket.getInputStream()).thenReturn(new ByteArrayInputStream(SUCCESSFUL_PLAIN_AUTH_RESPONSE.getBytes()));

        createAndOpenPop3Connection(settings, mockTrustedSocketFactory);

        assertEquals(SUCCESSFUL_PLAIN_AUTH, new String(outputStreamForMockSocket.toByteArray()));
    }

    private MockPop3Server setupStartTLSConnection() throws IOException {new MockPop3Server();
        MockPop3Server server = new MockPop3Server();
        setupServerWithStartTLSAvailable(server);
        server.expect("STLS");
        server.output("+OK Begin TLS negotiation");
        server.start();
        settings.setHost(server.getHost());
        settings.setPort(server.getPort());
        settings.setConnectionSecurity(ConnectionSecurity.STARTTLS_REQUIRED);
        return server;
    }

    private MockPop3Server setupFailedStartTLSConnection() throws IOException {new MockPop3Server();
        MockPop3Server server = new MockPop3Server();
        setupServerWithStartTLSAvailable(server);
        server.expect("STLS");
        server.output("-ERR Unavailable");
        server.start();
        settings.setHost(server.getHost());
        settings.setPort(server.getPort());
        settings.setConnectionSecurity(ConnectionSecurity.STARTTLS_REQUIRED);
        return server;
    }

    private MockPop3Server setupUnavailableStartTLSConnection() throws IOException {new MockPop3Server();
        MockPop3Server server = new MockPop3Server();
        server.output("+OK POP3 server greeting");
        server.expect("AUTH");
        server.output("+OK Listing of supported mechanisms follows");
        server.output("PLAIN");
        server.output(".");
        server.expect("CAPA");
        server.output("+OK Listing of supported mechanisms follows");
        server.output(".");
        server.start();
        settings.setHost(server.getHost());
        settings.setPort(server.getPort());
        settings.setConnectionSecurity(ConnectionSecurity.STARTTLS_REQUIRED);
        return server;
    }

    private void setupServerWithStartTLSAvailable(MockPop3Server server) {
        server.output("+OK POP3 server greeting");
        server.expect("AUTH");
        server.output("+OK Listing of supported mechanisms follows");
        server.output("PLAIN");
        server.output(".");
        server.expect("CAPA");
        server.output("+OK Listing of supported mechanisms follows");
        server.output("STLS");
        server.output(".");
    }

    //Using RealSocketFactory with MockPop3Server

    @Test
    public void open_withAuthTypePlainAndPlainAuthCapability_performsPlainAuth() throws Exception {
        settings.setAuthType(AuthType.PLAIN);

        MockPop3Server server = new MockPop3Server();
        server.output("+OK POP3 server greeting");
        server.expect("AUTH");
        server.output("+OK Listing of supported mechanisms follows");
        server.output("PLAIN");
        server.output("CRAM-MD5");
        server.output("EXTERNAL");
        server.output(".");
        server.expect("CAPA");
        server.output("+OK Listing of supported mechanisms follows");
        server.output("PLAIN");
        server.output("CRAM-MD5");
        server.output("EXTERNAL");
        server.output(".");
        server.expect("AUTH PLAIN");
        server.output("+OK");
        server.expect(new String(Base64.encodeBase64(("\000"+username+"\000"+password).getBytes())));
        server.output("+OK");
        startServerAndCreateOpenConnection(server);

        server.verifyConnectionStillOpen();
        server.verifyInteractionCompleted();
    }

    @Test
    public void open_withAuthTypePlainAndPlainAuthCapabilityAndInvalidPasswordResponse_throwsException() throws Exception {
        settings.setAuthType(AuthType.PLAIN);

        MockPop3Server server = new MockPop3Server();
        server.output("+OK POP3 server greeting");
        server.expect("AUTH");
        server.output("+OK Listing of supported mechanisms follows");
        server.output("PLAIN");
        server.output("CRAM-MD5");
        server.output("EXTERNAL");
        server.output(".");
        server.expect("CAPA");
        server.output("+OK Listing of supported mechanisms follows");
        server.output("PLAIN");
        server.output("CRAM-MD5");
        server.output("EXTERNAL");
        server.output(".");
        server.expect("AUTH PLAIN");
        server.output("+OK");
        server.expect(new String(Base64.encodeBase64(("\000"+username+"\000"+password).getBytes())));
        server.output("-ERR");

        try {
            startServerAndCreateOpenConnection(server);
            fail("Expected auth failure");
        } catch (AuthenticationFailedException ignored) {}

        server.verifyInteractionCompleted();
    }

    @Test
    public void open_withAuthTypePlainAndNoPlainAuthCapability_performsLogin() throws Exception {
        settings.setAuthType(AuthType.PLAIN);

        MockPop3Server server = new MockPop3Server();
        server.output("+OK POP3 server greeting");
        server.expect("AUTH");
        server.output("+OK Listing of supported mechanisms follows");
        server.output("CRAM-MD5");
        server.output("EXTERNAL");
        server.output(".");
        server.expect("CAPA");
        server.output("+OK Listing of supported mechanisms follows");
        server.output("CRAM-MD5");
        server.output("EXTERNAL");
        server.output(".");
        server.expect("USER user");
        server.output("+OK");
        server.expect("PASS password");
        server.output("-ERR");

        try {
            startServerAndCreateOpenConnection(server);
            fail("Expected auth failure");
        } catch (AuthenticationFailedException ignored) {}

        server.verifyInteractionCompleted();
    }

    @Test
    public void open_withAuthTypePlainAndNoPlainAuthCapabilityAndLoginFailure_throwsException() throws Exception {
        settings.setAuthType(AuthType.PLAIN);

        MockPop3Server server = new MockPop3Server();
        server.output("+OK POP3 server greeting");
        server.expect("AUTH");
        server.output("+OK Listing of supported mechanisms follows");
        server.output("CRAM-MD5");
        server.output("EXTERNAL");
        server.output(".");
        server.expect("CAPA");
        server.output("+OK Listing of supported mechanisms follows");
        server.output("CRAM-MD5");
        server.output("EXTERNAL");
        server.output(".");
        server.expect("USER user");
        server.output("+OK");
        server.expect("PASS password");
        server.output("+OK");

        startServerAndCreateOpenConnection(server);

        server.verifyInteractionCompleted();
    }

    @Test
    public void open_withAuthTypeCramMd5AndCapability_performsCramMd5Auth() throws IOException, MessagingException {
        settings.setAuthType(AuthType.CRAM_MD5);

        MockPop3Server server = new MockPop3Server();
        server.output("+OK POP3 server greeting");
        server.expect("AUTH");
        server.output("+OK Listing of supported mechanisms follows");
        server.output("PLAIN");
        server.output("CRAM-MD5");
        server.output("EXTERNAL");
        server.output(".");
        server.expect("CAPA");
        server.output("+OK Listing of supported mechanisms follows");
        server.output("PLAIN");
        server.output("CRAM-MD5");
        server.output("EXTERNAL");
        server.output(".");
        server.expect("AUTH CRAM-MD5");
        server.output("+ abcd");
        server.expect("dXNlciBhZGFhZTU2Zjk1NzAxZjQwNDQwZjhhMWU2YzY1ZjZmZg==");
        server.output("+OK");
        startServerAndCreateOpenConnection(server);

        server.verifyConnectionStillOpen();
        server.verifyInteractionCompleted();
    }

    @Test
    public void open_withAuthTypeCramMd5AndCapabilityAndCramFailure_throwsException() throws IOException, MessagingException {
        settings.setAuthType(AuthType.CRAM_MD5);


        MockPop3Server server = new MockPop3Server();
        server.output("+OK POP3 server greeting");
        server.expect("AUTH");
        server.output("+OK Listing of supported mechanisms follows");
        server.output("PLAIN");
        server.output("CRAM-MD5");
        server.output("EXTERNAL");
        server.output(".");
        server.expect("CAPA");
        server.output("+OK Listing of supported mechanisms follows");
        server.output("PLAIN");
        server.output("CRAM-MD5");
        server.output("EXTERNAL");
        server.output(".");
        server.expect("AUTH CRAM-MD5");
        server.output("+ abcd");
        server.expect("dXNlciBhZGFhZTU2Zjk1NzAxZjQwNDQwZjhhMWU2YzY1ZjZmZg==");
        server.output("-ERR");

        try {
            startServerAndCreateOpenConnection(server);
            fail("Expected auth failure");
        } catch (AuthenticationFailedException ignored) {}

        server.verifyInteractionCompleted();
    }

    @Test
    public void open_withAuthTypeCramMd5AndNoCapability_performsApopAuth() throws IOException, MessagingException {
        settings.setAuthType(AuthType.CRAM_MD5);

        MockPop3Server server = new MockPop3Server();
        server.output("+OK abc<a>abcd");
        server.expect("AUTH");
        server.output("+OK Listing of supported mechanisms follows");
        server.output("PLAIN");
        server.output("EXTERNAL");
        server.output(".");
        server.expect("CAPA");
        server.output("+OK Listing of supported mechanisms follows");
        server.output("PLAIN");
        server.output("EXTERNAL");
        server.output(".");
        server.expect("APOP user c8e8c560e385faaa6367d4145572b8ea");
        server.output("+OK");
        startServerAndCreateOpenConnection(server);

        server.verifyConnectionStillOpen();
        server.verifyInteractionCompleted();
    }

    @Test
    public void open_withAuthTypeCramMd5AndNoCapabilityAndApopFailure_throwsException() throws IOException, MessagingException {
        settings.setAuthType(AuthType.CRAM_MD5);


        MockPop3Server server = new MockPop3Server();
        server.output("+OK abc<a>abcd");
        server.expect("AUTH");
        server.output("+OK Listing of supported mechanisms follows");
        server.output("PLAIN");
        server.output("EXTERNAL");
        server.output(".");
        server.expect("CAPA");
        server.output("+OK Listing of supported mechanisms follows");
        server.output("PLAIN");
        server.output("EXTERNAL");
        server.output(".");
        server.expect("APOP user c8e8c560e385faaa6367d4145572b8ea");
        server.output("-ERR");

        try {
            startServerAndCreateOpenConnection(server);
            fail("Expected auth failure");
        } catch (AuthenticationFailedException ignored) {}

        server.verifyInteractionCompleted();
    }

    @Test
    public void open_withAuthTypeExternalAndCapability_performsExternalAuth() throws IOException, MessagingException {
        settings.setAuthType(AuthType.EXTERNAL);

        MockPop3Server server = new MockPop3Server();
        server.output("+OK POP3 server greeting");
        server.expect("AUTH");
        server.output("+OK Listing of supported mechanisms follows");
        server.output("PLAIN");
        server.output("CRAM-MD5");
        server.output("EXTERNAL");
        server.output(".");
        server.expect("CAPA");
        server.output("+OK Listing of supported mechanisms follows");
        server.output("PLAIN");
        server.output("CRAM-MD5");
        server.output("EXTERNAL");
        server.output(".");
        server.expect("AUTH EXTERNAL dXNlcg==");
        server.output("+OK");
        startServerAndCreateOpenConnection(server);

        server.verifyConnectionStillOpen();
        server.verifyInteractionCompleted();
    }

    @Test
    public void open_withAuthTypeExternalAndNoCapability_throwsCVE() throws IOException, MessagingException {
        settings.setAuthType(AuthType.EXTERNAL);

        MockPop3Server server = new MockPop3Server();
        server.output("+OK POP3 server greeting");
        server.expect("AUTH");
        server.output("+OK Listing of supported mechanisms follows");
        server.output("PLAIN");
        server.output("CRAM-MD5");
        server.output(".");
        server.expect("CAPA");
        server.output("+OK Listing of supported mechanisms follows");
        server.output("PLAIN");
        server.output("CRAM-MD5");
        server.output("EXTERNAL");
        server.output(".");

        try {
            startServerAndCreateOpenConnection(server);
            fail("CVE expected");
        } catch (CertificateValidationException e) {
            assertEquals(Reason.MissingCapability, e.getReason());
        }

        server.verifyConnectionStillOpen();
        server.verifyInteractionCompleted();
    }

    @Test
    public void open_withAuthTypeExternalAndCapability_withRejection_throwsCVE() throws IOException, MessagingException {
        settings.setAuthType(AuthType.EXTERNAL);

        MockPop3Server server = new MockPop3Server();
        server.output("+OK POP3 server greeting");
        server.expect("AUTH");
        server.output("+OK Listing of supported mechanisms follows");
        server.output("PLAIN");
        server.output("CRAM-MD5");
        server.output("EXTERNAL");
        server.output(".");
        server.expect("CAPA");
        server.output("+OK Listing of supported mechanisms follows");
        server.output("PLAIN");
        server.output("CRAM-MD5");
        server.output("EXTERNAL");
        server.output(".");
        server.expect("AUTH EXTERNAL dXNlcg==");
        server.output("-ERR Invalid certificate");

        try {
            startServerAndCreateOpenConnection(server);
            fail("CVE expected");
        } catch (CertificateValidationException e) {
            assertEquals("POP3 client certificate authentication failed: -ERR Invalid certificate", e.getMessage());
        }

        server.verifyInteractionCompleted();
    }

    private void startServerAndCreateOpenConnection(MockPop3Server server) throws IOException,
            MessagingException {
        server.start();
        settings.setHost(server.getHost());
        settings.setPort(server.getPort());
        createAndOpenPop3Connection(settings, socketFactory);
    }

    private void createAndOpenPop3Connection(Pop3Settings settings, TrustedSocketFactory socketFactory)
            throws MessagingException {
        Pop3Connection connection = new Pop3Connection(settings, socketFactory);
        connection.open();
    }
}
