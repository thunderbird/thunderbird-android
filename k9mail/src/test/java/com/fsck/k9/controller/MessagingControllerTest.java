package com.fsck.k9.controller;


import android.content.Context;

import com.fsck.k9.Account;
import com.fsck.k9.AccountStats;
import com.fsck.k9.mail.FetchProfile;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessageRetrievalListener;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Store;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.mailstore.LocalStore;
import com.fsck.k9.notification.NotificationController;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.cglib.core.Local;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
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
@RunWith(RobolectricTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml", sdk = 21)
public class MessagingControllerTest {
    private static final String FOLDER_NAME = "Folder";
    private static final int MAXIMUM_SMALL_MESSAGE_SIZE = 1000;


    private MessagingController controller;
    @Mock
    private Account account;
    @Mock
    private AccountStats accountStats;
    @Mock
    private MessagingListener listener;
    @Mock
    private LocalFolder localFolder;
    @Mock
    private Folder remoteFolder;
    @Mock
    private LocalStore localStore;
    @Mock
    private Store remoteStore;
    @Mock
    private NotificationController notificationController;
    @Captor
    private ArgumentCaptor<List<Message>> messageListCaptor;
    @Captor
    private ArgumentCaptor<FetchProfile> fetchProfileCaptor;


    @Before
    public void setUp() throws MessagingException {
        MockitoAnnotations.initMocks(this);
        Context appContext = ShadowApplication.getInstance().getApplicationContext();

        controller = new MessagingController(appContext, notificationController);

        configureAccount();
        configureLocalStore();
    }

    @After
    public void tearDown() throws Exception {
        controller.stop();
    }

    @Test
    public void synchronizeMailboxSynchronous_withOneMessageInRemoteFolder_shouldFinishWithoutError() throws Exception {
        messageCountInRemoteFolder(1);

        controller.synchronizeMailboxSynchronous(account, FOLDER_NAME, listener, remoteFolder);

        verify(listener).synchronizeMailboxFinished(account, FOLDER_NAME, 1, 0);
    }

    @Test
    public void synchronizeMailboxSynchronous_withEmptyRemoteFolder_shouldFinishWithoutError() throws Exception {
        messageCountInRemoteFolder(0);

        controller.synchronizeMailboxSynchronous(account, FOLDER_NAME, listener, remoteFolder);

        verify(listener).synchronizeMailboxFinished(account, FOLDER_NAME, 0, 0);
    }

    @Test
    public void synchronizeMailboxSynchronous_withNegativeMessageCountInRemoteFolder_shouldFinishWithError()
            throws Exception {
        messageCountInRemoteFolder(-1);

        controller.synchronizeMailboxSynchronous(account, FOLDER_NAME, listener, remoteFolder);

        verify(listener).synchronizeMailboxFailed(account, FOLDER_NAME,
                "Exception: Message count -1 for folder Folder");
    }

    @Test
    public void synchronizeMailboxSynchronous_withRemoteFolderProvided_shouldNotOpenRemoteFolder() throws Exception {
        messageCountInRemoteFolder(1);

        controller.synchronizeMailboxSynchronous(account, FOLDER_NAME, listener, remoteFolder);

        verify(remoteFolder, never()).open(Folder.OPEN_MODE_RW);

    }

    @Test
    public void synchronizeMailboxSynchronous_withNoRemoteFolderProvided_shouldOpenRemoteFolderFromStore() throws Exception {
        messageCountInRemoteFolder(1);
        when(account.getExpungePolicy()).thenReturn(Account.Expunge.EXPUNGE_ON_POLL);
        when(account.getRemoteStore()).thenReturn(remoteStore);
        when(remoteStore.getFolder(FOLDER_NAME)).thenReturn(remoteFolder);

        controller.synchronizeMailboxSynchronous(account, FOLDER_NAME, listener, null);

        verify(remoteFolder).open(Folder.OPEN_MODE_RW);

    }

    @Test
    public void synchronizeMailboxSynchronous_withAccountPolicySetToExpungeOnPoll_shouldExpungeRemoteFolder() throws Exception {
        messageCountInRemoteFolder(1);
        when(account.getExpungePolicy()).thenReturn(Account.Expunge.EXPUNGE_ON_POLL);
        when(account.getRemoteStore()).thenReturn(remoteStore);
        when(remoteStore.getFolder(FOLDER_NAME)).thenReturn(remoteFolder);

        controller.synchronizeMailboxSynchronous(account, FOLDER_NAME, listener, null);

        verify(remoteFolder).expunge();
    }

    @Test
    public void synchronizeMailboxSynchronous_withAccountPolicySetToExpungeManually_shouldNotExpungeRemoteFolder() throws Exception {
        messageCountInRemoteFolder(1);
        when(account.getExpungePolicy()).thenReturn(Account.Expunge.EXPUNGE_MANUALLY);

        controller.synchronizeMailboxSynchronous(account, FOLDER_NAME, listener, null);

        verify(remoteFolder, never()).expunge();
    }

    @Test
    public void synchronizeMailboxSynchronous_withAccountSetToSyncRemoteDeletions_shouldDeleteLocalCopiesOfDeletedMessages() throws Exception {
        messageCountInRemoteFolder(0);
        LocalMessage localCopyOfRemoteDeletedMessage = mock(LocalMessage.class);
        when(account.syncRemoteDeletions()).thenReturn(true);
        when(localFolder.getMessages(null)).thenReturn(Collections.singletonList(localCopyOfRemoteDeletedMessage));

        controller.synchronizeMailboxSynchronous(account, FOLDER_NAME, listener, remoteFolder);

        verify(localFolder).destroyMessages(messageListCaptor.capture());
        assertEquals(localCopyOfRemoteDeletedMessage, messageListCaptor.getValue().get(0));
    }

    @Test
    public void synchronizeMailboxSynchronous_withAccountSetToSyncRemoteDeletions_shouldNotDeleteLocalCopiesOfExistingMessagesAfterEarliestPollDate() throws Exception {
        messageCountInRemoteFolder(1);
        Date dateOfEarliestPoll = new Date();
        LocalMessage localMessage = localMessageWithCopyOnServer();
        when(account.syncRemoteDeletions()).thenReturn(true);
        when(account.getEarliestPollDate()).thenReturn(dateOfEarliestPoll);
        when(localMessage.olderThan(dateOfEarliestPoll)).thenReturn(false);
        when(localFolder.getMessages(null)).thenReturn(Collections.singletonList(localMessage));

        controller.synchronizeMailboxSynchronous(account, FOLDER_NAME, listener, remoteFolder);

        verify(localFolder, never()).destroyMessages(messageListCaptor.capture());
    }


    @Test
    public void synchronizeMailboxSynchronous_withAccountSetToSyncRemoteDeletions_shouldDeleteLocalCopiesOfExistingMessagesBeforeEarliestPollDate() throws Exception {
        messageCountInRemoteFolder(1);
        LocalMessage localMessage = localMessageWithCopyOnServer();
        Date dateOfEarliestPoll = new Date();

        when(account.syncRemoteDeletions()).thenReturn(true);
        when(account.getEarliestPollDate()).thenReturn(dateOfEarliestPoll);
        when(localMessage.olderThan(dateOfEarliestPoll)).thenReturn(true);
        when(localFolder.getMessages(null)).thenReturn(Collections.singletonList(localMessage));

        controller.synchronizeMailboxSynchronous(account, FOLDER_NAME, listener, remoteFolder);

        verify(localFolder).destroyMessages(messageListCaptor.capture());
        assertEquals(localMessage, messageListCaptor.getValue().get(0));
    }

    @Test
    public void synchronizeMailboxSynchronous_withAccountSetNotToSyncRemoteDeletions_shouldNotDeleteLocalCopiesOfMessages() throws Exception {
        messageCountInRemoteFolder(0);
        LocalMessage remoteDeletedMessage = mock(LocalMessage.class);
        when(account.syncRemoteDeletions()).thenReturn(false);
        when(localFolder.getMessages(null)).thenReturn(Collections.singletonList(remoteDeletedMessage));

        controller.synchronizeMailboxSynchronous(account, FOLDER_NAME, listener, remoteFolder);

        verify(localFolder, never()).destroyMessages(messageListCaptor.capture());
    }

    @Test
    public void synchronizeMailboxSynchronous_withAccountSupportingFetchingFlags_shouldFetchUnsychronizedMessagesListAndFlags() throws Exception {
        messageCountInRemoteFolder(1);
        hasUnsychedRemoteMessage();
        when(remoteFolder.supportsFetchingFlags()).thenReturn(true);

        controller.synchronizeMailboxSynchronous(account, FOLDER_NAME, listener, remoteFolder);

        verify(remoteFolder, atLeastOnce()).fetch(any(List.class), fetchProfileCaptor.capture(), any(MessageRetrievalListener.class));
        assertEquals(2, fetchProfileCaptor.getAllValues().get(0).size());
        assertTrue(fetchProfileCaptor.getAllValues().get(0).contains(FetchProfile.Item.FLAGS));
        assertTrue(fetchProfileCaptor.getAllValues().get(0).contains(FetchProfile.Item.ENVELOPE));
    }

    @Test
    public void synchronizeMailboxSynchronous_withAccountNotSupportingFetchingFlags_shouldFetchUnsychronizedMessages() throws Exception {
        messageCountInRemoteFolder(1);
        hasUnsychedRemoteMessage();
        when(remoteFolder.supportsFetchingFlags()).thenReturn(false);

        controller.synchronizeMailboxSynchronous(account, FOLDER_NAME, listener, remoteFolder);

        verify(remoteFolder, atLeastOnce()).fetch(any(List.class), fetchProfileCaptor.capture(), any(MessageRetrievalListener.class));
        assertEquals(1, fetchProfileCaptor.getAllValues().get(0).size());
        assertTrue(fetchProfileCaptor.getAllValues().get(0).contains(FetchProfile.Item.ENVELOPE));
    }

    @Test
    public void synchronizeMailboxSynchronous_withUnsyncedNewSmallMessage_shouldFetchBodyOfSmallMessage() throws Exception {
        final Message smallMessageEnvelope = mock(Message.class);
        when(smallMessageEnvelope.olderThan(any(Date.class))).thenReturn(false);
        when(smallMessageEnvelope.getSize()).thenReturn(MAXIMUM_SMALL_MESSAGE_SIZE);

        messageCountInRemoteFolder(1);
        hasUnsychedRemoteMessage();
        when(remoteFolder.supportsFetchingFlags()).thenReturn(false);
        doAnswer(new Answer() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FetchProfile fp = (FetchProfile) invocation.getArguments()[1];
                MessageRetrievalListener l = (MessageRetrievalListener) invocation.getArguments()[2];
                if(fp.contains(FetchProfile.Item.ENVELOPE)) {
                    l.messageStarted("UID", 1, 1);
                    l.messageFinished(smallMessageEnvelope, 1, 1);
                    l.messagesFinished(1);
                }
                return null;
            }
        }).when(remoteFolder).fetch(any(List.class), any(FetchProfile.class), any(MessageRetrievalListener.class));

        controller.synchronizeMailboxSynchronous(account, FOLDER_NAME, listener, remoteFolder);

        verify(remoteFolder, atLeast(2)).fetch(any(List.class), fetchProfileCaptor.capture(), any(MessageRetrievalListener.class));
        assertEquals(1, fetchProfileCaptor.getAllValues().get(1).size());
        assertTrue(fetchProfileCaptor.getAllValues().get(1).contains(FetchProfile.Item.BODY));
    }

    @Test
    public void synchronizeMailboxSynchronous_withUnsyncedNewSmallMessage_shouldOnlyFetchStructureOfLargeMessage() throws Exception {
        final Message largeMessageEnvelope = mock(Message.class);
        when(largeMessageEnvelope.olderThan(any(Date.class))).thenReturn(false);
        when(largeMessageEnvelope.getSize()).thenReturn(MAXIMUM_SMALL_MESSAGE_SIZE+1);

        messageCountInRemoteFolder(1);
        hasUnsychedRemoteMessage();
        when(remoteFolder.supportsFetchingFlags()).thenReturn(false);
        doAnswer(new Answer() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FetchProfile fp = (FetchProfile) invocation.getArguments()[1];
                System.out.println(fp);
                if(invocation.getArguments()[2] != null) {
                    MessageRetrievalListener l = (MessageRetrievalListener) invocation.getArguments()[2];
                    if (fp.contains(FetchProfile.Item.ENVELOPE)) {
                        l.messageStarted("UID", 1, 1);
                        l.messageFinished(largeMessageEnvelope, 1, 1);
                        l.messagesFinished(1);
                    }
                }
                return null;
            }
        }).when(remoteFolder).fetch(any(List.class), any(FetchProfile.class), any(MessageRetrievalListener.class));

        controller.synchronizeMailboxSynchronous(account, FOLDER_NAME, listener, remoteFolder);

        //TODO: Don't bother fetching messages of a size we don't have
        verify(remoteFolder, atLeast(4)).fetch(any(List.class), fetchProfileCaptor.capture(), any(MessageRetrievalListener.class));

        assertEquals(1, fetchProfileCaptor.getAllValues().get(2).size());
        assertEquals(FetchProfile.Item.STRUCTURE, fetchProfileCaptor.getAllValues().get(2).get(0));
        assertEquals(1, fetchProfileCaptor.getAllValues().get(3).size());
        assertEquals(FetchProfile.Item.BODY_SANE, fetchProfileCaptor.getAllValues().get(3).get(0));
    }

    private void messageCountInRemoteFolder(int value) throws MessagingException {
        when(remoteFolder.getMessageCount()).thenReturn(value);
    }

    private LocalMessage localMessageWithCopyOnServer() throws MessagingException {
        String messageUid = "UID";
        Message remoteMessage = mock(Message.class);
        LocalMessage localMessage = mock(LocalMessage.class);

        when(remoteMessage.getUid()).thenReturn(messageUid);
        when(localMessage.getUid()).thenReturn(messageUid);
        when(remoteFolder.getMessages(anyInt(), anyInt(), any(Date.class), any(MessageRetrievalListener.class)))
                .thenReturn(Collections.singletonList(remoteMessage));
        return localMessage;
    }

    private void hasUnsychedRemoteMessage() throws MessagingException {
        String messageUid = "UID";
        Message remoteMessage = mock(Message.class);
        when(remoteMessage.getUid()).thenReturn(messageUid);
        when(remoteFolder.getMessages(anyInt(), anyInt(), any(Date.class), any(MessageRetrievalListener.class)))
                .thenReturn(Collections.singletonList(remoteMessage));
    }

    private void configureAccount() throws MessagingException {
        when(account.getLocalStore()).thenReturn(localStore);
        when(account.getStats(any(Context.class))).thenReturn(accountStats);
        when(account.getMaximumAutoDownloadMessageSize()).thenReturn(MAXIMUM_SMALL_MESSAGE_SIZE);
    }

    private void configureLocalStore() {
        when(localStore.getFolder(FOLDER_NAME)).thenReturn(localFolder);
    }
}
