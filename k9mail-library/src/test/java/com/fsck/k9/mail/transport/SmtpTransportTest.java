package com.fsck.k9.mail.transport;


import java.io.IOException;
import java.net.InetAddress;

import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.AuthenticationFailedException;
import com.fsck.k9.mail.CertificateValidationException;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.ServerSettings;
import com.fsck.k9.mail.ServerSettings.Type;
import com.fsck.k9.mail.XOAuth2ChallengeParserTest;
import com.fsck.k9.mail.filter.Base64;
import com.fsck.k9.mail.helpers.TestMessageBuilder;
import com.fsck.k9.mail.helpers.TestTrustedSocketFactory;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.mail.oauth.OAuth2TokenProvider;
import com.fsck.k9.mail.oauth.XOAuth2ChallengeParser;
import com.fsck.k9.mail.ssl.TrustedSocketFactory;
import com.fsck.k9.mail.store.StoreConfig;
import com.fsck.k9.mail.transport.mockServer.MockSmtpServer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 21)
public class SmtpTransportTest {
    private static final String LOCALHOST_NAME = "localhost";
    private static final String USERNAME = "user";
    private static final String PASSWORD = "password";
    private static final String CLIENT_CERTIFICATE_ALIAS = null;

    
    private TrustedSocketFactory socketFactory;
    private OAuth2TokenProvider oAuth2TokenProvider;

    
    @Before
    public void before() throws AuthenticationFailedException {
        socketFactory = new TestTrustedSocketFactory();
        oAuth2TokenProvider = mock(OAuth2TokenProvider.class);
        when(oAuth2TokenProvider.getToken(eq(USERNAME), anyInt()))
                .thenReturn("oldToken").thenReturn("newToken");
    }

    @Test
    public void SmtpTransport_withValidTransportUri() throws Exception {
        StoreConfig storeConfig = createStoreConfigWithTransportUri("smtp://user:password:CRAM_MD5@server:123456");

        new SmtpTransport(storeConfig, socketFactory, oAuth2TokenProvider);
    }

    @Test(expected = MessagingException.class)
    public void SmtpTransport_withInvalidTransportUri_shouldThrow() throws Exception {
        StoreConfig storeConfig = createStoreConfigWithTransportUri("smpt://");

        new SmtpTransport(storeConfig, socketFactory, oAuth2TokenProvider);
    }

    @Test
    public void open_withoutAuthLoginExtension_shouldConnectWithoutAuthentication() throws Exception {
        MockSmtpServer server = new MockSmtpServer();
        server.output("220 localhost Simple Mail Transfer Service Ready");
        server.expect("EHLO localhost");
        server.output("250-localhost Hello client.localhost");
        server.output("250 OK");
        SmtpTransport transport = startServerAndCreateSmtpTransportWithoutPassword(server);

        transport.open();

        server.verifyConnectionStillOpen();
        server.verifyInteractionCompleted();
    }

    @Test
    public void open_withAuthPlainExtension() throws Exception {
        MockSmtpServer server = new MockSmtpServer();
        server.output("220 localhost Simple Mail Transfer Service Ready");
        server.expect("EHLO localhost");
        server.output("250-localhost Hello client.localhost");
        server.output("250 AUTH PLAIN LOGIN");
        server.expect("AUTH PLAIN AHVzZXIAcGFzc3dvcmQ=");
        server.output("235 2.7.0 Authentication successful");
        SmtpTransport transport = startServerAndCreateSmtpTransport(server, AuthType.PLAIN, ConnectionSecurity.NONE);

        transport.open();

        server.verifyConnectionStillOpen();
        server.verifyInteractionCompleted();
    }

    @Test
    public void open_withAuthLoginExtension() throws Exception {
        MockSmtpServer server = new MockSmtpServer();
        server.output("220 localhost Simple Mail Transfer Service Ready");
        server.expect("EHLO localhost");
        server.output("250-localhost Hello client.localhost");
        server.output("250 AUTH LOGIN");
        server.expect("AUTH LOGIN");
        server.output("250 OK");
        server.expect("dXNlcg==");
        server.output("250 OK");
        server.expect("cGFzc3dvcmQ=");
        server.output("235 2.7.0 Authentication successful");
        SmtpTransport transport = startServerAndCreateSmtpTransport(server, AuthType.PLAIN, ConnectionSecurity.NONE);

        transport.open();

        server.verifyConnectionStillOpen();
        server.verifyInteractionCompleted();
    }

