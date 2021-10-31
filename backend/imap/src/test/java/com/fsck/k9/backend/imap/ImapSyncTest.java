package com.fsck.k9.backend.imap;


import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.fsck.k9.backend.api.BackendFolder;
import com.fsck.k9.backend.api.BackendStorage;
import com.fsck.k9.backend.api.SyncConfig;
import com.fsck.k9.backend.api.SyncConfig.ExpungePolicy;
import com.fsck.k9.backend.api.SyncListener;
import com.fsck.k9.mail.FetchProfile;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.MessageRetrievalListener;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.store.imap.ImapFolder;
import com.fsck.k9.mail.store.imap.ImapMessage;
import com.fsck.k9.mail.store.imap.ImapStore;
import com.fsck.k9.mail.store.imap.OpenMode;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@SuppressWarnings("unchecked")
public class ImapSyncTest {
    private static final String EXTRA_UID_VALIDITY = "imapUidValidity";
    private static final String ACCOUNT_NAME = "Account";
    private static final String FOLDER_NAME = "Folder";
    private static final Long FOLDER_UID_VALIDITY = 42L;
    private static final int MAXIMUM_SMALL_MESSAGE_SIZE = 1000;
    private static final String MESSAGE_UID1 = "message-uid1";
    private static final int DEFAULT_VISIBLE_LIMIT = 25;
    private static final Set<Flag> SYNC_FLAGS = EnumSet.of(Flag.SEEN, Flag.FLAGGED, Flag.ANSWERED, Flag.FORWARDED);


    private ImapSync imapSync;
    @Mock
    private SyncListener listener;
    @Mock
    private ImapFolder remoteFolder;
    @Mock
    private BackendStorage backendStorage;
    @Mock
    private BackendFolder backendFolder;
    @Mock
    private ImapStore remoteStore;
    @Captor
    private ArgumentCaptor<List<String>> messageListCaptor;
    @Captor
    private ArgumentCaptor<FetchProfile> fetchProfileCaptor;

    private SyncConfig syncConfig;


    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        imapSync = new ImapSync(ACCOUNT_NAME, backendStorage, remoteStore);

