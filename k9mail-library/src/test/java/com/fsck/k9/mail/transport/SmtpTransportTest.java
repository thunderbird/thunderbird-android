package com.fsck.k9.mail.transport;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.CertificateValidationException;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.ServerSettings;
import com.fsck.k9.mail.ServerSettings.Type;
import com.fsck.k9.mail.filter.Base64;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.mail.ssl.TrustedSocketFactory;
import com.fsck.k9.mail.store.StoreConfig;
import com.fsck.k9.mail.transport.mockServer.MockSmtpServer;
import com.fsck.k9.mail.transport.mockServer.TestMessage;
import com.fsck.k9.mailstore.BinaryMemoryBody;
import com.fsck.k9.testHelpers.TestTrustedSocketFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 21)
public class SmtpTransportTest {
    private String host;
    private int port;
    private ConnectionSecurity connectionSecurity;
    private AuthType authenticationType;
    private String username;
    private String password;
    private String clientCertificateAlias;
    private List<String> extensions;
    private StoreConfig storeConfig = mock(StoreConfig.class);
    private TrustedSocketFactory socketFactory;

    
    @Before
    public void before() {
        socketFactory = new TestTrustedSocketFactory();
        resetConnectionParameters();
    }

    @Test
    public void SmtpTransport_withValidUri_canBeCreated() throws Exception {
        StoreConfig storeConfig = mock(StoreConfig.class);
        when(storeConfig.getTransportUri()).thenReturn("smtp://user:password:CRAM_MD5@server:123456");
        TrustedSocketFactory trustedSocketFactory = mock(TrustedSocketFactory.class);

        new SmtpTransport(storeConfig, trustedSocketFactory);
    }

    @Test(expected = MessagingException.class)
    public void SmtpTransport_withInvalidUri_throwsMessagingException() throws Exception {
        StoreConfig storeConfig = mock(StoreConfig.class);
        when(storeConfig.getTransportUri()).thenReturn("smpt://");
        TrustedSocketFactory trustedSocketFactory = mock(TrustedSocketFactory.class);

        new SmtpTransport(storeConfig, trustedSocketFactory);
    }

    @Test
    public void open_withNoSecurityOrPasswordPlainAuth_connectsToServer_withoutLogin() throws Exception {
        username = "user";
        authenticationType = AuthType.PLAIN;
        connectionSecurity = ConnectionSecurity.NONE;

        MockSmtpServer server = new MockSmtpServer();
        server.output("220 localhost Simple Mail Transfer Service Ready");
        server.expect("EHLO localhost");
        server.output("250-localhost Hello client.localhost");
        server.output("250 OK");

        SmtpTransport transport = startServerAndCreateSmtpTransport(server);
        transport.open();

        server.verifyConnectionStillOpen();
        server.verifyInteractionCompleted();
    }

    @Test
    public void open_withNoSecurityPlainAuth_connectsToServer() throws Exception {
        username = "user";
        password = "password";
        authenticationType = AuthType.PLAIN;
        connectionSecurity = ConnectionSecurity.NONE;

        MockSmtpServer server = new MockSmtpServer();
        server.output("220 localhost Simple Mail Transfer Service Ready");
        server.expect("EHLO localhost");
        server.output("250-localhost Hello client.localhost");
        server.output("250 AUTH PLAIN LOGIN");
        server.expect("AUTH PLAIN AHVzZXIAcGFzc3dvcmQ=");
        server.output("235 2.7.0 Authentication successful");

        SmtpTransport transport = startServerAndCreateSmtpTransport(server);
        transport.open();

        server.verifyConnectionStillOpen();
        server.verifyInteractionCompleted();
    }

    @Test
    public void open_withNoSecurityPlainAuth_usesLoginIfPlainUnavailable() throws Exception {
        username = "user";
        password = "password";
        authenticationType = AuthType.PLAIN;
        connectionSecurity = ConnectionSecurity.NONE;

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

        SmtpTransport transport = startServerAndCreateSmtpTransport(server);
        transport.open();

        server.verifyConnectionStillOpen();
        server.verifyInteractionCompleted();
    }