    @Test
    public void open_withoutLoginAndPlainAuthExtensions_shouldThrow() throws Exception {
        MockSmtpServer server = new MockSmtpServer();
        server.output("220 localhost Simple Mail Transfer Service Ready");
        server.expect("EHLO localhost");
        server.output("250-localhost Hello client.localhost");
        server.output("250 AUTH");
        SmtpTransport transport = startServerAndCreateSmtpTransport(server, AuthType.PLAIN, ConnectionSecurity.NONE);

        try {
            transport.open();
            fail("Exception expected");
        } catch (MessagingException e) {
            assertEquals("Authentication methods SASL PLAIN and LOGIN are unavailable.", e.getMessage());
        }

        server.verifyConnectionStillOpen();
        server.verifyInteractionCompleted();
    }

    @Test
    public void open_withCramMd5AuthExtension() throws Exception {
        MockSmtpServer server = new MockSmtpServer();
        server.output("220 localhost Simple Mail Transfer Service Ready");
        server.expect("EHLO localhost");
        server.output("250-localhost Hello client.localhost");
        server.output("250 AUTH CRAM-MD5");
        server.expect("AUTH CRAM-MD5");
        server.output(Base64.encode("<24609.1047914046@localhost>"));
        server.expect("dXNlciA3NmYxNWEzZmYwYTNiOGI1NzcxZmNhODZlNTcyMDk2Zg==");
        server.output("235 2.7.0 Authentication successful");
        SmtpTransport transport = startServerAndCreateSmtpTransport(server, AuthType.CRAM_MD5, ConnectionSecurity.NONE);

        transport.open();

        server.verifyConnectionStillOpen();
        server.verifyInteractionCompleted();
    }

    @Test
    public void open_withoutCramMd5AuthExtension_shouldThrow() throws Exception {
        MockSmtpServer server = new MockSmtpServer();
        server.output("220 localhost Simple Mail Transfer Service Ready");
        server.expect("EHLO localhost");
        server.output("250-localhost Hello client.localhost");
        server.output("250 AUTH PLAIN LOGIN");
        SmtpTransport transport = startServerAndCreateSmtpTransport(server, AuthType.CRAM_MD5, ConnectionSecurity.NONE);

        try {
            transport.open();
            fail("Exception expected");
        } catch (MessagingException e) {
            assertEquals("Authentication method CRAM-MD5 is unavailable.", e.getMessage());
        }

        server.verifyConnectionStillOpen();
        server.verifyInteractionCompleted();
    }

    @Test
    public void open_withXoauth2Extension() throws Exception {
        MockSmtpServer server = new MockSmtpServer();
        server.output("220 localhost Simple Mail Transfer Service Ready");
        server.expect("EHLO localhost");
        server.output("250-localhost Hello client.localhost");
        server.output("250 AUTH XOAUTH2");
        server.expect("AUTH XOAUTH2 dXNlcj11c2VyAWF1dGg9QmVhcmVyIG9sZFRva2VuAQE=");
        server.output("235 2.7.0 Authentication successful");
        SmtpTransport transport = startServerAndCreateSmtpTransport(server, AuthType.XOAUTH2, ConnectionSecurity.NONE);

        transport.open();

        server.verifyConnectionStillOpen();
        server.verifyInteractionCompleted();
    }

