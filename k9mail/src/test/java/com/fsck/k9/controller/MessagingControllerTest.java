package com.fsck.k9.controller;


import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import android.content.Context;

import com.fsck.k9.Account;
import com.fsck.k9.AccountStats;
import com.fsck.k9.K9;
import com.fsck.k9.K9RobolectricTestRunner;
import com.fsck.k9.Preferences;
import com.fsck.k9.controller.tasks.SearchResultsLoader;
import com.fsck.k9.helper.Contacts;
import com.fsck.k9.mail.AuthenticationFailedException;
import com.fsck.k9.mail.CertificateValidationException;
import com.fsck.k9.mail.FetchProfile;
import com.fsck.k9.mail.FetchProfile.Item;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessageRetrievalListener;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Store;
import com.fsck.k9.mail.Transport;
import com.fsck.k9.mail.TransportProvider;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.mailstore.LocalStore;
import com.fsck.k9.notification.NotificationController;
import com.fsck.k9.search.LocalSearch;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowLog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;


@SuppressWarnings("unchecked")
@RunWith(K9RobolectricTestRunner.class)
public class MessagingControllerTest {
    private static final String FOLDER_NAME = "Folder";
    private static final String SENT_FOLDER_NAME = "Sent";
    private static final int MAXIMUM_SMALL_MESSAGE_SIZE = 1000;
    private static final String MESSAGE_UID1 = "message-uid1";


    private MessagingController controller;
    @Mock
    private Contacts contacts;
    @Mock
    private Account account;
    @Mock
    private AccountStats accountStats;
    @Mock
    private SimpleMessagingListener listener;
    @Mock
    private LocalSearch search;
    @Mock
    private LocalFolder localFolder;
    @Mock
    private LocalFolder sentFolder;
    @Mock
    private Folder remoteFolder;
    @Mock
    private LocalStore localStore;
    @Mock
    private Store remoteStore;
    @Mock
    private NotificationController notificationController;
    @Mock
    private TransportProvider transportProvider;
    @Mock
    private Transport transport;
    @Mock
    private SearchResultsLoader searchResultsLoader;
    @Captor
    private ArgumentCaptor<List<Message>> messageListCaptor;
    @Captor
    private ArgumentCaptor<FetchProfile> fetchProfileCaptor;
    @Captor
    private ArgumentCaptor<MessageRetrievalListener<LocalMessage>> messageRetrievalListenerCaptor;

    private Context appContext;

    @Mock
    private Message remoteNewMessage1;
    @Mock
    private LocalMessage localNewMessage1;
    @Mock
    private LocalMessage localMessageToSend1;


    @Before
    public void setUp() throws MessagingException {
        ShadowLog.stream = System.out;
        MockitoAnnotations.initMocks(this);
        appContext = ShadowApplication.getInstance().getApplicationContext();

        controller = new MessagingController(
                appContext, notificationController, contacts, transportProvider, searchResultsLoader);

        configureAccount();
        configureLocalStore();
    }

    @After
    public void tearDown() throws Exception {
        controller.stop();
    }

    @Test
    public void searchLocalMessagesSynchronous_shouldCallSearchForMessagesOnLocalStore()
            throws Exception {
        setAccountsInPreferences(Collections.singletonMap("1", account));
        when(search.getAccountUuids()).thenReturn(new String[]{"allAccounts"});

        controller.searchLocalMessagesSynchronous(search, listener);

        verify(localStore).searchForMessages(any(MessageRetrievalListener.class), eq(search));
    }

    @Test
    public void searchLocalMessagesSynchronous_shouldNotifyWhenStoreFinishesRetrievingAMessage()
            throws Exception {
        setAccountsInPreferences(Collections.singletonMap("1", account));
        LocalMessage localMessage = mock(LocalMessage.class);
        when(localMessage.getFolder()).thenReturn(localFolder);
        when(search.getAccountUuids()).thenReturn(new String[]{"allAccounts"});
        when(localStore.searchForMessages(any(MessageRetrievalListener.class), eq(search)))
                .thenThrow(new MessagingException("Test"));

        controller.searchLocalMessagesSynchronous(search, listener);

        verify(localStore).searchForMessages(messageRetrievalListenerCaptor.capture(), eq(search));
        messageRetrievalListenerCaptor.getValue().messageFinished(localMessage, 1, 1);
        verify(listener).listLocalMessagesAddMessages(eq(account),
                eq((String) null), eq(Collections.singletonList(localMessage)));
    }

