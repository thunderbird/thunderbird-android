package com.fsck.k9.mail.store.pop3;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.fsck.k9.mail.FetchProfile;
import com.fsck.k9.mail.FetchProfile.Item;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.Folder.FolderType;
import com.fsck.k9.mail.MessageRetrievalListener;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.BinaryTempFileBody;
import com.fsck.k9.mail.store.StoreConfig;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;


public class Pop3FolderTest {
    private Pop3Store mockStore;
    private Pop3Connection mockConnection;
    private StoreConfig mockStoreConfig;
    private MessageRetrievalListener<Pop3Message> mockListener;
    private Pop3Folder folder;

    @Before
    public void before() throws MessagingException {
        mockStore = mock(Pop3Store.class);
        mockConnection = mock(Pop3Connection.class);
        mockStoreConfig = mock(StoreConfig.class);
        mockListener = mock(MessageRetrievalListener.class);
        when(mockStore.getConfig()).thenReturn(mockStoreConfig);
        when(mockStoreConfig.getInboxFolderId()).thenReturn("Inbox");
        when(mockStore.createConnection()).thenReturn(mockConnection);
        when(mockConnection.executeSimpleCommand(Pop3Commands.STAT_COMMAND)).thenReturn("+OK 10 0");
        folder = new Pop3Folder(mockStore, "Inbox");
        setupTempDirectory();
    }

    private void setupTempDirectory() {
        File tempDirectory = new File("temp");
        if (!tempDirectory.exists()) {
            assertTrue(tempDirectory.mkdir());
            tempDirectory.deleteOnExit();
        }
        BinaryTempFileBody.setTempDirectory(tempDirectory);
    }

    @Test
    public void create_withHoldsFoldersArgument_shouldDoNothing() throws Exception {
        Pop3Folder folder = new Pop3Folder(mockStore, "TestFolder");

        boolean result = folder.create(FolderType.HOLDS_FOLDERS);

        assertFalse(result);
        verifyZeroInteractions(mockConnection);
    }

    @Test
    public void create_withHoldsMessagesArgument_shouldDoNothing() throws Exception {
        Pop3Folder folder = new Pop3Folder(mockStore, "TestFolder");

        boolean result = folder.create(FolderType.HOLDS_MESSAGES);

        assertFalse(result);
        verifyZeroInteractions(mockConnection);
    }

    @Test
    public void exists_withInbox_shouldReturnTrue() throws Exception {
        boolean result = folder.exists();

        assertTrue(result);
    }

    @Test
    public void exists_withNonInboxFolder_shouldReturnFalse() throws Exception {
        folder = new Pop3Folder(mockStore, "TestFolder");

        boolean result = folder.exists();

        assertFalse(result);
    }

    @Test
    public void getUnreadMessageCount_shouldBeMinusOne() throws Exception {
        int result = folder.getUnreadMessageCount();

        assertEquals(-1, result);
    }

    @Test
    public void getFlaggedMessageCount_shouldBeMinusOne() throws Exception {
        int result = folder.getFlaggedMessageCount();

        assertEquals(-1, result);
    }

    @Test(expected = MessagingException.class)
    public void open_withoutInboxFolder_shouldThrow() throws Exception {
        Pop3Folder folder = new Pop3Folder(mockStore, "TestFolder");

        folder.open(Folder.OPEN_MODE_RW);
    }

    @Test
    public void open_withoutInboxFolder_shouldNotTryAndCreateConnection() throws Exception {
        Pop3Folder folder = new Pop3Folder(mockStore, "TestFolder");
        try {
            folder.open(Folder.OPEN_MODE_RW);
        } catch (Exception ignored) {}
        verify(mockStore, never()).createConnection();
    }

    @Test(expected = MessagingException.class)
    public void open_withInboxFolderWithExceptionCreatingConnection_shouldThrow()
            throws MessagingException {

        when(mockStore.createConnection()).thenThrow(new MessagingException("Test"));
        folder.open(Folder.OPEN_MODE_RW);
    }

    @Test
    public void open_withInboxFolder_shouldSetMessageCountFromStatResponse()
            throws MessagingException {
        folder.open(Folder.OPEN_MODE_RW);

        int messageCount = folder.getMessageCount();

        assertEquals(10, messageCount);
    }

    @Test(expected = MessagingException.class)
    public void open_withInboxFolder_whenStatCommandFails_shouldThrow()
            throws MessagingException {
        when(mockConnection.executeSimpleCommand(Pop3Commands.STAT_COMMAND))
                .thenThrow(new MessagingException("Test"));

        folder.open(Folder.OPEN_MODE_RW);
    }

    @Test
    public void open_whenAlreadyOpenWithValidConnection_doesNotCreateAnotherConnection()
            throws MessagingException {
        folder.open(Folder.OPEN_MODE_RW);
        when(mockConnection.isOpen()).thenReturn(true);

        folder.open(Folder.OPEN_MODE_RW);

        verify(mockStore, times(1)).createConnection();
    }