    @Test
    public void open_withXoauth2Extension_shouldThrowOn401Response() throws Exception {
        MockSmtpServer server = new MockSmtpServer();
        server.output("220 localhost Simple Mail Transfer Service Ready");
        server.expect("EHLO localhost");
        server.output("250-localhost Hello client.localhost");
        server.output("250 AUTH XOAUTH2");
        server.expect("AUTH XOAUTH2 dXNlcj11c2VyAWF1dGg9QmVhcmVyIG9sZFRva2VuAQE=");
        server.output("334 "+ XOAuth2ChallengeParserTest.STATUS_401_RESPONSE);
        server.expect("");
        server.output("535-5.7.1 Username and Password not accepted. Learn more at");
        server.output("535 5.7.1 http://support.google.com/mail/bin/answer.py?answer=14257 hx9sm5317360pbc.68");
        SmtpTransport transport = startServerAndCreateSmtpTransport(server, AuthType.XOAUTH2, ConnectionSecurity.NONE);

        try {
            transport.open();
            fail("Exception expected");
        } catch (AuthenticationFailedException e) {
            assertEquals(
                    "Negative SMTP reply: 535 5.7.1 http://support.google.com/mail/bin/answer.py?answer=14257 hx9sm5317360pbc.68",
                    e.getMessage());
        }

        InOrder inOrder = inOrder(oAuth2TokenProvider);
        inOrder.verify(oAuth2TokenProvider).getToken(eq(USERNAME), anyInt());
        inOrder.verify(oAuth2TokenProvider).invalidateToken(USERNAME);
        server.verifyConnectionStillOpen();
        server.verifyInteractionCompleted();
    }

    @Test
    public void open_withXoauth2Extension_shouldInvalidateAndRetryOn400Response() throws Exception {
        MockSmtpServer server = new MockSmtpServer();
        server.output("220 localhost Simple Mail Transfer Service Ready");
        server.expect("EHLO localhost");
        server.output("250-localhost Hello client.localhost");
        server.output("250 AUTH XOAUTH2");
        server.expect("AUTH XOAUTH2 dXNlcj11c2VyAWF1dGg9QmVhcmVyIG9sZFRva2VuAQE=");
        server.output("334 "+ XOAuth2ChallengeParserTest.STATUS_400_RESPONSE);
        server.expect("");
        server.output("535-5.7.1 Username and Password not accepted. Learn more at");
        server.output("535 5.7.1 http://support.google.com/mail/bin/answer.py?answer=14257 hx9sm5317360pbc.68");
        server.expect("AUTH XOAUTH2 dXNlcj11c2VyAWF1dGg9QmVhcmVyIG5ld1Rva2VuAQE=");
        server.output("235 2.7.0 Authentication successful");
        SmtpTransport transport = startServerAndCreateSmtpTransport(server, AuthType.XOAUTH2, ConnectionSecurity.NONE);

        transport.open();

        InOrder inOrder = inOrder(oAuth2TokenProvider);
        inOrder.verify(oAuth2TokenProvider).getToken(eq(USERNAME), anyInt());
        inOrder.verify(oAuth2TokenProvider).invalidateToken(USERNAME);
        inOrder.verify(oAuth2TokenProvider).getToken(eq(USERNAME), anyInt());
        server.verifyConnectionStillOpen();
        server.verifyInteractionCompleted();
    }

    @Test
    public void open_withXoauth2Extension_shouldInvalidateAndRetryOnInvalidJsonResponse() throws Exception {
        MockSmtpServer server = new MockSmtpServer();
        server.output("220 localhost Simple Mail Transfer Service Ready");
        server.expect("EHLO localhost");
        server.output("250-localhost Hello client.localhost");
        server.output("250 AUTH XOAUTH2");
        server.expect("AUTH XOAUTH2 dXNlcj11c2VyAWF1dGg9QmVhcmVyIG9sZFRva2VuAQE=");
        server.output("334 "+ XOAuth2ChallengeParserTest.INVALID_RESPONSE);
        server.expect("");
        server.output("535-5.7.1 Username and Password not accepted. Learn more at");
        server.output("535 5.7.1 http://support.google.com/mail/bin/answer.py?answer=14257 hx9sm5317360pbc.68");
        server.expect("AUTH XOAUTH2 dXNlcj11c2VyAWF1dGg9QmVhcmVyIG5ld1Rva2VuAQE=");
        server.output("235 2.7.0 Authentication successful");
        SmtpTransport transport = startServerAndCreateSmtpTransport(server, AuthType.XOAUTH2, ConnectionSecurity.NONE);

        transport.open();

        InOrder inOrder = inOrder(oAuth2TokenProvider);
        inOrder.verify(oAuth2TokenProvider).getToken(eq(USERNAME), anyInt());
        inOrder.verify(oAuth2TokenProvider).invalidateToken(USERNAME);
        inOrder.verify(oAuth2TokenProvider).getToken(eq(USERNAME), anyInt());
        server.verifyConnectionStillOpen();
        server.verifyInteractionCompleted();
    }

