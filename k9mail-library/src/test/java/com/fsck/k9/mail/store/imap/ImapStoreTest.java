package com.fsck.k9.mail.store.imap;


import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.net.ConnectivityManager;

import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.oauth.OAuth2TokenProvider;
import com.fsck.k9.mail.ssl.TrustedSocketFactory;
import com.fsck.k9.mail.store.StoreConfig;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.util.collections.Sets;

import static com.fsck.k9.mail.store.imap.ImapResponseHelper.createImapResponse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;


public class ImapStoreTest {
    private StoreConfig storeConfig;
    private TestImapStore imapStore;
    private TrustedSocketFactory trustedSocketFactory;
    private ConnectivityManager connectivityManager;
    private OAuth2TokenProvider oauth2TokenProvider;

    @Before
    public void setUp() throws Exception {
        storeConfig = createStoreConfig();
        trustedSocketFactory = mock(TrustedSocketFactory.class);
        connectivityManager = mock(ConnectivityManager.class);
        oauth2TokenProvider = mock(OAuth2TokenProvider.class);

        imapStore = new TestImapStore(storeConfig, trustedSocketFactory, connectivityManager, oauth2TokenProvider);
    }

    @Test
    public void getFolder_shouldReturnImapFolderInstance() throws Exception {
        Folder result = imapStore.getFolder("INBOX");

        assertEquals(ImapFolder.class, result.getClass());
    }

    @Test
    public void getFolder_calledTwice_shouldReturnFirstInstance() throws Exception {
        String folderName = "Trash";
        Folder imapFolder = imapStore.getFolder(folderName);

        Folder result = imapStore.getFolder(folderName);

        assertEquals(imapFolder, result);
    }

    @Test
    public void checkSettings_shouldCreateImapConnectionAndCallOpen() throws Exception {
        ImapConnection imapConnection = mock(ImapConnection.class);
        imapStore.enqueueImapConnection(imapConnection);

        imapStore.checkSettings();

        verify(imapConnection).open();
    }

    @Test
    public void checkSettings_withOpenThrowing_shouldThrowMessagingException() throws Exception {
        ImapConnection imapConnection = mock(ImapConnection.class);
        doThrow(IOException.class).when(imapConnection).open();
        imapStore.enqueueImapConnection(imapConnection);

        try {
            imapStore.checkSettings();
            fail("Expected exception");
        } catch (MessagingException e) {
            assertEquals("Unable to connect", e.getMessage());
            assertNotNull(e.getCause());
            assertEquals(IOException.class, e.getCause().getClass());
        }
    }

    @Test
    public void autoconfigureFolders_withSpecialUseCapability_shouldSetSpecialFolders() throws Exception {
        ImapConnection imapConnection = mock(ImapConnection.class);
        when(imapConnection.hasCapability(Capabilities.SPECIAL_USE)).thenReturn(true);
        List<ImapResponse> imapResponses = Arrays.asList(
                createImapResponse("* LIST (\\HasNoChildren) \"/\" \"INBOX\""),
                createImapResponse("* LIST (\\Noselect \\HasChildren) \"/\" \"[Gmail]\""),
                createImapResponse("* LIST (\\HasNoChildren \\All) \"/\" \"[Gmail]/All Mail\""),
                createImapResponse("* LIST (\\HasNoChildren \\Drafts) \"/\" \"[Gmail]/Drafts\""),
                createImapResponse("* LIST (\\HasNoChildren \\Important) \"/\" \"[Gmail]/Important\""),
                createImapResponse("* LIST (\\HasNoChildren \\Sent) \"/\" \"[Gmail]/Sent Mail\""),
                createImapResponse("* LIST (\\HasNoChildren \\Junk) \"/\" \"[Gmail]/Spam\""),
                createImapResponse("* LIST (\\HasNoChildren \\Flagged) \"/\" \"[Gmail]/Starred\""),
                createImapResponse("* LIST (\\HasNoChildren \\Trash) \"/\" \"[Gmail]/Trash\""),
                createImapResponse("5 OK Success")
        );
        when(imapConnection.executeSimpleCommand("LIST (SPECIAL-USE) \"\" \"*\"")).thenReturn(imapResponses);

        imapStore.autoconfigureFolders(imapConnection);

        verify(storeConfig).setDraftsFolderId("[Gmail]/Drafts");
        verify(storeConfig).setSentFolderId("[Gmail]/Sent Mail");
        verify(storeConfig).setSpamFolderId("[Gmail]/Spam");
        verify(storeConfig).setTrashFolderId("[Gmail]/Trash");
        verify(storeConfig).setArchiveFolderId("[Gmail]/All Mail");
    }