    @Test
    public void loadMessageRemoteSynchronous_shouldOpenLocalFolder() throws Exception {
        controller.loadMessageRemoteSynchronous(account, FOLDER_NAME, "1", listener,
                false);

        verify(localFolder).open(Folder.OPEN_MODE_RW);
    }

    @Test
    public void loadMessageRemoteSynchronous_shouldGetMessageFromLocalFolder() throws Exception {
        controller.loadMessageRemoteSynchronous(account, FOLDER_NAME, "1", listener,
                false);

        verify(localFolder).getMessage("1");
    }

    @Test
    public void loadMessageRemoteSynchronous_shouldNotOpenRemoteFolder_forLocalUid() throws Exception {
        controller.loadMessageRemoteSynchronous(account, FOLDER_NAME, "K9LOCAL:1", listener,
                false);

        verify(remoteFolder, never()).open(anyInt());
    }

    @Test
    public void loadMessageRemoteSynchronous_shouldOpenRemoteFolder_forRemoteUid() throws Exception {
        configureRemoteStoreWithFolder();
        controller.loadMessageRemoteSynchronous(account, FOLDER_NAME, "1", listener,
                false);

        verify(remoteFolder).open(Folder.OPEN_MODE_RW);
    }

    @Test
    public void loadMessageRemoteSynchronous_shouldGetMessageUsingRemoteUid() throws Exception {
        configureRemoteStoreWithFolder();
        controller.loadMessageRemoteSynchronous(account, FOLDER_NAME, "1", listener,
                false);

        verify(remoteFolder).getMessage("1");
    }

    @Test
    public void loadMessageRemoteSynchronous_withLoadPartialFromSearchFalse_shouldFetchBodyAndFlags()
            throws Exception {
        configureRemoteStoreWithFolder();
        when(localFolder.getMessage("1")).thenReturn(localNewMessage1);
        when(remoteFolder.getMessage("1")).thenReturn(remoteNewMessage1);
        controller.loadMessageRemoteSynchronous(account, FOLDER_NAME, "1", listener,
                false);

        verify(remoteFolder).fetch(eq(Collections.singletonList(remoteNewMessage1)),
                fetchProfileCaptor.capture(), Matchers.<MessageRetrievalListener>eq(null));
        assertTrue(fetchProfileCaptor.getValue().contains(Item.BODY));
        assertTrue(fetchProfileCaptor.getValue().contains(Item.FLAGS));
        assertEquals(2, fetchProfileCaptor.getValue().size());
    }

    @Test
    public void loadMessageRemoteSynchronous_withLoadPartialFromSearchTrue_shouldFetchUnsyncedSmallAndLargeMessages()
            throws Exception {
        configureRemoteStoreWithFolder();
        when(localFolder.getMessage("1")).thenReturn(localNewMessage1);
        when(localNewMessage1.getUid()).thenReturn("1");
        when(remoteFolder.getMessage("1")).thenReturn(remoteNewMessage1);
        when(remoteNewMessage1.getUid()).thenReturn("1");
        controller.loadMessageRemoteSynchronous(account, FOLDER_NAME, "1", listener,
                true);

        verify(remoteFolder, times(3)).fetch(eq(Collections.emptyList()),
                fetchProfileCaptor.capture(), messageRetrievalListenerCaptor.capture());
        assertTrue(fetchProfileCaptor.getAllValues().get(0).contains(Item.ENVELOPE));
        assertEquals(1, fetchProfileCaptor.getAllValues().get(0).size());
        assertTrue(fetchProfileCaptor.getAllValues().get(1).contains(Item.BODY));
        assertEquals(1, fetchProfileCaptor.getAllValues().get(1).size());
        assertTrue(fetchProfileCaptor.getAllValues().get(2).contains(Item.STRUCTURE));
        assertEquals(1, fetchProfileCaptor.getAllValues().get(2).size());
    }

    @Test
    public void sendPendingMessagesSynchronous_withNonExistentOutbox_shouldNotStartSync() throws MessagingException {
        when(account.getOutboxFolderName()).thenReturn(FOLDER_NAME);
        when(localFolder.exists()).thenReturn(false);
        controller.addListener(listener);

        controller.sendPendingMessagesSynchronous(account);

        verifyZeroInteractions(listener);
    }

    @Test
    public void sendPendingMessagesSynchronous_shouldCallListenerOnStart() throws MessagingException {
        setupAccountWithMessageToSend();

        controller.sendPendingMessagesSynchronous(account);

        verify(listener).sendPendingMessagesStarted(account);
    }