    @Test
    public void open_withXoauth2Extension_shouldInvalidateAndRetryOnMissingStatusJsonResponse() throws Exception {
        MockSmtpServer server = new MockSmtpServer();
        server.output("220 localhost Simple Mail Transfer Service Ready");
        server.expect("EHLO localhost");
        server.output("250-localhost Hello client.localhost");
        server.output("250 AUTH XOAUTH2");
        server.expect("AUTH XOAUTH2 dXNlcj11c2VyAWF1dGg9QmVhcmVyIG9sZFRva2VuAQE=");
        server.output("334 "+ XOAuth2ChallengeParserTest.MISSING_STATUS_RESPONSE);
        server.expect("");
        server.output("535-5.7.1 Username and Password not accepted. Learn more at");
        server.output("535 5.7.1 http://support.google.com/mail/bin/answer.py?answer=14257 hx9sm5317360pbc.68");
        server.expect("AUTH XOAUTH2 dXNlcj11c2VyAWF1dGg9QmVhcmVyIG5ld1Rva2VuAQE=");
        server.output("235 2.7.0 Authentication successful");
        SmtpTransport transport = startServerAndCreateSmtpTransport(server, AuthType.XOAUTH2, ConnectionSecurity.NONE);

        transport.open();

        InOrder inOrder = inOrder(oAuth2TokenProvider);
        inOrder.verify(oAuth2TokenProvider).getToken(eq(USERNAME), anyInt());
        inOrder.verify(oAuth2TokenProvider).invalidateToken(USERNAME);
        inOrder.verify(oAuth2TokenProvider).getToken(eq(USERNAME), anyInt());
        server.verifyConnectionStillOpen();
        server.verifyInteractionCompleted();
    }

    @Test
    public void open_withXoauth2Extension_shouldThrowOnMultipleFailure() throws Exception {
        MockSmtpServer server = new MockSmtpServer();
        server.output("220 localhost Simple Mail Transfer Service Ready");
        server.expect("EHLO localhost");
        server.output("250-localhost Hello client.localhost");
        server.output("250 AUTH XOAUTH2");
        server.expect("AUTH XOAUTH2 dXNlcj11c2VyAWF1dGg9QmVhcmVyIG9sZFRva2VuAQE=");
        server.output("334 " + XOAuth2ChallengeParserTest.STATUS_400_RESPONSE);
        server.expect("");
        server.output("535-5.7.1 Username and Password not accepted. Learn more at");
        server.output("535 5.7.1 http://support.google.com/mail/bin/answer.py?answer=14257 hx9sm5317360pbc.68");
        server.expect("AUTH XOAUTH2 dXNlcj11c2VyAWF1dGg9QmVhcmVyIG5ld1Rva2VuAQE=");
        server.output("334 " + XOAuth2ChallengeParserTest.STATUS_400_RESPONSE);
        server.expect("");
        server.output("535-5.7.1 Username and Password not accepted. Learn more at");
        server.output("535 5.7.1 http://support.google.com/mail/bin/answer.py?answer=14257 hx9sm5317360pbc.68");
        SmtpTransport transport = startServerAndCreateSmtpTransport(server, AuthType.XOAUTH2, ConnectionSecurity.NONE);

        try {
            transport.open();
            fail("Exception expected");
        } catch (AuthenticationFailedException e) {
            assertEquals(
                "Negative SMTP reply: 535 5.7.1 http://support.google.com/mail/bin/answer.py?answer=14257 hx9sm5317360pbc.68",
                e.getMessage());
        }

        server.verifyConnectionStillOpen();
        server.verifyInteractionCompleted();
    }