    @Test
    public void autoconfigureFolders_withoutSpecialUseCapability_shouldNotIssueImapCommand() throws Exception {
        ImapConnection imapConnection = mock(ImapConnection.class);
        when(imapConnection.hasCapability(Capabilities.SPECIAL_USE)).thenReturn(false);

        imapStore.autoconfigureFolders(imapConnection);

        verify(imapConnection, atLeastOnce()).hasCapability(anyString());
        verifyNoMoreInteractions(imapConnection);
    }

    @Test
    public void getPersonalNamespaces_withForceListAll() throws Exception {
        when(storeConfig.subscribedFoldersOnly()).thenReturn(true);
        ImapConnection imapConnection = mock(ImapConnection.class);
        List<ImapResponse> imapResponses = Arrays.asList(
                createImapResponse("* LIST (\\HasNoChildren) \".\" \"INBOX\""),
                createImapResponse("* LIST (\\Noselect \\HasChildren) \".\" \"Folder\""),
                createImapResponse("* LIST (\\HasNoChildren) \".\" \"Folder.SubFolder\""),
                createImapResponse("6 OK Success")
        );
        when(imapConnection.executeSimpleCommand("LIST \"\" \"*\"")).thenReturn(imapResponses);
        imapStore.enqueueImapConnection(imapConnection);

        List<? extends Folder> result = imapStore.getFolders(true);

        assertNotNull(result);
        assertEquals(Sets.newSet("INBOX", "Folder.SubFolder"), extractFolderIds(result));
    }

    @Test
    public void getPersonalNamespaces_withPathPrefix() throws Exception {
        when(storeConfig.getStoreUri()).thenReturn("imap://user:password@imap.example.org/0%7CpathPrefix/");
        imapStore = new TestImapStore(storeConfig, trustedSocketFactory, connectivityManager, oauth2TokenProvider);

        ImapConnection imapConnection = mock(ImapConnection.class);
        List<ImapResponse> imapResponses = Arrays.asList(
                createImapResponse("* LIST (\\HasNoChildren) \".\" \"pathPrefix/INBOX\""),
                createImapResponse("* LIST (\\Noselect \\HasChildren) \".\" \"pathPrefix/.Folder\""),
                createImapResponse("* LIST (\\HasNoChildren) \".\" \"pathPrefix/.Folder.SubFolder\""),
                createImapResponse("6 OK Success")
        );
        when(imapConnection.executeSimpleCommand("LIST \"\" \"pathPrefix/*\"")).thenReturn(imapResponses);
        imapStore.enqueueImapConnection(imapConnection);

        List<? extends Folder> result = imapStore.getFolders(true);

        assertNotNull(result);
        assertEquals(Sets.newSet("INBOX", "Folder.SubFolder"), extractFolderIds(result));
    }

