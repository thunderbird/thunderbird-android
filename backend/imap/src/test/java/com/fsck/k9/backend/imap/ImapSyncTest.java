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
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessageRetrievalListener;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.store.imap.ImapFolder;
import com.fsck.k9.mail.store.imap.ImapStore;
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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@SuppressWarnings("unchecked")
public class ImapSyncTest {
    private static final String ACCOUNT_NAME = "Account";
    private static final String FOLDER_NAME = "Folder";
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
    }

    @Test
    public void sync_withOneMessageInRemoteFolder_shouldFinishWithoutError() {
        messageCountInRemoteFolder(1);

        imapSync.sync(FOLDER_NAME, syncConfig, listener, remoteFolder);

        verify(listener).syncFinished(FOLDER_NAME, 1, 0);
    }

    @Test
    public void sync_withEmptyRemoteFolder_shouldFinishWithoutError() {
        messageCountInRemoteFolder(0);

        imapSync.sync(FOLDER_NAME, syncConfig, listener, remoteFolder);

        verify(listener).syncFinished(FOLDER_NAME, 0, 0);
    }

    @Test
    public void sync_withNegativeMessageCountInRemoteFolder_shouldFinishWithError() {
        messageCountInRemoteFolder(-1);

        imapSync.sync(FOLDER_NAME, syncConfig, listener, remoteFolder);

        verify(listener).syncFailed(eq(FOLDER_NAME), eq("Exception: Message count -1 for folder Folder"),
                any(Exception.class));
    }

    @Test
    public void sync_withRemoteFolderProvided_shouldNotOpenRemoteFolder() throws Exception {
        messageCountInRemoteFolder(1);

        imapSync.sync(FOLDER_NAME, syncConfig, listener, remoteFolder);

        verify(remoteFolder, never()).open(Folder.OPEN_MODE_RW);
    }

    @Test
    public void sync_withNoRemoteFolderProvided_shouldOpenRemoteFolderFromStore() throws Exception {
        messageCountInRemoteFolder(1);
        configureRemoteStoreWithFolder();

        imapSync.sync(FOLDER_NAME, syncConfig, listener, null);

        verify(remoteFolder).open(Folder.OPEN_MODE_RO);
    }

    @Test
    public void sync_withRemoteFolderProvided_shouldNotCloseRemoteFolder() {
        messageCountInRemoteFolder(1);

        imapSync.sync(FOLDER_NAME, syncConfig, listener, remoteFolder);

        verify(remoteFolder, never()).close();
    }

    @Test
    public void sync_withNoRemoteFolderProvided_shouldCloseRemoteFolderFromStore() {
        messageCountInRemoteFolder(1);
        configureRemoteStoreWithFolder();

        imapSync.sync(FOLDER_NAME, syncConfig, listener, null);

        verify(remoteFolder).close();
    }

    @Test
    public void sync_withAccountPolicySetToExpungeOnPoll_shouldExpungeRemoteFolder() throws Exception {
        messageCountInRemoteFolder(1);
        configureSyncConfigWithExpungePolicy(ExpungePolicy.ON_POLL);
        configureRemoteStoreWithFolder();

        imapSync.sync(FOLDER_NAME, syncConfig, listener, null);

        verify(remoteFolder).expunge();
    }

    @Test
    public void sync_withAccountPolicySetToExpungeManually_shouldNotExpungeRemoteFolder() throws Exception {
        messageCountInRemoteFolder(1);
        configureSyncConfigWithExpungePolicy(ExpungePolicy.MANUALLY);

        imapSync.sync(FOLDER_NAME, syncConfig, listener, null);

        verify(remoteFolder, never()).expunge();
    }

    @Test
    public void sync_withAccountSetToSyncRemoteDeletions_shouldDeleteLocalCopiesOfDeletedMessages() {
        messageCountInRemoteFolder(0);
        configureSyncConfigWithSyncRemoteDeletions(true);
        when(backendFolder.getAllMessagesAndEffectiveDates()).thenReturn(Collections.singletonMap(MESSAGE_UID1, 0L));

        imapSync.sync(FOLDER_NAME, syncConfig, listener, remoteFolder);

        verify(backendFolder).destroyMessages(messageListCaptor.capture());
        assertEquals(MESSAGE_UID1, messageListCaptor.getValue().get(0));
    }

    @Test
    public void sync_withAccountSetToSyncRemoteDeletions_shouldNotDeleteLocalCopiesOfExistingMessagesAfterEarliestPollDate()
            throws Exception {
        messageCountInRemoteFolder(1);
        Date dateOfEarliestPoll = new Date();
        Message remoteMessage = messageOnServer();
        configureSyncConfigWithSyncRemoteDeletionsAndEarliestPollDate(dateOfEarliestPoll);
        when(remoteMessage.olderThan(dateOfEarliestPoll)).thenReturn(false);

        imapSync.sync(FOLDER_NAME, syncConfig, listener, remoteFolder);

        verify(backendFolder, never()).destroyMessages(messageListCaptor.capture());
    }

    @Test
    public void sync_withAccountSetToSyncRemoteDeletions_shouldDeleteLocalCopiesOfExistingMessagesBeforeEarliestPollDate()
            throws Exception {
        messageCountInRemoteFolder(1);
        Message remoteMessage = messageOnServer();
        Date dateOfEarliestPoll = new Date();
        configureSyncConfigWithSyncRemoteDeletionsAndEarliestPollDate(dateOfEarliestPoll);
        when(remoteMessage.olderThan(dateOfEarliestPoll)).thenReturn(true);
        when(backendFolder.getAllMessagesAndEffectiveDates()).thenReturn(Collections.singletonMap(MESSAGE_UID1, 0L));

        imapSync.sync(FOLDER_NAME, syncConfig, listener, remoteFolder);

        verify(backendFolder).destroyMessages(messageListCaptor.capture());
        assertEquals(MESSAGE_UID1, messageListCaptor.getValue().get(0));
    }

    @Test
    public void sync_withAccountSetNotToSyncRemoteDeletions_shouldNotDeleteLocalCopiesOfMessages() {
        messageCountInRemoteFolder(0);
        configureSyncConfigWithSyncRemoteDeletions(false);

        imapSync.sync(FOLDER_NAME, syncConfig, listener, remoteFolder);

        verify(backendFolder, never()).destroyMessages(messageListCaptor.capture());
    }

    @Test
    public void sync_shouldFetchUnsynchronizedMessagesListAndFlags() throws Exception {
        messageCountInRemoteFolder(1);
        hasUnsyncedRemoteMessage();
        when(remoteFolder.supportsFetchingFlags()).thenReturn(true);

        imapSync.sync(FOLDER_NAME, syncConfig, listener, remoteFolder);

        verify(remoteFolder, atLeastOnce()).fetch(any(List.class), fetchProfileCaptor.capture(),
                nullable(MessageRetrievalListener.class));
        assertTrue(fetchProfileCaptor.getAllValues().get(0).contains(FetchProfile.Item.FLAGS));
        assertTrue(fetchProfileCaptor.getAllValues().get(0).contains(FetchProfile.Item.ENVELOPE));
        assertEquals(2, fetchProfileCaptor.getAllValues().get(0).size());
    }

    @Test
    public void sync_withUnsyncedNewSmallMessage_shouldFetchBodyOfSmallMessage() throws Exception {
        Message smallMessage = buildSmallNewMessage();
        messageCountInRemoteFolder(1);
        hasUnsyncedRemoteMessage();
        when(remoteFolder.supportsFetchingFlags()).thenReturn(false);
        respondToFetchEnvelopesWithMessage(smallMessage);

        imapSync.sync(FOLDER_NAME, syncConfig, listener, remoteFolder);

        verify(remoteFolder, atLeast(2)).fetch(any(List.class), fetchProfileCaptor.capture(),
                nullable(MessageRetrievalListener.class));
        assertEquals(1, fetchProfileCaptor.getAllValues().get(1).size());
        assertTrue(fetchProfileCaptor.getAllValues().get(1).contains(FetchProfile.Item.BODY));
    }

    @Test
    public void sync_withUnsyncedNewSmallMessage_shouldFetchStructureAndLimitedBodyOfLargeMessage() throws Exception {
        Message largeMessage = buildLargeNewMessage();
        messageCountInRemoteFolder(1);
        hasUnsyncedRemoteMessage();
        when(remoteFolder.supportsFetchingFlags()).thenReturn(false);
        respondToFetchEnvelopesWithMessage(largeMessage);

        imapSync.sync(FOLDER_NAME, syncConfig, listener, remoteFolder);

        //TODO: Don't bother fetching messages of a size we don't have
        verify(remoteFolder, atLeast(4)).fetch(any(List.class), fetchProfileCaptor.capture(),
                nullable(MessageRetrievalListener.class));
        assertEquals(1, fetchProfileCaptor.getAllValues().get(2).size());
        assertEquals(FetchProfile.Item.STRUCTURE, fetchProfileCaptor.getAllValues().get(2).get(0));
        assertEquals(1, fetchProfileCaptor.getAllValues().get(3).size());
        assertEquals(FetchProfile.Item.BODY_SANE, fetchProfileCaptor.getAllValues().get(3).get(0));
    }

    private void respondToFetchEnvelopesWithMessage(final Message message) throws MessagingException {
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
        }).when(remoteFolder).fetch(any(List.class), any(FetchProfile.class), nullable(MessageRetrievalListener.class));
    }

    private Message buildSmallNewMessage() {
        Message message = mock(Message.class);
        when(message.olderThan(nullable(Date.class))).thenReturn(false);
        when(message.getSize()).thenReturn((long) MAXIMUM_SMALL_MESSAGE_SIZE);
        return message;
    }

    private Message buildLargeNewMessage() {
        Message message = mock(Message.class);
        when(message.olderThan(nullable(Date.class))).thenReturn(false);
        when(message.getSize()).thenReturn((long) (MAXIMUM_SMALL_MESSAGE_SIZE + 1));
        return message;
    }

    private void messageCountInRemoteFolder(int value) {
        when(remoteFolder.getMessageCount()).thenReturn(value);
    }

    private Message messageOnServer() throws MessagingException {
        String messageUid = "UID";
        Message remoteMessage = mock(Message.class);

        when(remoteMessage.getUid()).thenReturn(messageUid);
        when(remoteFolder.getMessages(anyInt(), anyInt(), nullable(Date.class),
                nullable(MessageRetrievalListener.class))).thenReturn(Collections.singletonList(remoteMessage));
        return remoteMessage;
    }

    private void hasUnsyncedRemoteMessage() throws MessagingException {
        String messageUid = "UID";
        Message remoteMessage = mock(Message.class);
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