    @Test
    public void open_withXoauth2Extension_shouldThrowOnFailure_fetchingToken() throws Exception {
        MockSmtpServer server = new MockSmtpServer();
        server.output("220 localhost Simple Mail Transfer Service Ready");
        server.expect("EHLO localhost");
        server.output("250-localhost Hello client.localhost");
        server.output("250 AUTH XOAUTH2");
        when(oAuth2TokenProvider.getToken(anyString(), anyInt())).thenThrow(new AuthenticationFailedException("Failed to fetch token"));
        SmtpTransport transport = startServerAndCreateSmtpTransport(server, AuthType.XOAUTH2, ConnectionSecurity.NONE);

        try {
            transport.open();
            fail("Exception expected");
        } catch (AuthenticationFailedException e) {
            assertEquals("Failed to fetch token", e.getMessage());
        }

        server.verifyConnectionStillOpen();
        server.verifyInteractionCompleted();
    }


    @Test
    public void open_withoutXoauth2Extension_shouldThrow() throws Exception {
        MockSmtpServer server = new MockSmtpServer();
        server.output("220 localhost Simple Mail Transfer Service Ready");
        server.expect("EHLO localhost");
        server.output("250-localhost Hello client.localhost");
        server.output("250 AUTH PLAIN LOGIN");
        SmtpTransport transport = startServerAndCreateSmtpTransport(server, AuthType.XOAUTH2, ConnectionSecurity.NONE);

        try {
            transport.open();
            fail("Exception expected");
        } catch (MessagingException e) {
            assertEquals("Authentication method XOAUTH2 is unavailable.", e.getMessage());
        }

        server.verifyConnectionStillOpen();
        server.verifyInteractionCompleted();
    }

    @Test
    public void open_withAuthExternalExtension() throws Exception {
        MockSmtpServer server = new MockSmtpServer();
        server.output("220 localhost Simple Mail Transfer Service Ready");
        server.expect("EHLO localhost");
        server.output("250-localhost Hello client.localhost");
        server.output("250 AUTH EXTERNAL");
        server.expect("AUTH EXTERNAL dXNlcg==");
        server.output("235 2.7.0 Authentication successful");
        SmtpTransport transport = startServerAndCreateSmtpTransport(server, AuthType.EXTERNAL, ConnectionSecurity.NONE);

        transport.open();

        server.verifyConnectionStillOpen();
        server.verifyInteractionCompleted();
    }

    @Test
    public void open_withoutAuthExternalExtension_shouldThrow() throws Exception {
        MockSmtpServer server = new MockSmtpServer();
        server.output("220 localhost Simple Mail Transfer Service Ready");
        server.expect("EHLO localhost");
        server.output("250-localhost Hello client.localhost");
        server.output("250 AUTH");
        SmtpTransport transport = startServerAndCreateSmtpTransport(server, AuthType.EXTERNAL, ConnectionSecurity.NONE);

        try {
            transport.open();
            fail("Exception expected");
        } catch (CertificateValidationException e) {
            assertEquals(CertificateValidationException.Reason.MissingCapability, e.getReason());
        }

        server.verifyConnectionStillOpen();
        server.verifyInteractionCompleted();
    }

    @Test
    public void open_withAutomaticAuthAndNoTransportSecurityAndAuthCramMd5Extension_shouldUseAuthCramMd5()
            throws Exception {
        MockSmtpServer server = new MockSmtpServer();
        server.output("220 localhost Simple Mail Transfer Service Ready");
        server.expect("EHLO localhost");
        server.output("250-localhost Hello client.localhost");
        server.output("250 AUTH CRAM-MD5");
        server.expect("AUTH CRAM-MD5");
        server.output(Base64.encode("<24609.1047914046@localhost>"));
        server.expect("dXNlciA3NmYxNWEzZmYwYTNiOGI1NzcxZmNhODZlNTcyMDk2Zg==");
        server.output("235 2.7.0 Authentication successful");
        SmtpTransport transport = startServerAndCreateSmtpTransport(server, AuthType.AUTOMATIC,
                ConnectionSecurity.NONE);

        transport.open();

        server.verifyConnectionStillOpen();
        server.verifyInteractionCompleted();
    }