    @Test
    public void open_withNoSecurityPlainAuth_withNeither_throwsException() throws Exception {
        username = "user";
        password = "password";
        authenticationType = AuthType.PLAIN;
        connectionSecurity = ConnectionSecurity.NONE;

        MockSmtpServer server = new MockSmtpServer();
        server.output("220 localhost Simple Mail Transfer Service Ready");
        server.expect("EHLO localhost");
        server.output("250-localhost Hello client.localhost");
        server.output("250 AUTH");

        try {
            SmtpTransport transport = startServerAndCreateSmtpTransport(server);
            transport.open();
            fail("Exception expected");
        } catch (MessagingException e) {
            assertEquals("Authentication methods SASL PLAIN and LOGIN are unavailable.", e.getMessage());
        }

        server.verifyConnectionStillOpen();
        server.verifyInteractionCompleted();
    }

    @Test
    public void open_withNoSecurityCramMd5Auth_connectsToServer() throws Exception {
        username = "user";
        password = "password";
        authenticationType = AuthType.CRAM_MD5;
        connectionSecurity = ConnectionSecurity.NONE;

        MockSmtpServer server = new MockSmtpServer();
        server.output("220 localhost Simple Mail Transfer Service Ready");
        server.expect("EHLO localhost");
        server.output("250-localhost Hello client.localhost");
        server.output("250 AUTH CRAM-MD5");
        server.expect("AUTH CRAM-MD5");
        server.output(Base64.encode("<24609.1047914046@localhost>"));
        server.expect("dXNlciA3NmYxNWEzZmYwYTNiOGI1NzcxZmNhODZlNTcyMDk2Zg==");
        server.output("235 2.7.0 Authentication successful");

        SmtpTransport transport = startServerAndCreateSmtpTransport(server);
        transport.open();

        server.verifyConnectionStillOpen();
        server.verifyInteractionCompleted();
    }

    @Test
    public void open_withNoSecurityCramMd5Auth_withNoSupport_throwsException() throws Exception {
        username = "user";
        password = "password";
        authenticationType = AuthType.CRAM_MD5;
        connectionSecurity = ConnectionSecurity.NONE;

        MockSmtpServer server = new MockSmtpServer();
        server.output("220 localhost Simple Mail Transfer Service Ready");
        server.expect("EHLO localhost");
        server.output("250-localhost Hello client.localhost");
        server.output("250 AUTH PLAIN LOGIN");

        try {
            SmtpTransport transport = startServerAndCreateSmtpTransport(server);
            transport.open();
            fail("Exception expected");
        } catch (MessagingException e) {
            assertEquals("Authentication method CRAM-MD5 is unavailable.", e.getMessage());
        }

        server.verifyConnectionStillOpen();
        server.verifyInteractionCompleted();
    }

    @Test
    public void open_withNoSecurityExternalAuth_connectsToServer() throws Exception {
        username = "user";
        password = "password";
        authenticationType = AuthType.EXTERNAL;
        connectionSecurity = ConnectionSecurity.NONE;

        MockSmtpServer server = new MockSmtpServer();
        server.output("220 localhost Simple Mail Transfer Service Ready");
        server.expect("EHLO localhost");
        server.output("250-localhost Hello client.localhost");
        server.output("250 AUTH EXTERNAL");
        server.expect("AUTH EXTERNAL dXNlcg==");
        server.output("235 2.7.0 Authentication successful");

        SmtpTransport transport = startServerAndCreateSmtpTransport(server);
        transport.open();

        server.verifyConnectionStillOpen();
        server.verifyInteractionCompleted();
    }

    @Test
    public void open_withNoSecurityExternal_withNoSupport_throwsException() throws Exception {
        username = "user";
        password = "password";
        authenticationType = AuthType.EXTERNAL;
        connectionSecurity = ConnectionSecurity.NONE;

        MockSmtpServer server = new MockSmtpServer();
        server.output("220 localhost Simple Mail Transfer Service Ready");
        server.expect("EHLO localhost");
        server.output("250-localhost Hello client.localhost");
        server.output("250 AUTH");

        try {
            SmtpTransport transport = startServerAndCreateSmtpTransport(server);
            transport.open();
            fail("Exception expected");
        } catch (CertificateValidationException e) {
            assertEquals(CertificateValidationException.Reason.MissingCapability, e.getReason());
        }

        server.verifyConnectionStillOpen();
        server.verifyInteractionCompleted();
    }