    @Test
    public void sendPendingMessagesSynchronous_shouldSetProgress() throws MessagingException {
        setupAccountWithMessageToSend();

        controller.sendPendingMessagesSynchronous(account);

        verify(listener).synchronizeMailboxProgress(account, "Sent", 0, 1);
    }

    @Test
    public void sendPendingMessagesSynchronous_shouldSendMessageUsingTransport() throws MessagingException {
        setupAccountWithMessageToSend();

        controller.sendPendingMessagesSynchronous(account);

        verify(transport).sendMessage(localMessageToSend1);
    }

    @Test
    public void sendPendingMessagesSynchronous_shouldSetAndRemoveSendInProgressFlag() throws MessagingException {
        setupAccountWithMessageToSend();

        controller.sendPendingMessagesSynchronous(account);

        InOrder ordering = inOrder(localMessageToSend1, transport);
        ordering.verify(localMessageToSend1).setFlag(Flag.X_SEND_IN_PROGRESS, true);
        ordering.verify(transport).sendMessage(localMessageToSend1);
        ordering.verify(localMessageToSend1).setFlag(Flag.X_SEND_IN_PROGRESS, false);
    }

    @Test
    public void sendPendingMessagesSynchronous_shouldMarkSentMessageAsSeen() throws MessagingException {
        setupAccountWithMessageToSend();

        controller.sendPendingMessagesSynchronous(account);

        verify(localMessageToSend1).setFlag(Flag.SEEN, true);
    }

    @Test
    public void sendPendingMessagesSynchronous_whenMessageSentSuccesfully_shouldUpdateProgress() throws MessagingException {
        setupAccountWithMessageToSend();

        controller.sendPendingMessagesSynchronous(account);

        verify(listener).synchronizeMailboxProgress(account, "Sent", 1, 1);
    }

    @Test
    public void sendPendingMessagesSynchronous_shouldUpdateProgress() throws MessagingException {
        setupAccountWithMessageToSend();

        controller.sendPendingMessagesSynchronous(account);

        verify(listener).synchronizeMailboxProgress(account, "Sent", 1, 1);
    }

    @Test
    public void sendPendingMessagesSynchronous_withAuthenticationFailure_shouldNotify() throws MessagingException {
        setupAccountWithMessageToSend();
        doThrow(new AuthenticationFailedException("Test")).when(transport).sendMessage(localMessageToSend1);

        controller.sendPendingMessagesSynchronous(account);

        verify(notificationController).showAuthenticationErrorNotification(account, false);
    }

    @Test
    public void sendPendingMessagesSynchronous_withCertificateFailure_shouldNotify() throws MessagingException {
        setupAccountWithMessageToSend();
        doThrow(new CertificateValidationException("Test")).when(transport).sendMessage(localMessageToSend1);

        controller.sendPendingMessagesSynchronous(account);

        verify(notificationController).showCertificateErrorNotification(account, false);
    }

    @Test
    public void sendPendingMessagesSynchronous_shouldCallListenerOnCompletion() throws MessagingException {
        setupAccountWithMessageToSend();

        controller.sendPendingMessagesSynchronous(account);

        verify(listener).sendPendingMessagesCompleted(account);
    }


    @Test
    public void synchronizeMailboxSynchronousLegacy_withOneMessageInRemoteFolder_shouldFinishWithoutError()
            throws Exception {
        configureRemoteStoreWithFolder();
        messageCountInRemoteFolder(1);

        controller.synchronizeMailboxSynchronousLegacy(account, FOLDER_NAME, listener);

        verify(listener).synchronizeMailboxFinished(account, FOLDER_NAME, 1, 0);
    }

    @Test
    public void synchronizeMailboxSynchronousLegacy_withEmptyRemoteFolder_shouldFinishWithoutError()
            throws Exception {
        configureRemoteStoreWithFolder();
        messageCountInRemoteFolder(0);

        controller.synchronizeMailboxSynchronousLegacy(account, FOLDER_NAME, listener);

        verify(listener).synchronizeMailboxFinished(account, FOLDER_NAME, 0, 0);
    }

    @Test
    public void synchronizeMailboxSynchronousLegacy_withNegativeMessageCountInRemoteFolder_shouldFinishWithError()
            throws Exception {
        configureRemoteStoreWithFolder();
        messageCountInRemoteFolder(-1);

        controller.synchronizeMailboxSynchronousLegacy(account, FOLDER_NAME, listener);

        verify(listener).synchronizeMailboxFailed(account, FOLDER_NAME,
                "Exception: Message count -1 for folder Folder");
    }