    @Test
    public void open_withAutomaticAuthAndNoTransportSecurityAndAuthPlainExtension_shouldThrow() throws Exception {
        MockSmtpServer server = new MockSmtpServer();
        server.output("220 localhost Simple Mail Transfer Service Ready");
        server.expect("EHLO localhost");
        server.output("250-localhost Hello client.localhost");
        server.output("250 AUTH PLAIN LOGIN");
        SmtpTransport transport = startServerAndCreateSmtpTransport(server, AuthType.AUTOMATIC,
                ConnectionSecurity.NONE);

        try {
            transport.open();
            fail("Exception expected");
        } catch (MessagingException e) {
            assertEquals("Update your outgoing server authentication setting. AUTOMATIC auth. is unavailable.",
                    e.getMessage());
        }

        server.verifyConnectionStillOpen();
        server.verifyInteractionCompleted();
    }

    @Test
    public void open_withEhloFailing_shouldTryHelo() throws Exception {
        MockSmtpServer server = new MockSmtpServer();
        server.output("220 localhost Simple Mail Transfer Service Ready");
        server.expect("EHLO localhost");
        server.output("502 5.5.1, Unrecognized command.");
        server.expect("HELO localhost");
        server.output("250 localhost");
        SmtpTransport transport = startServerAndCreateSmtpTransportWithoutPassword(server);

        transport.open();

        server.verifyConnectionStillOpen();
        server.verifyInteractionCompleted();
    }

    @Test
    public void sendMessage_withoutAddressToSendTo_shouldNotOpenConnection() throws Exception {
        MimeMessage message = new MimeMessage();
        MockSmtpServer server = createServerAndSetupForPlainAuthentication();
        SmtpTransport transport = startServerAndCreateSmtpTransport(server);

        transport.sendMessage(message);

        server.verifyConnectionNeverCreated();
    }

    @Test
    public void sendMessage_withSingleRecipient() throws Exception {
        Message message = getDefaultMessage();
        MockSmtpServer server = createServerAndSetupForPlainAuthentication();
        server.expect("MAIL FROM:<user@localhost>");
        server.output("250 OK");
        server.expect("RCPT TO:<user2@localhost>");
        server.output("250 OK");
        server.expect("DATA");
        server.output("354 End data with <CR><LF>.<CR><LF>");
        server.expect("[message data]");
        server.expect(".");
        server.output("250 OK: queued as 12345");
        server.expect("QUIT");
        server.output("221 BYE");
        server.closeConnection();
        SmtpTransport transport = startServerAndCreateSmtpTransport(server);

        transport.sendMessage(message);

        server.verifyConnectionClosed();
        server.verifyInteractionCompleted();
    }

    @Test
    public void sendMessage_with8BitEncoding() throws Exception {
        Message message = getDefaultMessage();
        MockSmtpServer server = createServerAndSetupForPlainAuthentication("8BITMIME");
        server.expect("MAIL FROM:<user@localhost> BODY=8BITMIME");
        server.output("250 OK");
        server.expect("RCPT TO:<user2@localhost>");
        server.output("250 OK");
        server.expect("DATA");
        server.output("354 End data with <CR><LF>.<CR><LF>");
        server.expect("[message data]");
        server.expect(".");
        server.output("250 OK: queued as 12345");
        server.expect("QUIT");
        server.output("221 BYE");
        server.closeConnection();
        SmtpTransport transport = startServerAndCreateSmtpTransport(server);

        transport.sendMessage(message);

        server.verifyConnectionClosed();
        server.verifyInteractionCompleted();
    }

    @Test
    public void sendMessage_withMessageTooLarge_shouldThrow() throws Exception {
        Message message = getDefaultMessageBuilder()
                .setHasAttachments(true)
                .messageSize(1234L)
                .build();
        MockSmtpServer server = createServerAndSetupForPlainAuthentication("SIZE 1000");
        SmtpTransport transport = startServerAndCreateSmtpTransport(server);

        try {
            transport.sendMessage(message);
            fail("Expected message too large error");
        } catch (MessagingException e) {
            assertTrue(e.isPermanentFailure());
            assertEquals("Message too large for server", e.getMessage());
        }
        
        //FIXME: Make sure connection was closed 
        //server.verifyConnectionClosed();
    }