    @Test
    public void getMode_withFolderOpenedInRO_isRW() throws MessagingException {

        folder.open(Folder.OPEN_MODE_RO);

        int mode = folder.getMode();

        assertEquals(Folder.OPEN_MODE_RW, mode);
    }

    @Test
    public void close_onNonOpenedFolder_succeeds()
            throws MessagingException {


        folder.close();
    }

    @Test
    public void close_onOpenedFolder_succeeds()
            throws MessagingException {

        folder.open(Folder.OPEN_MODE_RW);

        folder.close();
    }

    @Test
    public void close_onOpenedFolder_sendsQUIT()
            throws MessagingException {

        folder.open(Folder.OPEN_MODE_RW);
        when(mockConnection.isOpen()).thenReturn(true);

        folder.close();

        verify(mockConnection).executeSimpleCommand(Pop3Commands.QUIT_COMMAND);
    }

    @Test
    public void close_withExceptionQuiting_ignoresException()
            throws MessagingException {

        folder.open(Folder.OPEN_MODE_RW);
        when(mockConnection.isOpen()).thenReturn(true);
        doThrow(new MessagingException("Test"))
                .when(mockConnection)
                .executeSimpleCommand(Pop3Commands.QUIT_COMMAND);

        folder.close();
    }

    @Test
    public void close_onOpenedFolder_closesConnection()
            throws MessagingException {

        folder.open(Folder.OPEN_MODE_RW);
        when(mockConnection.isOpen()).thenReturn(true);

        folder.close();

        verify(mockConnection).close();
    }

    @Test
    public void getMessages_returnsListOfMessagesOnServer() throws IOException, MessagingException {
        folder.open(Folder.OPEN_MODE_RW);

        when(mockConnection.readLine()).thenReturn("1 abcd").thenReturn(".");

        List<Pop3Message> result = folder.getMessages(1, 1, null, mockListener);

        assertEquals(1, result.size());
    }

    @Test(expected = MessagingException.class)
    public void getMessages_withInvalidSet_throwsException() throws IOException, MessagingException {
        folder.open(Folder.OPEN_MODE_RW);

        folder.getMessages(2, 1, null, mockListener);
    }

    @Test(expected = MessagingException.class)
    public void getMessages_withIOExceptionReadingLine_throwsException() throws IOException, MessagingException {
        folder.open(Folder.OPEN_MODE_RW);

        when(mockConnection.readLine()).thenThrow(new IOException("Test"));

        folder.getMessages(1, 1, null, mockListener);
    }

    @Test
    public void getMessage_withPreviouslyFetchedMessage_returnsMessage()
            throws IOException, MessagingException {
        folder.open(Folder.OPEN_MODE_RW);

        List<Pop3Message> messageList = setupMessageFromServer();

        Pop3Message message = folder.getMessage("abcd");

        assertSame(messageList.get(0), message);
    }

    @Test
    public void getMessage_withNoPreviouslyFetchedMessage_returnsNewMessage()
            throws IOException, MessagingException {
        folder.open(Folder.OPEN_MODE_RW);

        Pop3Message message = folder.getMessage("abcd");

        assertNotNull(message);
    }


    @Test
    public void fetch_withEnvelopeProfile_setsSizeOfMessage() throws MessagingException, IOException {
        folder.open(Folder.OPEN_MODE_RW);
        List<Pop3Message> messageList = setupMessageFromServer();
        FetchProfile fetchProfile = new FetchProfile();
        fetchProfile.add(Item.ENVELOPE);
        when(mockConnection.readLine()).thenReturn("1 100").thenReturn(".");

        folder.fetch(messageList, fetchProfile, mockListener);

        assertEquals(100, messageList.get(0).getSize());
    }

    @Test
    public void fetch_withBodyProfile_setsContentOfMessage() throws MessagingException, IOException {
        InputStream messageInputStream = new ByteArrayInputStream((
                "From: <adam@example.org>\r\n" +
                "To: <eva@example.org>\r\n" +
                "Subject: Testmail\r\n" +
                "MIME-Version: 1.0\r\n" +
                "Content-type: text/plain\r\n" +
                "Content-Transfer-Encoding: 7bit\r\n" +
                "\r\n" +
                "this is some test text.").getBytes());
        folder.open(Folder.OPEN_MODE_RW);
        List<Pop3Message> messageList = setupMessageFromServer();
        FetchProfile fetchProfile = new FetchProfile();
        fetchProfile.add(Item.BODY);
        when(mockConnection.readLine()).thenReturn("1 100").thenReturn(".");
        when(mockConnection.getInputStream()).thenReturn(messageInputStream);

        folder.fetch(messageList, fetchProfile, mockListener);

        ByteArrayOutputStream bodyData = new ByteArrayOutputStream();
        messageList.get(0).getBody().writeTo(bodyData);

        assertEquals("this is some test text.", new String(bodyData.toByteArray(), "UTF-8"));
    }

    private List<Pop3Message> setupMessageFromServer() throws IOException, MessagingException {
        when(mockConnection.readLine()).thenReturn("1 abcd").thenReturn(".");
        return folder.getMessages(1, 1, null, mockListener);
    }
}