    @Test
    public void synchronizeMailboxSynchronousLegacy_withRemoteFolderProvided_shouldNotOpenRemoteFolder()
            throws Exception {
        messageCountInRemoteFolder(1);

        controller.synchronizeMailboxSynchronousLegacy(account, FOLDER_NAME, listener);

        verify(remoteFolder, never()).open(Folder.OPEN_MODE_RW);
    }

    @Test
    public void synchronizeMailboxSynchronousLegacy_withNoRemoteFolderProvided_shouldOpenRemoteFolderFromStore()
            throws Exception {
        messageCountInRemoteFolder(1);
        configureRemoteStoreWithFolder();

        controller.synchronizeMailboxSynchronousLegacy(account, FOLDER_NAME, listener);

        verify(remoteFolder).open(Folder.OPEN_MODE_RO);
    }

    @Test
    public void synchronizeMailboxSynchronousLegacy_withRemoteFolderProvided_shouldNotCloseRemoteFolder() throws Exception {
        messageCountInRemoteFolder(1);

        controller.synchronizeMailboxSynchronousLegacy(account, FOLDER_NAME, listener);

        verify(remoteFolder, never()).close();
    }

    @Test
    public void synchronizeMailboxSynchronousLegacy_withNoRemoteFolderProvided_shouldCloseRemoteFolderFromStore()
            throws Exception {
        messageCountInRemoteFolder(1);
        configureRemoteStoreWithFolder();

        controller.synchronizeMailboxSynchronousLegacy(account, FOLDER_NAME, listener);

        verify(remoteFolder).close();
    }

    @Test
    public void synchronizeMailboxSynchronousLegacy_withAccountSetToSyncRemoteDeletions_shouldDeleteLocalCopiesOfDeletedMessages()
            throws Exception {
        configureRemoteStoreWithFolder();
        messageCountInRemoteFolder(0);
        LocalMessage localCopyOfRemoteDeletedMessage = mock(LocalMessage.class);
        when(account.syncRemoteDeletions()).thenReturn(true);
        when(localFolder.getAllMessagesAndEffectiveDates()).thenReturn(Collections.singletonMap(MESSAGE_UID1, 0L));
        when(localFolder.getMessagesByUids(any(List.class)))
                .thenReturn(Collections.singletonList(localCopyOfRemoteDeletedMessage));

        controller.synchronizeMailboxSynchronousLegacy(account, FOLDER_NAME, listener);

        verify(localFolder).destroyMessages(messageListCaptor.capture());
        assertEquals(localCopyOfRemoteDeletedMessage, messageListCaptor.getValue().get(0));
    }

    @Test
    public void synchronizeMailboxSynchronousLegacy_withAccountSetToSyncRemoteDeletions_shouldNotDeleteLocalCopiesOfExistingMessagesAfterEarliestPollDate()
            throws Exception {
        messageCountInRemoteFolder(1);
        Date dateOfEarliestPoll = new Date();
        LocalMessage localMessage = localMessageWithCopyOnServer();
        when(account.syncRemoteDeletions()).thenReturn(true);
        when(account.getEarliestPollDate()).thenReturn(dateOfEarliestPoll);
        when(localMessage.olderThan(dateOfEarliestPoll)).thenReturn(false);
        when(localFolder.getMessages(null)).thenReturn(Collections.singletonList(localMessage));

        controller.synchronizeMailboxSynchronousLegacy(account, FOLDER_NAME, listener);

        verify(localFolder, never()).destroyMessages(messageListCaptor.capture());
    }

    @Test
    public void synchronizeMailboxSynchronousLegacy_withAccountSetToSyncRemoteDeletions_shouldDeleteLocalCopiesOfExistingMessagesBeforeEarliestPollDate()
            throws Exception {
        configureRemoteStoreWithFolder();
        messageCountInRemoteFolder(1);
        LocalMessage localMessage = localMessageWithCopyOnServer();
        Date dateOfEarliestPoll = new Date();
        when(account.syncRemoteDeletions()).thenReturn(true);
        when(account.getEarliestPollDate()).thenReturn(dateOfEarliestPoll);
        when(localMessage.olderThan(dateOfEarliestPoll)).thenReturn(true);
        when(localFolder.getAllMessagesAndEffectiveDates()).thenReturn(Collections.singletonMap(MESSAGE_UID1, 0L));
        when(localFolder.getMessagesByUids(any(List.class))).thenReturn(Collections.singletonList(localMessage));

        controller.synchronizeMailboxSynchronousLegacy(account, FOLDER_NAME, listener);

        verify(localFolder).destroyMessages(messageListCaptor.capture());
        assertEquals(localMessage, messageListCaptor.getValue().get(0));
    }