    @Test
    public void getPersonalNamespaces_withoutForceListAllAndWithoutSubscribedFoldersOnly() throws Exception {
        when(storeConfig.subscribedFoldersOnly()).thenReturn(false);
        ImapConnection imapConnection = mock(ImapConnection.class);
        List<ImapResponse> imapResponses = Arrays.asList(
                createImapResponse("* LIST (\\HasNoChildren) \".\" \"INBOX\""),
                createImapResponse("* LIST (\\Noselect \\HasChildren) \".\" \"Folder\""),
                createImapResponse("* LIST (\\HasNoChildren) \".\" \"Folder.SubFolder\""),
                createImapResponse("6 OK Success")
        );
        when(imapConnection.executeSimpleCommand("LIST \"\" \"*\"")).thenReturn(imapResponses);
        imapStore.enqueueImapConnection(imapConnection);

        List<? extends Folder> result = imapStore.getFolders(false);

        assertNotNull(result);
        assertEquals(Sets.newSet("INBOX", "Folder.SubFolder"), extractFolderIds(result));
    }

    @Test
    public void getPersonalNamespaces_withSubscribedFoldersOnlyAndWithoutForceListAll_shouldOnlyReturnExistingSubscribedFolders()
            throws Exception {
        when(storeConfig.subscribedFoldersOnly()).thenReturn(true);
        ImapConnection imapConnection = mock(ImapConnection.class);
        List<ImapResponse> lsubResponses = Arrays.asList(
                createImapResponse("* LSUB (\\HasNoChildren) \".\" \"INBOX\""),
                createImapResponse("* LSUB (\\Noselect \\HasChildren) \".\" \"Folder\""),
                createImapResponse("* LSUB (\\HasNoChildren) \".\" \"Folder.SubFolder\""),
                createImapResponse("* LSUB (\\HasNoChildren) \".\" \"SubscribedFolderThatHasBeenDeleted\""),
                createImapResponse("5 OK Success")
        );
        when(imapConnection.executeSimpleCommand("LSUB \"\" \"*\"")).thenReturn(lsubResponses);
        List<ImapResponse> imapResponses = Arrays.asList(
                createImapResponse("* LIST (\\HasNoChildren) \".\" \"INBOX\""),
                createImapResponse("* LIST (\\Noselect \\HasChildren) \".\" \"Folder\""),
                createImapResponse("* LIST (\\HasNoChildren) \".\" \"Folder.SubFolder\""),
                createImapResponse("6 OK Success")
        );
        when(imapConnection.executeSimpleCommand("LIST \"\" \"*\"")).thenReturn(imapResponses);
        imapStore.enqueueImapConnection(imapConnection);

        List<? extends Folder> result = imapStore.getFolders(false);

        assertNotNull(result);
        assertEquals(Sets.newSet("INBOX", "Folder.SubFolder"), extractFolderIds(result));
    }

    @Test
    public void getPersonalNamespaces_withoutException_shouldLeaveImapConnectionOpen() throws Exception {
        ImapConnection imapConnection = mock(ImapConnection.class);
        List<ImapResponse> imapResponses = Collections.singletonList(createImapResponse("5 OK Success"));
        when(imapConnection.executeSimpleCommand(anyString())).thenReturn(imapResponses);
        imapStore.enqueueImapConnection(imapConnection);

        imapStore.getFolders(true);

        verify(imapConnection, never()).close();
    }

    @Test
    public void getPersonalNamespaces_withIoException_shouldCloseImapConnection() throws Exception {
        ImapConnection imapConnection = mock(ImapConnection.class);
        doThrow(IOException.class).when(imapConnection).executeSimpleCommand("LIST \"\" \"*\"");
        imapStore.enqueueImapConnection(imapConnection);

        try {
            imapStore.getFolders(true);
            fail("Expected exception");
        } catch (MessagingException ignored) {
        }

        verify(imapConnection).close();
    }

    @Test
    public void getParentId_shouldReturnCorrectValue() {
        String result = imapStore.getParentId("Folder.SubFolder");

        assertEquals("Folder", result);
    }

    @Test
    public void getConnection_shouldCreateImapConnection() throws Exception {
        ImapConnection imapConnection = mock(ImapConnection.class);
        imapStore.enqueueImapConnection(imapConnection);

        ImapConnection result = imapStore.getConnection();

        assertSame(imapConnection, result);
    }

