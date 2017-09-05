package com.fsck.k9.mail.store.pop3;


import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.Folder.FolderType;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.store.StoreConfig;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;


public class Pop3FolderTest {
    private Pop3Store mockStore;
    private Pop3Connection mockConnection;
    private StoreConfig mockStoreConfig;

    @Before
    public void before() throws MessagingException {
        mockStore = mock(Pop3Store.class);
        mockConnection = mock(Pop3Connection.class);
        mockStoreConfig = mock(StoreConfig.class);
        when(mockStore.getConfig()).thenReturn(mockStoreConfig);
        when(mockStoreConfig.getInboxFolderId()).thenReturn("Inbox");
        when(mockStore.createConnection()).thenReturn(mockConnection);
        when(mockConnection.executeSimpleCommand(Pop3Commands.STAT_COMMAND)).thenReturn("+OK 10 0");
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
        Pop3Folder inbox = new Pop3Folder(mockStore, "Inbox");

        boolean result = inbox.exists();

        assertTrue(result);
    }

    @Test
    public void exists_withNonInboxFolder_shouldReturnFalse() throws Exception {
        Pop3Folder folder = new Pop3Folder(mockStore, "TestFolder");

        boolean result = folder.exists();

        assertFalse(result);
    }

    @Test
    public void getUnreadMessageCount_shouldBeMinusOne() throws Exception {
        Pop3Folder inbox = new Pop3Folder(mockStore, "Inbox");

        int result = inbox.getUnreadMessageCount();

        assertEquals(-1, result);
    }

    @Test
    public void getFlaggedMessageCount_shouldBeMinusOne() throws Exception {
        Pop3Folder inbox = new Pop3Folder(mockStore, "Inbox");

        int result = inbox.getFlaggedMessageCount();

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
        Pop3Folder folder = new Pop3Folder(mockStore, "Inbox");
        when(mockStore.createConnection()).thenThrow(new MessagingException("Test"));
        folder.open(Folder.OPEN_MODE_RW);
    }

    @Test
    public void open_withInboxFolder_shouldSetMessageCountFromStatResponse()
            throws MessagingException {
        Pop3Folder folder = new Pop3Folder(mockStore, "Inbox");
        folder.open(Folder.OPEN_MODE_RW);

        int messageCount = folder.getMessageCount();

        assertEquals(10, messageCount);
    }

    @Test(expected = MessagingException.class)
    public void open_withInboxFolder_whenStatCommandFails_shouldThrow()
            throws MessagingException {
        when(mockConnection.executeSimpleCommand(Pop3Commands.STAT_COMMAND))
                .thenThrow(new MessagingException("Test"));

        Pop3Folder folder = new Pop3Folder(mockStore, "Inbox");
        folder.open(Folder.OPEN_MODE_RW);
    }

    @Test
    public void getMode_withFolderOpenedInRO_isRW() throws MessagingException {
        Pop3Folder folder = new Pop3Folder(mockStore, "Inbox");
        folder.open(Folder.OPEN_MODE_RO);

        int mode = folder.getMode();

        assertEquals(Folder.OPEN_MODE_RW, mode);
    }

    @Test
    public void close_onNonOpenedFolder_succeeds()
            throws MessagingException {
        Pop3Folder folder = new Pop3Folder(mockStore, "Inbox");

        folder.close();
    }

    @Test
    public void close_onOpenedFolder_succeeds()
            throws MessagingException {
        Pop3Folder folder = new Pop3Folder(mockStore, "Inbox");
        folder.open(Folder.OPEN_MODE_RW);

        folder.close();
    }

    @Test
    public void close_onOpenedFolder_sendsQUIT()
            throws MessagingException {
        Pop3Folder folder = new Pop3Folder(mockStore, "Inbox");
        folder.open(Folder.OPEN_MODE_RW);
        when(mockConnection.isOpen()).thenReturn(true);

        folder.close();

        verify(mockConnection).executeSimpleCommand(Pop3Commands.QUIT_COMMAND);
    }

    @Test
    public void close_withExceptionQuiting_ignoresException()
            throws MessagingException {
        Pop3Folder folder = new Pop3Folder(mockStore, "Inbox");
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
        Pop3Folder folder = new Pop3Folder(mockStore, "Inbox");
        folder.open(Folder.OPEN_MODE_RW);
        when(mockConnection.isOpen()).thenReturn(true);

        folder.close();

        verify(mockConnection).close();
    }
}