    @Test
    public void synchronizeMailboxSynchronousLegacy_withAccountSetNotToSyncRemoteDeletions_shouldNotDeleteLocalCopiesOfMessages()
            throws Exception {
        messageCountInRemoteFolder(0);
        LocalMessage remoteDeletedMessage = mock(LocalMessage.class);
        when(account.syncRemoteDeletions()).thenReturn(false);
        when(localFolder.getMessages(null)).thenReturn(Collections.singletonList(remoteDeletedMessage));

        controller.synchronizeMailboxSynchronousLegacy(account, FOLDER_NAME, listener);

        verify(localFolder, never()).destroyMessages(messageListCaptor.capture());
    }

    @Test
    public void synchronizeMailboxSynchronousLegacy_withAccountSupportingFetchingFlags_shouldFetchUnsychronizedMessagesListAndFlags()
            throws Exception {
        configureRemoteStoreWithFolder();
        messageCountInRemoteFolder(1);
        hasUnsyncedRemoteMessage();
        when(remoteFolder.supportsFetchingFlags()).thenReturn(true);

        controller.synchronizeMailboxSynchronousLegacy(account, FOLDER_NAME, listener);

        verify(remoteFolder, atLeastOnce()).fetch(any(List.class), fetchProfileCaptor.capture(),
                any(MessageRetrievalListener.class));
        assertTrue(fetchProfileCaptor.getAllValues().get(0).contains(FetchProfile.Item.FLAGS));
        assertTrue(fetchProfileCaptor.getAllValues().get(0).contains(FetchProfile.Item.ENVELOPE));
        assertEquals(2, fetchProfileCaptor.getAllValues().get(0).size());
    }

    @Test
    public void synchronizeMailboxSynchronousLegacy_withAccountNotSupportingFetchingFlags_shouldFetchUnsychronizedMessages()
            throws Exception {
        configureRemoteStoreWithFolder();
        messageCountInRemoteFolder(1);
        hasUnsyncedRemoteMessage();
        when(remoteFolder.supportsFetchingFlags()).thenReturn(false);

        controller.synchronizeMailboxSynchronousLegacy(account, FOLDER_NAME, listener);

        verify(remoteFolder, atLeastOnce()).fetch(any(List.class), fetchProfileCaptor.capture(),
                any(MessageRetrievalListener.class));
        assertEquals(1, fetchProfileCaptor.getAllValues().get(0).size());
        assertTrue(fetchProfileCaptor.getAllValues().get(0).contains(FetchProfile.Item.ENVELOPE));
    }

    @Test
    public void synchronizeMailboxSynchronousLegacy_withUnsyncedNewSmallMessage_shouldFetchBodyOfSmallMessage()
            throws Exception {
        Message smallMessage = buildSmallNewMessage();
        configureRemoteStoreWithFolder();
        messageCountInRemoteFolder(1);
        hasUnsyncedRemoteMessage();
        when(remoteFolder.supportsFetchingFlags()).thenReturn(false);
        respondToFetchEnvelopesWithMessage(smallMessage);

        controller.synchronizeMailboxSynchronousLegacy(account, FOLDER_NAME, listener);

        verify(remoteFolder, atLeast(2)).fetch(any(List.class), fetchProfileCaptor.capture(),
                any(MessageRetrievalListener.class));
        assertEquals(1, fetchProfileCaptor.getAllValues().get(1).size());
        assertTrue(fetchProfileCaptor.getAllValues().get(1).contains(FetchProfile.Item.BODY));
    }

    @Test
    public void synchronizeMailboxSynchronousLegacy_withUnsyncedNewSmallMessage_shouldFetchStructureAndLimitedBodyOfLargeMessage()
            throws Exception {
        Message largeMessage = buildLargeNewMessage();
        configureRemoteStoreWithFolder();
        messageCountInRemoteFolder(1);
        hasUnsyncedRemoteMessage();
        when(remoteFolder.supportsFetchingFlags()).thenReturn(false);
        respondToFetchEnvelopesWithMessage(largeMessage);

        controller.synchronizeMailboxSynchronousLegacy(account, FOLDER_NAME, listener);

        //TODO: Don't bother fetching messages of a size we don't have
        verify(remoteFolder, atLeast(4)).fetch(any(List.class), fetchProfileCaptor.capture(),
                any(MessageRetrievalListener.class));
        assertEquals(1, fetchProfileCaptor.getAllValues().get(2).size());
        assertEquals(FetchProfile.Item.STRUCTURE, fetchProfileCaptor.getAllValues().get(2).get(0));
        assertEquals(1, fetchProfileCaptor.getAllValues().get(3).size());
        assertEquals(FetchProfile.Item.BODY_SANE, fetchProfileCaptor.getAllValues().get(3).get(0));
    }

