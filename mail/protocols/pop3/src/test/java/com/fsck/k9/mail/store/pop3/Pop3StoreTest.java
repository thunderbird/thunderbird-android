package com.fsck.k9.mail.store.pop3;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.AuthenticationFailedException;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.ServerSettings;
import com.fsck.k9.mail.filter.Base64;
import com.fsck.k9.mail.ssl.TrustedSocketFactory;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
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
    private TrustedSocketFactory mockTrustedSocketFactory = mock(TrustedSocketFactory.class);
    private Socket mockSocket = mock(Socket.class);
    private OutputStream mockOutputStream = mock(OutputStream.class);


    @Before
    public void setUp() throws Exception {
        ServerSettings serverSettings = createServerSettings();
        when(mockTrustedSocketFactory.createSocket(null, "server", 12345, null)).thenReturn(mockSocket);
        when(mockSocket.isConnected()).thenReturn(true);
        when(mockSocket.isClosed()).thenReturn(false);

        when(mockSocket.getOutputStream()).thenReturn(mockOutputStream);
        store = new Pop3Store(serverSettings, mockTrustedSocketFactory);
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

        assertEquals("TestFolder", folder.getServerId());
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
        Pop3Folder folder = store.getFolder(Pop3Folder.INBOX);

        folder.open();

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
        Pop3Folder folder = store.getFolder(Pop3Folder.INBOX);

        folder.open();
    }

    private ServerSettings createServerSettings() {
        return new ServerSettings(
                "pop3",
                "server",
                12345,
                ConnectionSecurity.SSL_TLS_REQUIRED,
                AuthType.PLAIN,
                "user",
                "password",
                null);
    }
}
