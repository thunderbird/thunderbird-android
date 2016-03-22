package com.fsck.k9.mail.store.pop3;

import com.fsck.k9.mail.AuthenticationFailedException;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.filter.Base64;
import com.fsck.k9.mail.ssl.TrustedSocketFactory;
import com.fsck.k9.mail.store.StoreConfig;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class Pop3StoreTest {
    private Pop3Store store;
    private StoreConfig mockStoreConfig = mock(StoreConfig.class);
    private TrustedSocketFactory mockTrustedSocketFactory = mock(TrustedSocketFactory.class);
    private Socket mockSocket = mock(Socket.class);
    private OutputStream mockOutputStream = mock(OutputStream.class);

    private static final String INITIAL_RESPONSE =
            "+OK POP3 server greeting\r\n";
    private static final String AUTH = "AUTH\r\n";
    private static final String AUTH_HANDLE_RESPONSE =
            "+OK Listing of supported mechanisms follows\r\n" +
                    "PLAIN\r\n" +
                    "CRAM-MD5\r\n" +
                    "EXTERNAL\r\n" +
                    ".\r\n";
    private static final String CAPA = "CAPA\r\n";
    private static final String CAPA_RESPONSE =
            "+OK Listing of supported mechanisms follows\r\n" +
                    "PLAIN\r\n" +
                    "CRAM-MD5\r\n" +
                    "EXTERNAL\r\n" +
                    ".\r\n";
    private static final String AUTH_PLAIN_WITH_LOGIN =
            "AUTH PLAIN\r\n" +
            new String(Base64.encodeBase64(("\000user\000password").getBytes()))+"\r\n";
    private static final String AUTH_PLAIN_AUTHENTICATED_RESPONSE =
            "+OK\r\n" +
            "+OK\r\n";
    private static final String AUTH_PLAIN_FAILED_RESPONSE =
            "+OK\r\n" +
            "Plain authentication failure";
    private static final String STAT = "STAT\r\n";
    private static final String STAT_RESPONSE =
            "+OK 20 0\r\n";

    @Before
    public void setUp() throws Exception {
        //Using a SSL socket allows us to mock it
        when(mockStoreConfig.getStoreUri()).thenReturn(
                "pop3+ssl+://PLAIN:user:password@server:12345");
        when(mockStoreConfig.getInboxFolderName()).thenReturn("Inbox");
        when(mockTrustedSocketFactory.createSocket(null, "server", 12345, null)).thenReturn(mockSocket);
        when(mockSocket.isConnected()).thenReturn(true);
        when(mockSocket.isClosed()).thenReturn(false);

        when(mockSocket.getOutputStream()).thenReturn(mockOutputStream);
        store = new Pop3Store(mockStoreConfig, mockTrustedSocketFactory);

    }

    @Test
    public void getFolder_returnsSameFolderEachTime() {
        Folder folder = store.getFolder("TestFolder");
        Folder folder2 = store.getFolder("TestFolder");
        assertSame(folder, folder2);
    }

    @Test
    public void getFolder_returnsFolderWithCorrectName() throws MessagingException {
        Folder folder = store.getFolder("TestFolder");
        assertEquals("TestFolder", folder.getName());
    }

    @Test
    public void createFolder_doesNothing() throws MessagingException {
        Folder folder = store.getFolder("TestFolder");

        assertFalse(folder.create(Folder.FolderType.HOLDS_FOLDERS));
        assertFalse(folder.create(Folder.FolderType.HOLDS_MESSAGES));
        verifyZeroInteractions(mockSocket);
    }

    @Test
    public void exists_returnsTrueForInbox() throws MessagingException {
        Folder inbox = store.getFolder("Inbox");

        assertTrue(inbox.exists());
    }

    @Test
    public void exists_returnsFalseForAnyOtherFolder() throws MessagingException {
        Folder folder = store.getFolder("TestFolder");

        assertFalse(folder.exists());
    }

    @Test
    public void unreadAndFlaggedMessageCount_isAlwaysMinus1() throws MessagingException {
        Folder inbox = store.getFolder("Inbox");

        assertEquals(-1, inbox.getUnreadMessageCount());
        assertEquals(-1, inbox.getFlaggedMessageCount());
    }



    @Test
    public void getPersonalNamespace_returns_list_containing_Inbox() throws MessagingException {
        List<? extends Folder> folders = store.getPersonalNamespaces(true);
        assertEquals(1, folders.size());
        assertEquals("Inbox", folders.get(0).getName());
    }

    @Test
    public void isSeenFlagSupported_isFalse() throws MessagingException {
        assertFalse(store.isSeenFlagSupported());
    }

    @Test(expected = MessagingException.class)
    public void openFolder_whenFolderIsntInbox_throwsMessagingException() throws MessagingException {
        Folder folder = store.getFolder("TestFolder");
        folder.open(Folder.OPEN_MODE_RW);
    }

    @Test
    public void openFolder_withAuthResponseUsingAuthPlain_retrivesMessageCountOnAuthenticatedSocket()
            throws MessagingException, IOException {
        String response =
                INITIAL_RESPONSE + AUTH_HANDLE_RESPONSE + CAPA_RESPONSE +
                        AUTH_PLAIN_AUTHENTICATED_RESPONSE + STAT_RESPONSE;
        when(mockSocket.getInputStream()).thenReturn(
                new ByteArrayInputStream(response.getBytes("UTF-8")));
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        when(mockSocket.getOutputStream()).thenReturn(byteArrayOutputStream);
        Folder folder = store.getFolder("Inbox");
        folder.open(Folder.OPEN_MODE_RW);
        assertEquals(20, folder.getMessageCount());
        assertEquals(AUTH + CAPA + AUTH_PLAIN_WITH_LOGIN + STAT, byteArrayOutputStream.toString("UTF-8"));
    }

    @Test(expected = AuthenticationFailedException.class)
    public void openFolder_withFailedAuth_throws_AuthenticationFailedException()
            throws IOException, MessagingException {
        String response =
                INITIAL_RESPONSE + AUTH_HANDLE_RESPONSE + CAPA_RESPONSE +
                        AUTH_PLAIN_FAILED_RESPONSE;
        when(mockSocket.getInputStream()).thenReturn(
                new ByteArrayInputStream(response.getBytes("UTF-8")));
        Folder folder = store.getFolder("Inbox");
        folder.open(Folder.OPEN_MODE_RW);
    }
}
