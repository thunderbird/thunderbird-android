package com.fsck.k9.mail.store.pop3;


import com.fsck.k9.mail.FetchProfile;
import com.fsck.k9.mail.FetchProfile.Item;
import com.fsck.k9.mail.MessageRetrievalListener;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.BinaryTempFileBody;
import com.fsck.k9.mail.store.StoreConfig;

import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
        when(mockStoreConfig.getInboxFolder()).thenReturn(Pop3Folder.INBOX);
        when(mockStore.createConnection()).thenReturn(mockConnection);
        when(mockConnection.executeSimpleCommand(Pop3Commands.STAT_COMMAND)).thenReturn("+OK 10 0");
        folder = new Pop3Folder(mockStore, Pop3Folder.INBOX);
        BinaryTempFileBody.setTempDirectory(new File(System.getProperty("java.io.tmpdir")));
    }

    @Test(expected = MessagingException.class)
    public void open_withoutInboxFolder_shouldThrow() throws Exception {
        Pop3Folder folder = new Pop3Folder(mockStore, "TestFolder");

        folder.open();
    }

    @Test
    public void open_withoutInboxFolder_shouldNotTryAndCreateConnection() throws Exception {
        Pop3Folder folder = new Pop3Folder(mockStore, "TestFolder");
        try {
            folder.open();
        } catch (Exception ignored) {}
        verify(mockStore, never()).createConnection();
    }

    @Test(expected = MessagingException.class)
    public void open_withInboxFolderWithExceptionCreatingConnection_shouldThrow()
            throws MessagingException {

        when(mockStore.createConnection()).thenThrow(new MessagingException("Test"));
        folder.open();
    }

    @Test
    public void open_withInboxFolder_shouldSetMessageCountFromStatResponse()
            throws MessagingException {
        folder.open();

        int messageCount = folder.getMessageCount();

        assertEquals(10, messageCount);
    }

    @Test(expected = MessagingException.class)
    public void open_withInboxFolder_whenStatCommandFails_shouldThrow()
            throws MessagingException {
        when(mockConnection.executeSimpleCommand(Pop3Commands.STAT_COMMAND))
                .thenThrow(new MessagingException("Test"));

        folder.open();
    }

    @Test
    public void open_createsAndOpensConnection()
            throws MessagingException {
        folder.open();

        verify(mockStore, times(1)).createConnection();
        verify(mockConnection).open();
    }

    @Test
    public void open_whenAlreadyOpenWithValidConnection_doesNotCreateAnotherConnection()
            throws MessagingException {
        folder.open();
        when(mockConnection.isOpen()).thenReturn(true);

        folder.open();

        verify(mockStore, times(1)).createConnection();
    }

    @Test
    public void close_onNonOpenedFolder_succeeds()
            throws MessagingException {


        folder.close();
    }

    @Test
    public void close_onOpenedFolder_succeeds()
            throws MessagingException {

        folder.open();

        folder.close();
    }

    @Test
    public void close_onOpenedFolder_sendsQUIT()
            throws MessagingException {

        folder.open();
        when(mockConnection.isOpen()).thenReturn(true);

        folder.close();

        verify(mockConnection).executeSimpleCommand(Pop3Commands.QUIT_COMMAND);
    }

    @Test
    public void close_withExceptionQuiting_ignoresException()
            throws MessagingException {

        folder.open();
        when(mockConnection.isOpen()).thenReturn(true);
        doThrow(new MessagingException("Test"))
                .when(mockConnection)
                .executeSimpleCommand(Pop3Commands.QUIT_COMMAND);

        folder.close();
    }

    @Test
    public void close_onOpenedFolder_closesConnection()
            throws MessagingException {

        folder.open();
        when(mockConnection.isOpen()).thenReturn(true);

        folder.close();

        verify(mockConnection).close();
    }

    @Test
    public void getMessages_returnsListOfMessagesOnServer() throws IOException, MessagingException {
        folder.open();

        when(mockConnection.readLine()).thenReturn("1 abcd").thenReturn(".");

        List<Pop3Message> result = folder.getMessages(1, 1, mockListener);

        assertEquals(1, result.size());
    }

    @Test(expected = MessagingException.class)
    public void getMessages_withInvalidSet_throwsException() throws IOException, MessagingException {
        folder.open();

        folder.getMessages(2, 1, mockListener);
    }

    @Test(expected = MessagingException.class)
    public void getMessages_withIOExceptionReadingLine_throwsException() throws IOException, MessagingException {
        folder.open();

        when(mockConnection.readLine()).thenThrow(new IOException("Test"));

        folder.getMessages(1, 1, mockListener);
    }

    @Test
    public void getMessage_withPreviouslyFetchedMessage_returnsMessage()
            throws IOException, MessagingException {
        folder.open();

        List<Pop3Message> messageList = setupMessageFromServer();

        Pop3Message message = folder.getMessage("abcd");

        assertSame(messageList.get(0), message);
    }

    @Test
    public void getMessage_withNoPreviouslyFetchedMessage_returnsNewMessage()
            throws IOException, MessagingException {
        folder.open();

        Pop3Message message = folder.getMessage("abcd");

        assertNotNull(message);
    }


    @Test
    public void fetch_withEnvelopeProfile_setsSizeOfMessage() throws MessagingException, IOException {
        folder.open();
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
        folder.open();
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
        return folder.getMessages(1, 1, mockListener);
    }
}
