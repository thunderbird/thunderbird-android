package com.fsck.k9.mail.store.pop3;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;

import com.fsck.k9.mail.AuthenticationFailedException;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.Folder.FolderType;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.filter.Base64;
import com.fsck.k9.mail.ssl.TrustedSocketFactory;
import com.fsck.k9.mail.store.StoreConfig;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
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


    private Pop3Store store;
    private StoreConfig mockStoreConfig = mock(StoreConfig.class);
    private TrustedSocketFactory mockTrustedSocketFactory = mock(TrustedSocketFactory.class);
    private Socket mockSocket = mock(Socket.class);
    private OutputStream mockOutputStream = mock(OutputStream.class);


    @Before
    public void setUp() throws Exception {
        //Using a SSL socket allows us to mock it
        when(mockStoreConfig.getStoreUri()).thenReturn("pop3+ssl+://PLAIN:user:password@server:12345");
        when(mockStoreConfig.getInboxFolderName()).thenReturn("Inbox");
        when(mockTrustedSocketFactory.createSocket(null, "server", 12345, null)).thenReturn(mockSocket);
        when(mockSocket.isConnected()).thenReturn(true);
        when(mockSocket.isClosed()).thenReturn(false);

        when(mockSocket.getOutputStream()).thenReturn(mockOutputStream);
        store = new Pop3Store(mockStoreConfig, mockTrustedSocketFactory);
    }

    @Test
    public void getFolder_shouldReturnSameFolderEachTime() {
        Folder folderOne = store.getFolder("TestFolder");
        Folder folderTwo = store.getFolder("TestFolder");

        assertSame(folderOne, folderTwo);
    }

    @Test
    public void getFolder_shouldReturnFolderWithCorrectName() throws Exception {
        Folder folder = store.getFolder("TestFolder");

        assertEquals("TestFolder", folder.getName());
    }

    @Test
    public void create_withHoldsFoldersArgument_shouldDoNothing() throws Exception {
        Folder folder = store.getFolder("TestFolder");

        boolean result = folder.create(FolderType.HOLDS_FOLDERS);

        assertFalse(result);
        verifyZeroInteractions(mockSocket);
    }

    @Test
    public void create_withHoldsMessagesArgument_shouldDoNothing() throws Exception {
        Folder folder = store.getFolder("TestFolder");

        boolean result = folder.create(FolderType.HOLDS_MESSAGES);

        assertFalse(result);
        verifyZeroInteractions(mockSocket);
    }

    @Test
    public void exists_withInbox_shouldReturnTrue() throws Exception {
        Folder inbox = store.getFolder("Inbox");

        boolean result = inbox.exists();

        assertTrue(result);
    }

    @Test
    public void exists_withNonInboxFolder_shouldReturnFalse() throws Exception {
        Folder folder = store.getFolder("TestFolder");

        boolean result = folder.exists();

        assertFalse(result);
    }

    @Test
    public void getUnreadMessageCount_shouldBeMinusOne() throws Exception {
        Folder inbox = store.getFolder("Inbox");

        int result = inbox.getUnreadMessageCount();

        assertEquals(-1, result);
    }

    @Test
    public void getFlaggedMessageCount_shouldBeMinusOne() throws Exception {
        Folder inbox = store.getFolder("Inbox");

        int result = inbox.getFlaggedMessageCount();

        assertEquals(-1, result);
    }

    @Test
    public void getPersonalNamespace_shouldReturnListConsistingOfInbox() throws Exception {
        List<? extends Folder> folders = store.getPersonalNamespaces(true);

        assertEquals(1, folders.size());
        assertEquals("Inbox", folders.get(0).getName());
    }

    @Test
    public void isSeenFlagSupported_shouldReturnFalse() throws Exception {
        boolean result = store.isSeenFlagSupported();

        assertFalse(result);
    }

    @Test(expected = MessagingException.class)
    public void open_withoutInboxFolder_shouldThrow() throws Exception {
        Folder folder = store.getFolder("TestFolder");

        folder.open(Folder.OPEN_MODE_RW);
    }

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
        Folder folder = store.getFolder("Inbox");

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
        Folder folder = store.getFolder("Inbox");

        folder.open(Folder.OPEN_MODE_RW);
    }
}