    private void setupAccountWithMessageToSend() throws MessagingException {
        when(account.getOutboxFolderName()).thenReturn(FOLDER_NAME);
        when(account.hasSentFolder()).thenReturn(true);
        when(account.getSentFolderName()).thenReturn(SENT_FOLDER_NAME);
        when(localStore.getFolder(SENT_FOLDER_NAME)).thenReturn(sentFolder);
        when(sentFolder.getDatabaseId()).thenReturn(1L);
        when(localFolder.exists()).thenReturn(true);
        when(transportProvider.getTransport(appContext, account)).thenReturn(transport);
        when(localFolder.getMessages(null)).thenReturn(Collections.singletonList(localMessageToSend1));
        when(localMessageToSend1.getUid()).thenReturn("localMessageToSend1");
        when(localMessageToSend1.getHeader(K9.IDENTITY_HEADER)).thenReturn(new String[]{});
        controller.addListener(listener);
    }

    private void respondToFetchEnvelopesWithMessage(final Message message) throws MessagingException {
        doAnswer(new Answer() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
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
        }).when(remoteFolder).fetch(any(List.class), any(FetchProfile.class), any(MessageRetrievalListener.class));
    }

    private Message buildSmallNewMessage() {
        Message message = mock(Message.class);
        when(message.olderThan(any(Date.class))).thenReturn(false);
        when(message.getSize()).thenReturn((long) MAXIMUM_SMALL_MESSAGE_SIZE);
        return message;
    }

    private Message buildLargeNewMessage() {
        Message message = mock(Message.class);
        when(message.olderThan(any(Date.class))).thenReturn(false);
        when(message.getSize()).thenReturn((long) (MAXIMUM_SMALL_MESSAGE_SIZE + 1));
        return message;
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

    private void hasUnsyncedRemoteMessage() throws MessagingException {
        String messageUid = "UID";
        Message remoteMessage = mock(Message.class);
        when(remoteMessage.getUid()).thenReturn(messageUid);
        when(remoteFolder.getMessages(anyInt(), anyInt(), any(Date.class), any(MessageRetrievalListener.class)))
                .thenReturn(Collections.singletonList(remoteMessage));
    }

    private void configureAccount() throws MessagingException {
        when(account.isAvailable(appContext)).thenReturn(true);
        when(account.getLocalStore()).thenReturn(localStore);
        when(account.getStats(any(Context.class))).thenReturn(accountStats);
        when(account.getMaximumAutoDownloadMessageSize()).thenReturn(MAXIMUM_SMALL_MESSAGE_SIZE);
        when(account.getEmail()).thenReturn("user@host.com");
    }

    private void configureLocalStore() throws MessagingException {
        when(localStore.getFolder(FOLDER_NAME)).thenReturn(localFolder);
        when(localFolder.getName()).thenReturn(FOLDER_NAME);
        when(localStore.getPersonalNamespaces(false)).thenReturn(Collections.singletonList(localFolder));
    }

    private void configureRemoteStoreWithFolder() throws MessagingException {
        when(account.getRemoteStore()).thenReturn(remoteStore);
        when(remoteStore.getFolder(FOLDER_NAME)).thenReturn(remoteFolder);
        when(remoteFolder.getName()).thenReturn(FOLDER_NAME);
    }

    private void setAccountsInPreferences(Map<String, Account> newAccounts)
            throws Exception {
        Field accounts = Preferences.class.getDeclaredField("accounts");
        accounts.setAccessible(true);
        accounts.set(Preferences.getPreferences(appContext), newAccounts);

        Field accountsInOrder = Preferences.class.getDeclaredField("accountsInOrder");
        accountsInOrder.setAccessible(true);
        ArrayList<Account> newAccountsInOrder = new ArrayList<>();
        newAccountsInOrder.addAll(newAccounts.values());
        accountsInOrder.set(Preferences.getPreferences(appContext), newAccountsInOrder);
    }
}
