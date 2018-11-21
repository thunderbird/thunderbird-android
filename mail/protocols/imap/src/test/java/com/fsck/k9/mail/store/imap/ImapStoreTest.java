package com.fsck.k9.mail.store.imap;


import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.net.ConnectivityManager;

import com.fsck.k9.mail.AuthType;
import com.fsck.k9.mail.ConnectionSecurity;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.Folder.FolderType;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class ImapStoreTest {
    private StoreConfig storeConfig;
    private TestImapStore imapStore;

    @Before
    public void setUp() throws Exception {
        ImapStoreSettings serverSettings = createImapStoreSettings();
        storeConfig = createStoreConfig();
        TrustedSocketFactory trustedSocketFactory = mock(TrustedSocketFactory.class);
        ConnectivityManager connectivityManager = mock(ConnectivityManager.class);
        OAuth2TokenProvider oauth2TokenProvider = mock(OAuth2TokenProvider.class);

        imapStore = new TestImapStore(serverSettings, storeConfig, trustedSocketFactory, connectivityManager,
                oauth2TokenProvider);
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
    public void getPersonalNamespaces_withSpecialUseCapability_shouldReturnSpecialFolderInfo() throws Exception {
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
        imapStore.enqueueImapConnection(imapConnection);

        List<ImapFolder> folders = imapStore.getPersonalNamespaces();

        Map<String, ImapFolder> folderMap = toFolderMap(folders);
        assertEquals(FolderType.INBOX, folderMap.get("INBOX").getType());
        assertEquals(FolderType.DRAFTS, folderMap.get("[Gmail]/Drafts").getType());
        assertEquals(FolderType.SENT, folderMap.get("[Gmail]/Sent Mail").getType());
        assertEquals(FolderType.SPAM, folderMap.get("[Gmail]/Spam").getType());
        assertEquals(FolderType.TRASH, folderMap.get("[Gmail]/Trash").getType());
        assertEquals(FolderType.ARCHIVE, folderMap.get("[Gmail]/All Mail").getType());
    }

    @Test
    public void getPersonalNamespaces_withoutSpecialUseCapability_shouldUseSimpleListCommand() throws Exception {
        ImapConnection imapConnection = mock(ImapConnection.class);
        when(imapConnection.hasCapability(Capabilities.SPECIAL_USE)).thenReturn(false);
        imapStore.enqueueImapConnection(imapConnection);

        imapStore.getPersonalNamespaces();

        verify(imapConnection, never()).executeSimpleCommand("LIST (SPECIAL-USE) \"\" \"*\"");
        verify(imapConnection).executeSimpleCommand("LIST \"\" \"*\"");
    }

    @Test
    public void getPersonalNamespaces_withoutSubscribedFoldersOnly() throws Exception {
        when(storeConfig.isSubscribedFoldersOnly()).thenReturn(false);
        ImapConnection imapConnection = mock(ImapConnection.class);
        List<ImapResponse> imapResponses = Arrays.asList(
                createImapResponse("* LIST (\\HasNoChildren) \".\" \"INBOX\""),
                createImapResponse("* LIST (\\Noselect \\HasChildren) \".\" \"Folder\""),
                createImapResponse("* LIST (\\HasNoChildren) \".\" \"Folder.SubFolder\""),
                createImapResponse("6 OK Success")
        );
        when(imapConnection.executeSimpleCommand("LIST \"\" \"*\"")).thenReturn(imapResponses);
        imapStore.enqueueImapConnection(imapConnection);

        List<? extends Folder> result = imapStore.getPersonalNamespaces();

        assertNotNull(result);
        assertEquals(Sets.newSet("INBOX", "Folder.SubFolder"), extractFolderNames(result));
    }

    @Test
    public void getPersonalNamespaces_withSubscribedFoldersOnly_shouldOnlyReturnExistingSubscribedFolders()
            throws Exception {
        when(storeConfig.isSubscribedFoldersOnly()).thenReturn(true);
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

        List<? extends Folder> result = imapStore.getPersonalNamespaces();

        assertNotNull(result);
        assertEquals(Sets.newSet("INBOX", "Folder.SubFolder"), extractFolderNames(result));
    }

    @Test
    public void getPersonalNamespaces_withNamespacePrefix_shouldRemoveNamespacePrefix() throws Exception {
        ImapConnection imapConnection = mock(ImapConnection.class);
        List<ImapResponse> imapResponses = Arrays.asList(
                createImapResponse("* LIST () \".\" \"INBOX\""),
                createImapResponse("* LIST () \".\" \"INBOX.FolderOne\""),
                createImapResponse("* LIST () \".\" \"INBOX.FolderTwo\""),
                createImapResponse("5 OK Success")
        );
        when(imapConnection.executeSimpleCommand("LIST \"\" \"INBOX.*\"")).thenReturn(imapResponses);
        imapStore.enqueueImapConnection(imapConnection);
        imapStore.setTestCombinedPrefix("INBOX.");

        List<ImapFolder> result = imapStore.getPersonalNamespaces();

        assertNotNull(result);
        assertEquals(Sets.newSet("INBOX", "FolderOne", "FolderTwo"), extractFolderNames(result));
    }

    @Test
    public void getPersonalNamespaces_withFolderNotMatchingNamespacePrefix_shouldExcludeFolderWithoutPrefix()
            throws Exception {
        ImapConnection imapConnection = mock(ImapConnection.class);
        List<ImapResponse> imapResponses = Arrays.asList(
                createImapResponse("* LIST () \".\" \"INBOX\""),
                createImapResponse("* LIST () \".\" \"INBOX.FolderOne\""),
                createImapResponse("* LIST () \".\" \"FolderTwo\""),
                createImapResponse("5 OK Success")
        );
        when(imapConnection.executeSimpleCommand("LIST \"\" \"INBOX.*\"")).thenReturn(imapResponses);
        imapStore.enqueueImapConnection(imapConnection);
        imapStore.setTestCombinedPrefix("INBOX.");

        List<ImapFolder> result = imapStore.getPersonalNamespaces();

        assertNotNull(result);
        assertEquals(Sets.newSet("INBOX", "FolderOne"), extractFolderNames(result));
    }

    @Test
    public void getPersonalNamespaces_withoutException_shouldLeaveImapConnectionOpen() throws Exception {
        ImapConnection imapConnection = mock(ImapConnection.class);
        List<ImapResponse> imapResponses = Collections.singletonList(createImapResponse("5 OK Success"));
        when(imapConnection.executeSimpleCommand(anyString())).thenReturn(imapResponses);
        imapStore.enqueueImapConnection(imapConnection);

        imapStore.getPersonalNamespaces();

        verify(imapConnection, never()).close();
    }

    @Test
    public void getPersonalNamespaces_withIoException_shouldCloseImapConnection() throws Exception {
        ImapConnection imapConnection = mock(ImapConnection.class);
        doThrow(IOException.class).when(imapConnection).executeSimpleCommand("LIST \"\" \"*\"");
        imapStore.enqueueImapConnection(imapConnection);

        try {
            imapStore.getPersonalNamespaces();
            fail("Expected exception");
        } catch (MessagingException ignored) {
        }

        verify(imapConnection).close();
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


    private ImapStoreSettings createImapStoreSettings() {
        return new ImapStoreSettings(
                "imap.example.org",
                143,
                ConnectionSecurity.NONE,
                AuthType.PLAIN,
                "user",
                "password",
                null,
                true,
                null);
    }

    private StoreConfig createStoreConfig() {
        StoreConfig storeConfig = mock(StoreConfig.class);
        when(storeConfig.getInboxFolder()).thenReturn("INBOX");

        return storeConfig;
    }

    private Set<String> extractFolderNames(List<? extends Folder> folders) {
        Set<String> folderNames = new HashSet<>(folders.size());
        for (Folder folder : folders) {
            folderNames.add(folder.getServerId());
        }

        return folderNames;
    }

    private Map<String, ImapFolder> toFolderMap(List<ImapFolder> folders) {
        Map<String, ImapFolder> folderMap = new HashMap<>();
        for (ImapFolder folder : folders) {
            folderMap.put(folder.getServerId(), folder);
        }

        return folderMap;
    }


    static class TestImapStore extends ImapStore {
        private Deque<ImapConnection> imapConnections = new ArrayDeque<>();
        private String testCombinedPrefix;

        public TestImapStore(ImapStoreSettings serverSettings, StoreConfig storeConfig,
                TrustedSocketFactory trustedSocketFactory, ConnectivityManager connectivityManager,
                OAuth2TokenProvider oauth2TokenProvider) {
            super(serverSettings, storeConfig, trustedSocketFactory, connectivityManager, oauth2TokenProvider);
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

        @Override
        String getCombinedPrefix() {
            return testCombinedPrefix != null ? testCombinedPrefix : super.getCombinedPrefix();
        }

        void setTestCombinedPrefix(String prefix) {
            testCombinedPrefix = prefix;
        }
    }
}
