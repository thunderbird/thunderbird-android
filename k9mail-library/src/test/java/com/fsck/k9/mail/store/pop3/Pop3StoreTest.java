package com.fsck.k9.mail.store.pop3;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;

import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.AuthenticationFailedException;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.ServerSettings;
import com.fsck.k9.mail.ServerSettings.Type;
import com.fsck.k9.mail.filter.Base64;
import com.fsck.k9.mail.ssl.TrustedSocketFactory;
import com.fsck.k9.mail.store.StoreConfig;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class Pop3StoreTest {
    private static final String INITIAL_RESPONSE = "+OK POP3 server greeting\r\n";
    private static final String AUTH = "AUTH\r\n";
    private static final String AUTH_HANDLE_RESPONSE = "+OK Listing of supported mechanisms follows\r\n" +
            "PLAIN\r\n" +
            "CRAM-MD5\r\n" +
            "EXTERNAL\r\n" +
            ".\r\n";
    private static final String CAPA = "CAPA\r\n";
    private static final String CAPA_RESPONSE = "+OK Listing of supported mechanisms follows\r\n" +
            "PLAIN\r\n" +
            "CRAM-MD5\r\n" +
            "EXTERNAL\r\n" +
            ".\r\n";
    private static final String AUTH_PLAIN_WITH_LOGIN = "AUTH PLAIN\r\n" +
            new String(Base64.encodeBase64(("\000user\000password").getBytes())) + "\r\n";
    private static final String AUTH_PLAIN_AUTHENTICATED_RESPONSE = "+OK\r\n" + "+OK\r\n";
    private static final String AUTH_PLAIN_FAILED_RESPONSE = "+OK\r\n" + "Plain authentication failure";
    private static final String STAT = "STAT\r\n";
    private static final String STAT_RESPONSE = "+OK 20 0\r\n";
    private static final String UIDL_UNSUPPORTED_RESPONSE = "-ERR UIDL unsupported\r\n";
    private static final String UIDL_SUPPORTED_RESPONSE = "+OK UIDL supported\r\n";


    private Pop3Store store;
    private StoreConfig mockStoreConfig = mock(StoreConfig.class);
    private TrustedSocketFactory mockTrustedSocketFactory = mock(TrustedSocketFactory.class);
    private Socket mockSocket = mock(Socket.class);
    private OutputStream mockOutputStream = mock(OutputStream.class);


    @Before
    public void setUp() throws Exception {
        //Using a SSL socket allows us to mock it
        when(mockStoreConfig.getStoreUri()).thenReturn("pop3+ssl+://PLAIN:user:password@server:12345");
        when(mockStoreConfig.getInboxFolderId()).thenReturn("Inbox");
        when(mockTrustedSocketFactory.createSocket(null, "server", 12345, null)).thenReturn(mockSocket);
        when(mockSocket.isConnected()).thenReturn(true);
        when(mockSocket.isClosed()).thenReturn(false);

        when(mockSocket.getOutputStream()).thenReturn(mockOutputStream);
        store = new Pop3Store(mockStoreConfig, mockTrustedSocketFactory);
    }

    @Test
    public void decodeUri_withTLSUri_shouldUseStartTls() {
        ServerSettings settings = Pop3Store.decodeUri("pop3+tls+://PLAIN:user:password@server:12345");

        assertEquals(settings.connectionSecurity, ConnectionSecurity.STARTTLS_REQUIRED);
    }

    @Test
    public void decodeUri_withPlainUri_shouldUseNoSecurity() {
        ServerSettings settings = Pop3Store.decodeUri("pop3://PLAIN:user:password@server:12345");

        assertEquals(settings.connectionSecurity, ConnectionSecurity.NONE);
    }

    @Test
    public void decodeUri_withExternalCertificateShouldProvideAlias_shouldUseNoSecurity() {
        ServerSettings settings = Pop3Store.decodeUri("pop3://EXTERNAL:user:clientCert@server:12345");

        assertEquals(settings.clientCertificateAlias, "clientCert");
    }

    @Test(expected = IllegalArgumentException.class)
    public void decodeUri_withNonPop3Uri_shouldThrowException() {
        Pop3Store.decodeUri("imap://PLAIN:user:password@server:12345");
    }

    @Test
    public void createUri_withSSLTLS_required_shouldProduceSSLUri() {
        ServerSettings settings = new ServerSettings(Type.POP3, "server", 12345, ConnectionSecurity.SSL_TLS_REQUIRED,
                AuthType.PLAIN, "user", "password", null);

        String uri = Pop3Store.createUri(settings);

        assertEquals(uri, "pop3+ssl+://PLAIN:user:password@server:12345");
    }

    @Test
    public void createUri_withSTARTTLSRequired_shouldProduceTLSUri() {
        ServerSettings settings = new ServerSettings(Type.POP3, "server", 12345, ConnectionSecurity.STARTTLS_REQUIRED,
                AuthType.PLAIN, "user", "password", null);

        String uri = Pop3Store.createUri(settings);

        assertEquals(uri, "pop3+tls+://PLAIN:user:password@server:12345");
    }

    @Test
    public void createUri_withNONE_shouldProducePop3Uri() {
        ServerSettings settings = new ServerSettings(Type.POP3, "server", 12345, ConnectionSecurity.NONE,
                AuthType.PLAIN, "user", "password", null);

        String uri = Pop3Store.createUri(settings);

        assertEquals(uri, "pop3://PLAIN:user:password@server:12345");
    }

    @Test
    public void createUri_withPLAIN_shouldProducePlainAuthUri() {
        ServerSettings settings = new ServerSettings(Type.POP3, "server", 12345, ConnectionSecurity.NONE,
                AuthType.PLAIN, "user", "password", null);

        String uri = Pop3Store.createUri(settings);

        assertEquals(uri, "pop3://PLAIN:user:password@server:12345");
    }

    @Test
    public void createUri_withEXTERNAL_shouldProduceExternalAuthUri() {
        ServerSettings settings = new ServerSettings(Type.POP3, "server", 12345, ConnectionSecurity.NONE,
                AuthType.EXTERNAL, "user", "password", "clientCert");

        String uri = Pop3Store.createUri(settings);

        assertEquals(uri, "pop3://EXTERNAL:user:clientCert@server:12345");
    }

    @Test
    public void createUri_withCRAMMD5_shouldProduceCRAMMD5AuthUri() {
        ServerSettings settings = new ServerSettings(Type.POP3, "server", 12345, ConnectionSecurity.NONE,
                AuthType.CRAM_MD5, "user", "password", "clientCert");

        String uri = Pop3Store.createUri(settings);

        assertEquals(uri, "pop3://CRAM_MD5:user:password@server:12345");
    }


    @Test(expected = MessagingException.class)
    public void withInvalidStoreUri_shouldThrowMessagingException() throws MessagingException {
        when(mockStoreConfig.getStoreUri()).thenReturn("pop3://CRAM_MD5:user:password@[]:12345");
        store = new Pop3Store(mockStoreConfig, mockTrustedSocketFactory);
    }

    @Test
    public void getFolder_shouldReturnSameFolderEachTime() {
        Pop3Folder folderOne = store.getFolder("TestFolder");
        Pop3Folder folderTwo = store.getFolder("TestFolder");

        assertSame(folderOne, folderTwo);
    }

    @Test
    public void getFolder_shouldReturnFolderWithCorrectName() throws Exception {
        Pop3Folder folder = store.getFolder("TestFolder");

        assertEquals("TestFolder", folder.getId());
    }

    @Test
    public void getPersonalNamespace_shouldReturnListConsistingOfInbox() throws Exception {
        List<Pop3Folder> folders = store.getPersonalNamespaces(true);

        assertEquals(1, folders.size());
        assertEquals("Inbox", folders.get(0).getId());
    }

    @Test
    public void isSeenFlagSupported_shouldReturnFalse() throws Exception {
        boolean result = store.isSeenFlagSupported();

        assertFalse(result);
    }

    @Test(expected = MessagingException.class)
    public void checkSetting_whenConnectionThrowsException_shouldThrowMessagingException()
            throws Exception {
        when(mockTrustedSocketFactory.createSocket(any(Socket.class),
                anyString(), anyInt(), anyString())).thenThrow(new IOException("Test"));
        store.checkSettings();
    }

    @Test(expected = MessagingException.class)
    public void checkSetting_whenUidlUnsupported_shouldThrowMessagingException()
            throws Exception {
        String response = INITIAL_RESPONSE +
                AUTH_HANDLE_RESPONSE +
                CAPA_RESPONSE +
                AUTH_PLAIN_AUTHENTICATED_RESPONSE +
                STAT_RESPONSE +
                UIDL_UNSUPPORTED_RESPONSE;
        when(mockSocket.getInputStream()).thenReturn(new ByteArrayInputStream(response.getBytes("UTF-8")));
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        when(mockSocket.getOutputStream()).thenReturn(byteArrayOutputStream);
        store.checkSettings();
    }

    @Test
    public void checkSetting_whenUidlSupported_shouldReturn()
            throws Exception {
        String response = INITIAL_RESPONSE +
                AUTH_HANDLE_RESPONSE +
                CAPA_RESPONSE +
                AUTH_PLAIN_AUTHENTICATED_RESPONSE +
                STAT_RESPONSE +
                UIDL_SUPPORTED_RESPONSE;
        when(mockSocket.getInputStream()).thenReturn(new ByteArrayInputStream(response.getBytes("UTF-8")));
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        when(mockSocket.getOutputStream()).thenReturn(byteArrayOutputStream);
        store.checkSettings();
    }

    // Component Level Tests

    @Test
    public void open_withAuthResponseUsingAuthPlain_shouldRetrieveMessageCountOnAuthenticatedSocket() throws Exception {
        String response = INITIAL_RESPONSE +
                AUTH_HANDLE_RESPONSE +
                CAPA_RESPONSE +
                AUTH_PLAIN_AUTHENTICATED_RESPONSE +
                STAT_RESPONSE;
        when(mockSocket.getInputStream()).thenReturn(new ByteArrayInputStream(response.getBytes("UTF-8")));
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        when(mockSocket.getOutputStream()).thenReturn(byteArrayOutputStream);
        Pop3Folder folder = store.getFolder("Inbox");

        folder.open(Folder.OPEN_MODE_RW);

        assertEquals(20, folder.getMessageCount());
        assertEquals(AUTH + CAPA + AUTH_PLAIN_WITH_LOGIN + STAT, byteArrayOutputStream.toString("UTF-8"));
    }

    @Test(expected = AuthenticationFailedException.class)
    public void open_withFailedAuth_shouldThrow() throws Exception {
        String response = INITIAL_RESPONSE +
                AUTH_HANDLE_RESPONSE +
                CAPA_RESPONSE +
                AUTH_PLAIN_FAILED_RESPONSE;
        when(mockSocket.getInputStream()).thenReturn(new ByteArrayInputStream(response.getBytes("UTF-8")));
        Pop3Folder folder = store.getFolder("Inbox");

        folder.open(Folder.OPEN_MODE_RW);
    }
}