        configureSyncConfig();
        configureBackendStorage();
        configureRemoteStoreWithFolder();
    }

    @Test
    public void sync_withOneMessageInRemoteFolder_shouldFinishWithoutError() {
        messageCountInRemoteFolder(1);

        imapSync.sync(FOLDER_NAME, syncConfig, listener);

        verify(listener).syncFinished(FOLDER_NAME);
    }

    @Test
    public void sync_withEmptyRemoteFolder_shouldFinishWithoutError() {
        messageCountInRemoteFolder(0);

        imapSync.sync(FOLDER_NAME, syncConfig, listener);

        verify(listener).syncFinished(FOLDER_NAME);
    }

    @Test
    public void sync_withNegativeMessageCountInRemoteFolder_shouldFinishWithError() {
        messageCountInRemoteFolder(-1);

        imapSync.sync(FOLDER_NAME, syncConfig, listener);

        verify(listener).syncFailed(eq(FOLDER_NAME), eq("Exception: Message count -1 for folder Folder"),
                any(Exception.class));
    }

    @Test
    public void sync_shouldOpenRemoteFolder() throws Exception {
        messageCountInRemoteFolder(1);

        imapSync.sync(FOLDER_NAME, syncConfig, listener);

        verify(remoteFolder).open(OpenMode.READ_ONLY);
    }

    @Test
    public void sync_shouldCloseRemoteFolder() {
        messageCountInRemoteFolder(1);

        imapSync.sync(FOLDER_NAME, syncConfig, listener);

        verify(remoteFolder).close();
    }

    @Test
    public void sync_withAccountPolicySetToExpungeOnPoll_shouldExpungeRemoteFolder() throws Exception {
        messageCountInRemoteFolder(1);
        configureSyncConfigWithExpungePolicy(ExpungePolicy.ON_POLL);

        imapSync.sync(FOLDER_NAME, syncConfig, listener);

        verify(remoteFolder).expunge();
    }

    @Test
    public void sync_withAccountPolicySetToExpungeManually_shouldNotExpungeRemoteFolder() throws Exception {
        messageCountInRemoteFolder(1);
        configureSyncConfigWithExpungePolicy(ExpungePolicy.MANUALLY);

        imapSync.sync(FOLDER_NAME, syncConfig, listener);

        verify(remoteFolder, never()).expunge();
    }

    @Test
    public void sync_withAccountSetToSyncRemoteDeletions_shouldDeleteLocalCopiesOfDeletedMessages() {
        messageCountInRemoteFolder(0);
        configureSyncConfigWithSyncRemoteDeletions(true);
        when(backendFolder.getAllMessagesAndEffectiveDates()).thenReturn(Collections.singletonMap(MESSAGE_UID1, 0L));

        imapSync.sync(FOLDER_NAME, syncConfig, listener);

        verify(backendFolder).destroyMessages(messageListCaptor.capture());
        assertEquals(MESSAGE_UID1, messageListCaptor.getValue().get(0));
    }

    @Test
    public void sync_withAccountSetToSyncRemoteDeletions_shouldNotDeleteLocalCopiesOfExistingMessagesAfterEarliestPollDate()
            throws Exception {
        messageCountInRemoteFolder(1);
        Date dateOfEarliestPoll = new Date();
        ImapMessage remoteMessage = messageOnServer();
        configureSyncConfigWithSyncRemoteDeletionsAndEarliestPollDate(dateOfEarliestPoll);
        when(remoteMessage.olderThan(dateOfEarliestPoll)).thenReturn(false);

        imapSync.sync(FOLDER_NAME, syncConfig, listener);

        verify(backendFolder, never()).destroyMessages(messageListCaptor.capture());
    }

    @Test
    public void sync_withAccountSetToSyncRemoteDeletions_shouldDeleteLocalCopiesOfExistingMessagesBeforeEarliestPollDate()
            throws Exception {
        messageCountInRemoteFolder(1);
        ImapMessage remoteMessage = messageOnServer();
        Date dateOfEarliestPoll = new Date();
        configureSyncConfigWithSyncRemoteDeletionsAndEarliestPollDate(dateOfEarliestPoll);
        when(remoteMessage.olderThan(dateOfEarliestPoll)).thenReturn(true);
        when(backendFolder.getAllMessagesAndEffectiveDates()).thenReturn(Collections.singletonMap(MESSAGE_UID1, 0L));

        imapSync.sync(FOLDER_NAME, syncConfig, listener);

        verify(backendFolder).destroyMessages(messageListCaptor.capture());
        assertEquals(MESSAGE_UID1, messageListCaptor.getValue().get(0));
    }

    @Test
    public void sync_withAccountSetNotToSyncRemoteDeletions_shouldNotDeleteLocalCopiesOfMessages() {
        messageCountInRemoteFolder(0);
        configureSyncConfigWithSyncRemoteDeletions(false);

        imapSync.sync(FOLDER_NAME, syncConfig, listener);

        verify(backendFolder, never()).destroyMessages(messageListCaptor.capture());
    }

    @Test
    public void sync_shouldFetchUnsynchronizedMessagesListAndFlags() throws Exception {
        messageCountInRemoteFolder(1);
        hasUnsyncedRemoteMessage();

        imapSync.sync(FOLDER_NAME, syncConfig, listener);

        verify(remoteFolder, atLeastOnce()).fetch(any(List.class), fetchProfileCaptor.capture(),
                nullable(MessageRetrievalListener.class), anyInt());
        assertTrue(fetchProfileCaptor.getAllValues().get(0).contains(FetchProfile.Item.FLAGS));
        assertTrue(fetchProfileCaptor.getAllValues().get(0).contains(FetchProfile.Item.ENVELOPE));
        assertEquals(2, fetchProfileCaptor.getAllValues().get(0).size());
    }

    @Test
    public void sync_withUnsyncedNewSmallMessage_shouldFetchBodyOfSmallMessage() throws Exception {
        ImapMessage smallMessage = buildSmallNewMessage();
        messageCountInRemoteFolder(1);
        hasUnsyncedRemoteMessage();
        respondToFetchEnvelopesWithMessage(smallMessage);

        imapSync.sync(FOLDER_NAME, syncConfig, listener);

        verify(remoteFolder, atLeast(2)).fetch(any(List.class), fetchProfileCaptor.capture(),
                nullable(MessageRetrievalListener.class), anyInt());
        assertEquals(1, fetchProfileCaptor.getAllValues().get(1).size());
        assertTrue(fetchProfileCaptor.getAllValues().get(1).contains(FetchProfile.Item.BODY));
    }

    @Test
    public void sync_withUnsyncedNewSmallMessage_shouldFetchStructureAndLimitedBodyOfLargeMessage() throws Exception {
        ImapMessage largeMessage = buildLargeNewMessage();
        messageCountInRemoteFolder(1);
        hasUnsyncedRemoteMessage();
        respondToFetchEnvelopesWithMessage(largeMessage);

        imapSync.sync(FOLDER_NAME, syncConfig, listener);

        //TODO: Don't bother fetching messages of a size we don't have
        verify(remoteFolder, atLeast(4)).fetch(any(List.class), fetchProfileCaptor.capture(),
                nullable(MessageRetrievalListener.class), anyInt());
        assertEquals(1, fetchProfileCaptor.getAllValues().get(2).size());
        assertEquals(FetchProfile.Item.STRUCTURE, fetchProfileCaptor.getAllValues().get(2).get(0));
        assertEquals(1, fetchProfileCaptor.getAllValues().get(3).size());
        assertEquals(FetchProfile.Item.BODY_SANE, fetchProfileCaptor.getAllValues().get(3).get(0));
    }

    @Test
    public void sync_withUidValidityChange_shouldClearAllMessages() {
        when(backendFolder.getFolderExtraNumber(EXTRA_UID_VALIDITY)).thenReturn(23L);

        imapSync.sync(FOLDER_NAME, syncConfig, listener);

        verify(backendFolder).clearAllMessages();
        verify(backendFolder).setFolderExtraNumber(EXTRA_UID_VALIDITY, FOLDER_UID_VALIDITY);
    }

    @Test
    public void sync_withoutUidValidityChange_shouldNotClearAllMessages() {
        when(backendFolder.getFolderExtraNumber(EXTRA_UID_VALIDITY)).thenReturn(FOLDER_UID_VALIDITY);

        imapSync.sync(FOLDER_NAME, syncConfig, listener);

        verify(backendFolder, never()).clearAllMessages();
    }

    @Test
    public void sync_withFirstUidValidityValue_shouldNotClearAllMessages() {
        when(backendFolder.getFolderExtraNumber(EXTRA_UID_VALIDITY)).thenReturn(null);

        imapSync.sync(FOLDER_NAME, syncConfig, listener);

        verify(backendFolder, never()).clearAllMessages();
        verify(backendFolder).setFolderExtraNumber(EXTRA_UID_VALIDITY, FOLDER_UID_VALIDITY);
    }

    private void respondToFetchEnvelopesWithMessage(final ImapMessage message) throws MessagingException {
        doAnswer(new Answer() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                FetchProfile fetchProfile = (FetchProfile) invocation.getArguments()[1];
                if (invocation.getArguments()[2] != null) {
                    MessageRetrievalListener listener = (MessageRetrievalListener) invocation.getArguments()[2];
                    if (fetchProfile.contains(FetchProfile.Item.ENVELOPE)) {
                        listener.messageStarted("UID", 1, 1);
                        listener.messageFinished(message, 1, 1);
                        listener.messagesFinished(1);
                    }
                }
                return null;
            }
        }).when(remoteFolder).fetch(any(List.class), any(FetchProfile.class), nullable(MessageRetrievalListener.class),
                anyInt());
    }

    private ImapMessage buildSmallNewMessage() {
        ImapMessage message = mock(ImapMessage.class);
        when(message.olderThan(nullable(Date.class))).thenReturn(false);
        when(message.getSize()).thenReturn((long) MAXIMUM_SMALL_MESSAGE_SIZE);
        return message;
    }

    private ImapMessage buildLargeNewMessage() {
        ImapMessage message = mock(ImapMessage.class);
        when(message.olderThan(nullable(Date.class))).thenReturn(false);
        when(message.getSize()).thenReturn((long) (MAXIMUM_SMALL_MESSAGE_SIZE + 1));
        return message;
    }

    private void messageCountInRemoteFolder(int value) {
        when(remoteFolder.getMessageCount()).thenReturn(value);
    }

    private ImapMessage messageOnServer() throws MessagingException {
        String messageUid = "UID";
        ImapMessage remoteMessage = mock(ImapMessage.class);

        when(remoteMessage.getUid()).thenReturn(messageUid);
        when(remoteFolder.getMessages(anyInt(), anyInt(), nullable(Date.class),
                nullable(MessageRetrievalListener.class))).thenReturn(Collections.singletonList(remoteMessage));
        return remoteMessage;
    }

    private void hasUnsyncedRemoteMessage() throws MessagingException {
        String messageUid = "UID";
        ImapMessage remoteMessage = mock(ImapMessage.class);
        when(remoteMessage.getUid()).thenReturn(messageUid);
        when(remoteFolder.getMessages(anyInt(), anyInt(), nullable(Date.class),
                nullable(MessageRetrievalListener.class))).thenReturn(Collections.singletonList(remoteMessage));
    }

    private void configureSyncConfig() {
        syncConfig = new SyncConfig(
                ExpungePolicy.MANUALLY,
                null,
                true,
                MAXIMUM_SMALL_MESSAGE_SIZE,
                DEFAULT_VISIBLE_LIMIT,
                SYNC_FLAGS);
    }

    private void configureRemoteStoreWithFolder() {
        when(remoteStore.getFolder(FOLDER_NAME)).thenReturn(remoteFolder);
        when(remoteFolder.getServerId()).thenReturn(FOLDER_NAME);
        when(remoteFolder.getUidValidity()).thenReturn(FOLDER_UID_VALIDITY);
    }

    private void configureBackendStorage() {
        when(backendStorage.getFolder(FOLDER_NAME)).thenReturn(backendFolder);
    }

    private void configureSyncConfigWithExpungePolicy(ExpungePolicy expungePolicy) {
        syncConfig = syncConfig.copy(
                expungePolicy,
                syncConfig.getEarliestPollDate(),
                syncConfig.getSyncRemoteDeletions(),
                syncConfig.getMaximumAutoDownloadMessageSize(),
                syncConfig.getDefaultVisibleLimit(),
                syncConfig.getSyncFlags());
    }

    private void configureSyncConfigWithSyncRemoteDeletions(boolean syncRemoteDeletions) {
        syncConfig = syncConfig.copy(
                syncConfig.getExpungePolicy(),
                syncConfig.getEarliestPollDate(),
                syncRemoteDeletions,
                syncConfig.getMaximumAutoDownloadMessageSize(),
                syncConfig.getDefaultVisibleLimit(),
                syncConfig.getSyncFlags());
    }

    private void configureSyncConfigWithSyncRemoteDeletionsAndEarliestPollDate(Date earliestPollDate) {
        syncConfig = syncConfig.copy(
                syncConfig.getExpungePolicy(),
                earliestPollDate,
                true,
                syncConfig.getMaximumAutoDownloadMessageSize(),
                syncConfig.getDefaultVisibleLimit(),
                syncConfig.getSyncFlags());
    }
}