    @Test
    public void sendMessage_withNegativeReply_shouldThrow() throws Exception {
        Message message = getDefaultMessage();
        MockSmtpServer server = createServerAndSetupForPlainAuthentication();
        server.expect("MAIL FROM:<user@localhost>");
        server.output("250 OK");
        server.expect("RCPT TO:<user2@localhost>");
        server.output("250 OK");
        server.expect("DATA");
        server.output("354 End data with <CR><LF>.<CR><LF>");
        server.expect("[message data]");
        server.expect(".");
        server.output("421 4.7.0 Temporary system problem");
        server.expect("QUIT");
        server.output("221 BYE");
        server.closeConnection();
        SmtpTransport transport = startServerAndCreateSmtpTransport(server);

        try {
            transport.sendMessage(message);
            fail("Expected exception");
        } catch (SmtpTransport.NegativeSmtpReplyException e) {
            assertEquals(421, e.getReplyCode());
            assertEquals("4.7.0 Temporary system problem", e.getReplyText());
        }
        
        server.verifyConnectionClosed();
        server.verifyInteractionCompleted();
    }

    private SmtpTransport startServerAndCreateSmtpTransport(MockSmtpServer server) throws IOException,
            MessagingException {
        return startServerAndCreateSmtpTransport(server, AuthType.PLAIN, ConnectionSecurity.NONE);
    }

    private SmtpTransport startServerAndCreateSmtpTransportWithoutPassword(MockSmtpServer server) throws IOException,
            MessagingException {
        return startServerAndCreateSmtpTransport(server, AuthType.PLAIN, ConnectionSecurity.NONE, null);
    }

    private SmtpTransport startServerAndCreateSmtpTransport(MockSmtpServer server, AuthType authenticationType,
            ConnectionSecurity connectionSecurity) throws IOException, MessagingException {
        return startServerAndCreateSmtpTransport(server, authenticationType, connectionSecurity, PASSWORD);
    }

    private SmtpTransport startServerAndCreateSmtpTransport(MockSmtpServer server, AuthType authenticationType,
            ConnectionSecurity connectionSecurity, String password) throws IOException, MessagingException {
        server.start();

        String host = server.getHost();
        int port = server.getPort();
        ServerSettings serverSettings = new ServerSettings(
                Type.SMTP,
                host,
                port,
                connectionSecurity,
                authenticationType,
                USERNAME,
                password,
                CLIENT_CERTIFICATE_ALIAS);
        String uri = SmtpTransport.createUri(serverSettings);
        StoreConfig storeConfig = createStoreConfigWithTransportUri(uri);

        return new TestSmtpTransport(storeConfig, socketFactory, oAuth2TokenProvider);
    }

    private StoreConfig createStoreConfigWithTransportUri(String value) {
        StoreConfig storeConfig = mock(StoreConfig.class);
        when(storeConfig.getTransportUri()).thenReturn(value);
        return storeConfig;
    }

    private TestMessageBuilder getDefaultMessageBuilder() {
        return new TestMessageBuilder()
                .from("user@localhost")
                .to("user2@localhost");
    }

    private Message getDefaultMessage() {
        return getDefaultMessageBuilder().build();
    }

    private MockSmtpServer createServerAndSetupForPlainAuthentication(String... extensions) {
        MockSmtpServer server = new MockSmtpServer();
        
        server.output("220 localhost Simple Mail Transfer Service Ready");
        server.expect("EHLO localhost");
        server.output("250-localhost Hello client.localhost");
        
        for (String extension : extensions) {
            server.output("250-" + extension);
        }
        
        server.output("250 AUTH LOGIN PLAIN CRAM-MD5");
        server.expect("AUTH PLAIN AHVzZXIAcGFzc3dvcmQ=");
        server.output("235 2.7.0 Authentication successful");
        
        return server;
    }
    
    
    static class TestSmtpTransport extends SmtpTransport {
        TestSmtpTransport(StoreConfig storeConfig, TrustedSocketFactory trustedSocketFactory, OAuth2TokenProvider oAuth2TokenProvider)
                throws MessagingException {
            super(storeConfig, trustedSocketFactory, oAuth2TokenProvider);
        }

        @Override
        protected String getCanonicalHostName(InetAddress localAddress) {
            return LOCALHOST_NAME;
        }
    }
}
