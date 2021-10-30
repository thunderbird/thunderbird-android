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
import com.fsck.k9.mail.FolderType;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.ServerSettings;
import com.fsck.k9.mail.oauth.OAuth2TokenProvider;
import com.fsck.k9.mail.ssl.TrustedSocketFactory;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.util.collections.Sets;

import static com.fsck.k9.mail.store.imap.ImapResponseHelper.createImapResponse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class RealImapStoreTest {
    private ImapStoreConfig config = mock(ImapStoreConfig.class);
    private TestImapStore imapStore;

    @Before
    public void setUp() throws Exception {
        ServerSettings serverSettings = createServerSettings();
        TrustedSocketFactory trustedSocketFactory = mock(TrustedSocketFactory.class);
        ConnectivityManager connectivityManager = mock(ConnectivityManager.class);
        OAuth2TokenProvider oauth2TokenProvider = mock(OAuth2TokenProvider.class);

        imapStore = new TestImapStore(serverSettings, config, trustedSocketFactory, connectivityManager,
                oauth2TokenProvider);
    }

    @Test
    public void checkSettings_shouldCreateImapConnectionAndCallOpen() throws Exception {
        ImapConnection imapConnection = createMockConnection();
        imapStore.enqueueImapConnection(imapConnection);

        imapStore.checkSettings();

        verify(imapConnection).open();
    }

    @Test
    public void checkSettings_withOpenThrowing_shouldThrowMessagingException() throws Exception {
        ImapConnection imapConnection = createMockConnection();
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
    public void getFolders_withSpecialUseCapability_shouldReturnSpecialFolderInfo() throws Exception {
        ImapConnection imapConnection = createMockConnection();
        when(imapConnection.hasCapability(Capabilities.LIST_EXTENDED)).thenReturn(true);
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
        when(imapConnection.executeSimpleCommand("LIST \"\" \"*\" RETURN (SPECIAL-USE)")).thenReturn(imapResponses);
        imapStore.enqueueImapConnection(imapConnection);

        List<FolderListItem> folders = imapStore.getFolders();

        Map<String, FolderListItem> folderMap = toFolderMap(folders);
        assertEquals(FolderType.INBOX, folderMap.get("INBOX").getType());
        assertEquals(FolderType.DRAFTS, folderMap.get("[Gmail]/Drafts").getType());
        assertEquals(FolderType.SENT, folderMap.get("[Gmail]/Sent Mail").getType());
        assertEquals(FolderType.SPAM, folderMap.get("[Gmail]/Spam").getType());
        assertEquals(FolderType.TRASH, folderMap.get("[Gmail]/Trash").getType());
        assertEquals(FolderType.ARCHIVE, folderMap.get("[Gmail]/All Mail").getType());
    }

    @Test
    public void getFolders_withoutSpecialUseCapability_shouldUseSimpleListCommand() throws Exception {
        ImapConnection imapConnection = createMockConnection();
        when(imapConnection.hasCapability(Capabilities.LIST_EXTENDED)).thenReturn(true);
        when(imapConnection.hasCapability(Capabilities.SPECIAL_USE)).thenReturn(false);
        imapStore.enqueueImapConnection(imapConnection);

        imapStore.getFolders();

        verify(imapConnection, never()).executeSimpleCommand("LIST \"\" \"*\" RETURN (SPECIAL-USE)");
        verify(imapConnection).executeSimpleCommand("LIST \"\" \"*\"");
    }

    @Test
    public void getFolders_withoutListExtendedCapability_shouldUseSimpleListCommand() throws Exception {
        ImapConnection imapConnection = createMockConnection();
        when(imapConnection.hasCapability(Capabilities.LIST_EXTENDED)).thenReturn(false);
        when(imapConnection.hasCapability(Capabilities.SPECIAL_USE)).thenReturn(true);
        imapStore.enqueueImapConnection(imapConnection);

        imapStore.getFolders();

        verify(imapConnection, never()).executeSimpleCommand("LIST \"\" \"*\" RETURN (SPECIAL-USE)");
        verify(imapConnection).executeSimpleCommand("LIST \"\" \"*\"");
    }

    @Test
    public void getFolders_withoutSubscribedFoldersOnly() throws Exception {
        when(config.isSubscribedFoldersOnly()).thenReturn(false);
        ImapConnection imapConnection = createMockConnection();
        List<ImapResponse> imapResponses = Arrays.asList(
                createImapResponse("* LIST (\\HasNoChildren) \".\" \"INBOX\""),
                createImapResponse("* LIST (\\Noselect \\HasChildren) \".\" \"Folder\""),
                createImapResponse("* LIST (\\HasNoChildren) \".\" \"Folder.SubFolder\""),
                createImapResponse("6 OK Success")
        );
        when(imapConnection.executeSimpleCommand("LIST \"\" \"*\"")).thenReturn(imapResponses);
        imapStore.enqueueImapConnection(imapConnection);

        List<FolderListItem> result = imapStore.getFolders();

        assertNotNull(result);
        assertEquals(Sets.newSet("INBOX", "Folder.SubFolder"), extractFolderServerIds(result));
    }

    @Test
    public void getFolders_withSubscribedFoldersOnly_shouldOnlyReturnExistingSubscribedFolders()
            throws Exception {
        when(config.isSubscribedFoldersOnly()).thenReturn(true);
        ImapConnection imapConnection = createMockConnection();
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

        List<FolderListItem> result = imapStore.getFolders();

        assertNotNull(result);
        assertEquals(Sets.newSet("INBOX", "Folder.SubFolder"), extractFolderServerIds(result));
    }

    @Test
    public void getFolders_withNamespacePrefix() throws Exception {
        ImapConnection imapConnection = createMockConnection();
        List<ImapResponse> imapResponses = Arrays.asList(
                createImapResponse("* LIST () \".\" \"INBOX\""),
                createImapResponse("* LIST () \".\" \"INBOX.FolderOne\""),
                createImapResponse("* LIST () \".\" \"INBOX.FolderTwo\""),
                createImapResponse("5 OK Success")
        );
        when(imapConnection.executeSimpleCommand("LIST \"\" \"INBOX.*\"")).thenReturn(imapResponses);
        imapStore.enqueueImapConnection(imapConnection);
        imapStore.setTestCombinedPrefix("INBOX.");

        List<FolderListItem> result = imapStore.getFolders();

        assertNotNull(result);
        assertEquals(Sets.newSet("INBOX", "INBOX.FolderOne", "INBOX.FolderTwo"), extractFolderServerIds(result));
        assertEquals(Sets.newSet("INBOX", "FolderOne", "FolderTwo"), extractFolderNames(result));
        assertEquals(Sets.newSet("INBOX", "FolderOne", "FolderTwo"), extractOldFolderServerIds(result));
    }

    @Test
    public void getFolders_withFolderNotMatchingNamespacePrefix() throws Exception {
        ImapConnection imapConnection = createMockConnection();
        List<ImapResponse> imapResponses = Arrays.asList(
                createImapResponse("* LIST () \".\" \"INBOX\""),
                createImapResponse("* LIST () \".\" \"INBOX.FolderOne\""),
                createImapResponse("* LIST () \".\" \"FolderTwo\""),
                createImapResponse("5 OK Success")
        );
        when(imapConnection.executeSimpleCommand("LIST \"\" \"INBOX.*\"")).thenReturn(imapResponses);
        imapStore.enqueueImapConnection(imapConnection);
        imapStore.setTestCombinedPrefix("INBOX.");

        List<FolderListItem> result = imapStore.getFolders();

        assertNotNull(result);
        assertEquals(Sets.newSet("INBOX", "INBOX.FolderOne", "FolderTwo"), extractFolderServerIds(result));
        assertEquals(Sets.newSet("INBOX", "FolderOne", "FolderTwo"), extractFolderNames(result));
        assertEquals(Sets.newSet("INBOX", "FolderOne"), extractOldFolderServerIds(result));
    }

    @Test
    public void getFolders_withDuplicateFolderNames_shouldRemoveDuplicatesAndKeepFolderType()
            throws Exception {
        ImapConnection imapConnection = createMockConnection();
        when(imapConnection.hasCapability(Capabilities.LIST_EXTENDED)).thenReturn(true);
        when(imapConnection.hasCapability(Capabilities.SPECIAL_USE)).thenReturn(true);
        List<ImapResponse> imapResponses = Arrays.asList(
                createImapResponse("* LIST () \".\" \"INBOX\""),
                createImapResponse("* LIST (\\HasNoChildren) \".\" \"Junk\""),
                createImapResponse("* LIST (\\Junk) \".\" \"Junk\""),
                createImapResponse("* LIST (\\HasNoChildren) \".\" \"Junk\""),
                createImapResponse("5 OK Success")
        );
        when(imapConnection.executeSimpleCommand("LIST \"\" \"*\" RETURN (SPECIAL-USE)")).thenReturn(imapResponses);
        imapStore.enqueueImapConnection(imapConnection);

        List<FolderListItem> result = imapStore.getFolders();

        assertNotNull(result);
        assertEquals(2, result.size());
        FolderListItem junkFolder = getFolderByServerId(result, "Junk");
        assertNotNull(junkFolder);
        assertEquals(FolderType.SPAM, junkFolder.getType());
    }

    @Test
    public void getFolders_withoutException_shouldLeaveImapConnectionOpen() throws Exception {
        ImapConnection imapConnection = createMockConnection();
        List<ImapResponse> imapResponses = Collections.singletonList(createImapResponse("5 OK Success"));
        when(imapConnection.executeSimpleCommand(anyString())).thenReturn(imapResponses);
        imapStore.enqueueImapConnection(imapConnection);

        imapStore.getFolders();

        verify(imapConnection, never()).close();
    }

    @Test
    public void getFolders_withIoException_shouldCloseImapConnection() throws Exception {
        ImapConnection imapConnection = createMockConnection();
        doThrow(IOException.class).when(imapConnection).executeSimpleCommand("LIST \"\" \"*\"");
        imapStore.enqueueImapConnection(imapConnection);

        try {
            imapStore.getFolders();
            fail("Expected exception");
        } catch (MessagingException ignored) {
        }

        verify(imapConnection).close();
    }

    @Test
    public void getConnection_shouldCreateImapConnection() throws Exception {
        ImapConnection imapConnection = createMockConnection();
        imapStore.enqueueImapConnection(imapConnection);

        ImapConnection result = imapStore.getConnection();

        assertSame(imapConnection, result);
    }

    @Test
    public void getConnection_calledTwiceWithoutRelease_shouldCreateTwoImapConnection() throws Exception {
        ImapConnection imapConnectionOne = createMockConnection();
        ImapConnection imapConnectionTwo = createMockConnection();
        imapStore.enqueueImapConnection(imapConnectionOne);
        imapStore.enqueueImapConnection(imapConnectionTwo);

        ImapConnection resultOne = imapStore.getConnection();
        ImapConnection resultTwo = imapStore.getConnection();

        assertSame(imapConnectionOne, resultOne);
        assertSame(imapConnectionTwo, resultTwo);
    }

    @Test
    public void getConnection_calledAfterRelease_shouldReturnCachedImapConnection() throws Exception {
        ImapConnection imapConnection = createMockConnection();
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
        ImapConnection imapConnectionOne = createMockConnection();
        ImapConnection imapConnectionTwo = createMockConnection();
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
        ImapConnection imapConnectionOne = createMockConnection();
        ImapConnection imapConnectionTwo = createMockConnection();
        imapStore.enqueueImapConnection(imapConnectionOne);
        imapStore.enqueueImapConnection(imapConnectionTwo);
        imapStore.getConnection();
        when(imapConnectionOne.isConnected()).thenReturn(true);
        doThrow(IOException.class).when(imapConnectionOne).executeSimpleCommand(Commands.NOOP);
        imapStore.releaseConnection(imapConnectionOne);

        ImapConnection result = imapStore.getConnection();

        assertSame(imapConnectionTwo, result);
    }

    @Test
    public void getConnection_withConnectionInPoolAndCloseAllConnections_shouldReturnNewImapConnectionInstance()
            throws Exception {
        ImapConnection imapConnectionOne = createMockConnection(1);
        ImapConnection imapConnectionTwo = createMockConnection(2);
        imapStore.enqueueImapConnection(imapConnectionOne);
        imapStore.enqueueImapConnection(imapConnectionTwo);
        imapStore.getConnection();
        when(imapConnectionOne.isConnected()).thenReturn(true);
        imapStore.releaseConnection(imapConnectionOne);
        imapStore.closeAllConnections();

        ImapConnection result = imapStore.getConnection();

        assertSame(imapConnectionTwo, result);
    }

    @Test
    public void getConnection_withConnectionOutsideOfPoolAndCloseAllConnections_shouldReturnNewImapConnectionInstance()
            throws Exception {
        ImapConnection imapConnectionOne = createMockConnection(1);
        ImapConnection imapConnectionTwo = createMockConnection(2);
        imapStore.enqueueImapConnection(imapConnectionOne);
        imapStore.enqueueImapConnection(imapConnectionTwo);
        imapStore.getConnection();
        when(imapConnectionOne.isConnected()).thenReturn(true);
        imapStore.closeAllConnections();
        imapStore.releaseConnection(imapConnectionOne);

        ImapConnection result = imapStore.getConnection();

        assertSame(imapConnectionTwo, result);
    }


    private ImapConnection createMockConnection() {
        ImapConnection imapConnection = mock(ImapConnection.class);
        when(imapConnection.getConnectionGeneration()).thenReturn(1);
        return imapConnection;
    }

    private ImapConnection createMockConnection(int connectionGeneration) {
        ImapConnection imapConnection = mock(ImapConnection.class);
        when(imapConnection.getConnectionGeneration()).thenReturn(connectionGeneration);
        return imapConnection;
    }


    private ServerSettings createServerSettings() {
        Map<String, String> extra = ImapStoreSettings.createExtra(true, null);
        return new ServerSettings(
                "imap",
                "imap.example.org",
                143,
                ConnectionSecurity.NONE,
                AuthType.PLAIN,
                "user",
                "password",
                null,
                extra);
    }

    private Set<String> extractFolderServerIds(List<FolderListItem> folders) {
        Set<String> folderServerIds = new HashSet<>(folders.size());
        for (FolderListItem folder : folders) {
            folderServerIds.add(folder.getServerId());
        }

        return folderServerIds;
    }

    private Set<String> extractFolderNames(List<FolderListItem> folders) {
        Set<String> folderNames = new HashSet<>(folders.size());
        for (FolderListItem folder : folders) {
            folderNames.add(folder.getName());
        }

        return folderNames;
    }

    private Set<String> extractOldFolderServerIds(List<FolderListItem> folders) {
        Set<String> folderNames = new HashSet<>(folders.size());
        for (FolderListItem folder : folders) {
            String oldServerId = folder.getOldServerId();
            if (oldServerId != null) {
                folderNames.add(oldServerId);
            }
        }

        return folderNames;
    }

    private FolderListItem getFolderByServerId(List<FolderListItem> result, String serverId) {
        for (FolderListItem imapFolder : result) {
            if (imapFolder.getServerId().equals(serverId)) {
                return imapFolder;
            }
        }
        return null;
    }

    private Map<String, FolderListItem> toFolderMap(List<FolderListItem> folders) {
        Map<String, FolderListItem> folderMap = new HashMap<>();
        for (FolderListItem folder : folders) {
            folderMap.put(folder.getServerId(), folder);
        }

        return folderMap;
    }


    static class TestImapStore extends RealImapStore {
        private Deque<ImapConnection> imapConnections = new ArrayDeque<>();
        private String testCombinedPrefix;

        public TestImapStore(ServerSettings serverSettings, ImapStoreConfig config,
                TrustedSocketFactory trustedSocketFactory, ConnectivityManager connectivityManager,
                OAuth2TokenProvider oauth2TokenProvider) {
            super(serverSettings, config, trustedSocketFactory, connectivityManager, oauth2TokenProvider);
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
        @NotNull
        public String getCombinedPrefix() {
            return testCombinedPrefix != null ? testCombinedPrefix : super.getCombinedPrefix();
        }

        void setTestCombinedPrefix(String prefix) {
            testCombinedPrefix = prefix;
        }
    }
}
