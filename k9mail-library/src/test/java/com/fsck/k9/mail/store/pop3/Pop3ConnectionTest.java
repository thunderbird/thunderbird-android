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
import com.fsck.k9.mail.helpers.TestTrustedSocketFactory;
import com.fsck.k9.mail.ssl.TrustedSocketFactory;
import javax.net.ssl.SSLException;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
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
    private SimplePop3Settings settings;
    private TrustedSocketFactory socketFactory;

    @Before
    public void before() throws Exception {
        settings = new SimplePop3Settings();
        settings.setUsername(username);
        settings.setPassword(password);
        socketFactory = TestTrustedSocketFactory.newInstance();
        mockTrustedSocketFactory = mock(TrustedSocketFactory.class); //TODO: Remove
        mockSocket = mock(Socket.class); //TODO: Remove
        outputStream = new ByteArrayOutputStream(); //TODO: Remove
        when(mockTrustedSocketFactory.createSocket(null, "server", 12345, null)).thenReturn(mockSocket);  //TODO: Remove
        when(mockSocket.getOutputStream()).thenReturn(outputStream); //TODO: Remove
        when(mockSocket.isConnected()).thenReturn(true); //TODO: Remove
    }

    private void setSettingsForMockSocket() {
        settings.setHost(host);
        settings.setPort(port);
        settings.setConnectionSecurity(ConnectionSecurity.SSL_TLS_REQUIRED);
    }

    @Test(expected = CertificateValidationException.class)
    public void whenTrustedSocketFactoryThrowsSSLCertificateException_throwCertificateValidationException() throws Exception {
        when(mockTrustedSocketFactory.createSocket(null, "server", 12345, null)).thenThrow(
                new SSLException(new CertificateException()));
        setSettingsForMockSocket();
        settings.setAuthType(AuthType.PLAIN);

        new Pop3Connection(settings, mockTrustedSocketFactory);
    }

    @Test(expected = MessagingException.class)
    public void whenTrustedSocketFactoryThrowsCertificateException_throwMessagingException() throws Exception {
        when(mockTrustedSocketFactory.createSocket(null, "server", 12345, null)).thenThrow(
                new SSLException(""));

        setSettingsForMockSocket();
        settings.setAuthType(AuthType.PLAIN);

        new Pop3Connection(settings, mockTrustedSocketFactory);
    }

    @Test(expected = MessagingException.class)
    public void whenTrustedSocketFactoryThrowsGeneralSecurityException_throwMessagingException() throws Exception {
        when(mockTrustedSocketFactory.createSocket(null, "server", 12345, null)).thenThrow(
                new NoSuchAlgorithmException(""));

        setSettingsForMockSocket();
        settings.setAuthType(AuthType.PLAIN);

        new Pop3Connection(settings, mockTrustedSocketFactory);
    }

    @Test(expected = MessagingException.class)
    public void whenTrustedSocketFactoryThrowsIOException_throwMessagingException() throws Exception {
        when(mockTrustedSocketFactory.createSocket(null, "server", 12345, null)).thenThrow(
                new IOException(""));

        setSettingsForMockSocket();
        settings.setAuthType(AuthType.PLAIN);

        new Pop3Connection(settings, mockTrustedSocketFactory);
    }

    @Test(expected = MessagingException.class)
    public void whenSocketNotConnected_throwsMessagingException() throws Exception {
        when(mockSocket.isConnected()).thenReturn(false);

        setSettingsForMockSocket();
        settings.setAuthType(AuthType.PLAIN);

        new Pop3Connection(settings, mockTrustedSocketFactory);
    }

    @Test
    public void withTLS_connectsToSocket() throws Exception {
        String response = INITIAL_RESPONSE +
                AUTH_HANDLE_RESPONSE +
                CAPA_RESPONSE +
                AUTH_PLAIN_AUTHENTICATED_RESPONSE;

        when(mockSocket.getInputStream()).thenReturn(new ByteArrayInputStream(response.getBytes()));
        setSettingsForMockSocket();
        settings.setAuthType(AuthType.PLAIN);

        new Pop3Connection(settings, mockTrustedSocketFactory);

        assertEquals(AUTH +
                CAPA +
                AUTH_PLAIN_WITH_LOGIN, new String(outputStream.toByteArray()));
    }

    @Test
    public void withAuthTypePlainAndPlainAuthCapability_performsPlainAuth() throws Exception {
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
        startServerAndCreateConnection(server);

        server.verifyConnectionStillOpen();
        server.verifyInteractionCompleted();
    }

    @Test
    public void withAuthTypePlainAndPlainAuthCapabilityAndInvalidPasswordResponse_throwsException() throws Exception {
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
            startServerAndCreateConnection(server);
            fail("Expected auth failure");
        } catch (AuthenticationFailedException ignored) {}

        server.verifyInteractionCompleted();
    }

    @Test
    public void withAuthTypePlainAndNoPlainAuthCapability_performsLogin() throws Exception {
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
            startServerAndCreateConnection(server);
            fail("Expected auth failure");
        } catch (AuthenticationFailedException ignored) {}

        server.verifyInteractionCompleted();
    }

    @Test
    public void withAuthTypePlainAndNoPlainAuthCapabilityAndLoginFailure_throwsException() throws Exception {
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

        startServerAndCreateConnection(server);

        server.verifyInteractionCompleted();
    }

    @Test
    public void withAuthTypeCramMd5AndCapability_performsCramMd5Auth() throws IOException, MessagingException {
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
        startServerAndCreateConnection(server);

        server.verifyConnectionStillOpen();
        server.verifyInteractionCompleted();
    }

    @Test
    public void withAuthTypeCramMd5AndCapabilityAndCramFailure_throwsException() throws IOException, MessagingException {
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
            startServerAndCreateConnection(server);
            fail("Expected auth failure");
        } catch (AuthenticationFailedException ignored) {}

        server.verifyInteractionCompleted();
    }

    @Test
    public void withAuthTypeCramMd5AndNoCapability_performsApopAuth() throws IOException, MessagingException {
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
        startServerAndCreateConnection(server);

        server.verifyConnectionStillOpen();
        server.verifyInteractionCompleted();
    }

    @Test
    public void withAuthTypeCramMd5AndNoCapabilityAndApopFailure_throwsException() throws IOException, MessagingException {
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
            startServerAndCreateConnection(server);
            fail("Expected auth failure");
        } catch (AuthenticationFailedException ignored) {}

        server.verifyInteractionCompleted();
    }

    @Test
    public void withAuthTypeExternalAndCapability_performsExternalAuth() throws IOException, MessagingException {
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
        startServerAndCreateConnection(server);

        server.verifyConnectionStillOpen();
        server.verifyInteractionCompleted();
    }

    @Test
    public void withAuthTypeExternalAndCapability_withRejection_throwsCVE() throws IOException, MessagingException {
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
            startServerAndCreateConnection(server);
            fail("CVE expected");
        } catch (CertificateValidationException e) {
            assertEquals("POP3 client certificate authentication failed: -ERR Invalid certificate", e.getMessage());
        }

        server.verifyInteractionCompleted();
    }

    private Pop3Connection startServerAndCreateConnection(MockPop3Server server) throws IOException,
            MessagingException {
        server.start();
        settings.setHost(server.getHost());
        settings.setPort(server.getPort());
        return createPop3Connection(settings, socketFactory);
    }

    private Pop3Connection createPop3Connection(Pop3Settings settings, TrustedSocketFactory socketFactory)
            throws MessagingException {
        return new Pop3Connection(settings, socketFactory);
    }
}