    @Test
    public void open_withNoSecurityAutomatic_connectsToServerWithCramMD5IfSupported() throws Exception {
        username = "user";
        password = "password";
        authenticationType = AuthType.AUTOMATIC;
        connectionSecurity = ConnectionSecurity.NONE;

        MockSmtpServer server = new MockSmtpServer();
        server.output("220 localhost Simple Mail Transfer Service Ready");
        server.expect("EHLO localhost");
        server.output("250-localhost Hello client.localhost");
        server.output("250 AUTH CRAM-MD5");
        server.expect("AUTH CRAM-MD5");
        server.output(Base64.encode("<24609.1047914046@localhost>"));
        server.expect("dXNlciA3NmYxNWEzZmYwYTNiOGI1NzcxZmNhODZlNTcyMDk2Zg==");
        server.output("235 2.7.0 Authentication successful");

        SmtpTransport transport = startServerAndCreateSmtpTransport(server);
        transport.open();

        server.verifyConnectionStillOpen();
        server.verifyInteractionCompleted();
    }

    @Test
    public void open_withNoSecurityAutomatic_withCramMD5Unsupported_throwsException() throws Exception {
        username = "user";
        password = "password";
        authenticationType = AuthType.AUTOMATIC;
        connectionSecurity = ConnectionSecurity.NONE;

        MockSmtpServer server = new MockSmtpServer();
        server.output("220 localhost Simple Mail Transfer Service Ready");
        server.expect("EHLO localhost");
        server.output("250-localhost Hello client.localhost");
        server.output("250 AUTH PLAIN LOGIN");

        try {
            SmtpTransport transport = startServerAndCreateSmtpTransport(server);
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
    public void open_triesHELO_whenServerDoesntSupportEHLO() throws Exception {
        username = "user";
        authenticationType = AuthType.PLAIN;
        connectionSecurity = ConnectionSecurity.NONE;

        MockSmtpServer server = new MockSmtpServer();
        server.output("220 localhost Simple Mail Transfer Service Ready");
        server.expect("EHLO localhost");
        server.output("502 5.5.1, Unrecognized command.");
        server.expect("HELO localhost");
        server.output("250 localhost");

        SmtpTransport transport = startServerAndCreateSmtpTransport(server);
        transport.open();

        server.verifyConnectionStillOpen();
        server.verifyInteractionCompleted();
    }

    @Test
    public void sendMessage_withNoAddressToSendTo_doesntOpenConnection() throws Exception {
        MimeMessage message = new MimeMessage();

        MockSmtpServer server = new MockSmtpServer();
        setupConnectAndPlainAuthentication(server);

        SmtpTransport transport = startServerAndCreateSmtpTransport(server);
        transport.sendMessage(message);

        server.verifyConnectionNeverCreated();
    }

    @Test
    public void sendMessage_withToAddressToSendTo_opensConnection() throws Exception {
        TestMessage message = new TestMessage();
        message.setFrom(new Address("user@localhost"));
        message.setRecipients(Message.RecipientType.TO, new Address[] { new Address("user2@localhost") });

        MockSmtpServer server = new MockSmtpServer();
        setupConnectAndPlainAuthentication(server);
        server.expect("MAIL FROM:<user@localhost>");
        server.output("250 OK");
        server.expect("RCPT TO:<user2@localhost>");
        server.output("250 OK");
        server.expect("DATA");
        server.output("354 End data with <CR><LF>.<CR><LF>");
        server.expect("");
        server.expect(".");
        server.output("250 OK: queued as 12345");
        server.expect("QUIT");
        server.output("221 BYE");
        
        SmtpTransport transport = startServerAndCreateSmtpTransport(server);
        transport.sendMessage(message);
    }

    @Test
    public void sendMessage_with8BitEncoding_usesEncoding() throws Exception {
        extensions.add("8BITMIME");
        TestMessage message = new TestMessage();
        message.setFrom(new Address("user@localhost"));
        message.setRecipients(Message.RecipientType.TO, new Address[] { new Address("user2@localhost") });

        MockSmtpServer server = new MockSmtpServer();
        setupConnectAndPlainAuthentication(server);
        server.expect("MAIL FROM:<user@localhost> BODY=8BITMIME");
        server.output("250 OK");
        server.expect("RCPT TO:<user2@localhost>");
        server.output("250 OK");
        server.expect("DATA");
        server.output("354 End data with <CR><LF>.<CR><LF>");
        server.expect("");
        server.expect(".");
        server.output("250 OK: queued as 12345");
        server.expect("QUIT");
        server.output("221 BYE");
        
        SmtpTransport transport = startServerAndCreateSmtpTransport(server);
        transport.sendMessage(message);
    }

    @Test
    public void sendMessage_withMessageTooLarge_throwsException() throws Exception {
        extensions.add("SIZE 1000");
        TestMessage message = new TestMessage();
        message.setFrom(new Address("user@localhost"));
        message.setRecipients(Message.RecipientType.TO, new Address[] { new Address("user2@localhost") });
        message.setAttachmentCount(1);
        message.setBody(new BinaryMemoryBody(new byte[1001], "US-ASCII"));

        MockSmtpServer server = new MockSmtpServer();
        setupConnectAndPlainAuthentication(server);

        SmtpTransport transport = startServerAndCreateSmtpTransport(server);

        try {
            transport.sendMessage(message);
            fail("Expected message too large error");
        } catch (MessagingException e) {
            assertTrue(e.isPermanentFailure());
            assertEquals("Message too large for server", e.getMessage());
        }
    }

    @Test
    public void sendMessage_withNegativeReply_throwsException() throws Exception {
        TestMessage message = new TestMessage();
        message.setFrom(new Address("user@localhost"));
        message.setRecipients(Message.RecipientType.TO, new Address[] { new Address("user2@localhost") });

        MockSmtpServer server = new MockSmtpServer();
        setupConnectAndPlainAuthentication(server);
        server.expect("MAIL FROM:<user@localhost>");
        server.output("250 OK");
        server.expect("RCPT TO:<user2@localhost>");
        server.output("250 OK");
        server.expect("DATA");
        server.output("354 End data with <CR><LF>.<CR><LF>");
        server.expect("");
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
    }

    private void resetConnectionParameters() {
        host = null;
        port = -1;
        username = null;
        password = null;
        authenticationType = null;
        clientCertificateAlias = null;
        connectionSecurity = null;
        extensions = new ArrayList<>();
    }

    private SmtpTransport startServerAndCreateSmtpTransport(MockSmtpServer server) throws IOException,
            MessagingException {
        server.start();
        host = server.getHost();
        port = server.getPort();
        ServerSettings serverSettings = new ServerSettings(Type.SMTP, host, port, connectionSecurity,
                authenticationType, username, password, clientCertificateAlias);
        String uri = SmtpTransport.createUri(serverSettings);
        when(storeConfig.getTransportUri()).thenReturn(uri);
        return createSmtpTransport(storeConfig, socketFactory);
    }

    private SmtpTransport createSmtpTransport(StoreConfig storeConfig, TrustedSocketFactory socketFactory)
            throws MessagingException {
        return new SmtpTransport(storeConfig, socketFactory);
    }

    private void setupConnectAndPlainAuthentication(MockSmtpServer server) {
        username = "user";
        password = "password";
        authenticationType = AuthType.PLAIN;
        connectionSecurity = ConnectionSecurity.NONE;

        server.output("220 localhost Simple Mail Transfer Service Ready");
        server.expect("EHLO localhost");
        server.output("250-localhost Hello client.localhost");
        for (String extension : extensions) {
            server.output("250-" + extension);
        }
        server.output("250 AUTH LOGIN PLAIN CRAM-MD5");
        server.expect("AUTH PLAIN AHVzZXIAcGFzc3dvcmQ=");
        server.output("235 2.7.0 Authentication successful");
    }
}