    @Test
    public void getConnection_calledTwiceWithoutRelease_shouldCreateTwoImapConnection() throws Exception {
        ImapConnection imapConnectionOne = mock(ImapConnection.class);
        ImapConnection imapConnectionTwo = mock(ImapConnection.class);
        imapStore.enqueueImapConnection(imapConnectionOne);
        imapStore.enqueueImapConnection(imapConnectionTwo);

        ImapConnection resultOne = imapStore.getConnection();
        ImapConnection resultTwo = imapStore.getConnection();

        assertSame(imapConnectionOne, resultOne);
        assertSame(imapConnectionTwo, resultTwo);
    }

    @Test
    public void getConnection_calledAfterRelease_shouldReturnCachedImapConnection() throws Exception {
        ImapConnection imapConnection = mock(ImapConnection.class);
        when(imapConnection.isConnected()).thenReturn(true);
        imapStore.enqueueImapConnection(imapConnection);
        ImapConnection connection = imapStore.getConnection();
        imapStore.releaseConnection(connection);

        ImapConnection result = imapStore.getConnection();

        assertSame(imapConnection, result);
    }

    @Test
    public void getConnection_calledAfterReleaseWithAClosedConnection_shouldReturnNewImapConnectionInstance()
            throws Exception {
        ImapConnection imapConnectionOne = mock(ImapConnection.class);
        ImapConnection imapConnectionTwo = mock(ImapConnection.class);
        imapStore.enqueueImapConnection(imapConnectionOne);
        imapStore.enqueueImapConnection(imapConnectionTwo);
        imapStore.getConnection();
        when(imapConnectionOne.isConnected()).thenReturn(false);
        imapStore.releaseConnection(imapConnectionOne);

        ImapConnection result = imapStore.getConnection();

        assertSame(imapConnectionTwo, result);
    }

    @Test
    public void getConnection_withDeadConnectionInPool_shouldReturnNewImapConnectionInstance() throws Exception {
        ImapConnection imapConnectionOne = mock(ImapConnection.class);
        ImapConnection imapConnectionTwo = mock(ImapConnection.class);
        imapStore.enqueueImapConnection(imapConnectionOne);
        imapStore.enqueueImapConnection(imapConnectionTwo);
        imapStore.getConnection();
        when(imapConnectionOne.isConnected()).thenReturn(true);
        doThrow(IOException.class).when(imapConnectionOne).executeSimpleCommand(Commands.NOOP);
        imapStore.releaseConnection(imapConnectionOne);

        ImapConnection result = imapStore.getConnection();

        assertSame(imapConnectionTwo, result);
    }

    private StoreConfig createStoreConfig() {
        StoreConfig storeConfig = mock(StoreConfig.class);
        when(storeConfig.getInboxFolderId()).thenReturn("INBOX");
        when(storeConfig.getStoreUri()).thenReturn("imap://user:password@imap.example.org");

        return storeConfig;
    }

    private Set<String> extractFolderIds(List<? extends Folder> folders) {
        Set<String> folderNames = new HashSet<>(folders.size());
        for (Folder folder : folders) {
            folderNames.add(folder.getId());
        }

        return folderNames;
    }

    private static class TestImapStore extends ImapStore {
        private Deque<ImapConnection> imapConnections = new ArrayDeque<>();

        public TestImapStore(StoreConfig storeConfig, TrustedSocketFactory trustedSocketFactory,
                ConnectivityManager connectivityManager, OAuth2TokenProvider oauth2TokenProvider) throws MessagingException {
            super(storeConfig, trustedSocketFactory, connectivityManager, oauth2TokenProvider);
        }

        @Override
        ImapConnection createImapConnection() {
            if (imapConnections.isEmpty()) {
                throw new AssertionError("Unexpectedly tried to create an ImapConnection instance");
            }
            return imapConnections.pop();
        }

        public void enqueueImapConnection(ImapConnection imapConnection) {
            imapConnections.add(imapConnection);
        }
    }
}
