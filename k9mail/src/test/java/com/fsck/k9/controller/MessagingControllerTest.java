package com.fsck.k9.controller;


import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.Context;

import com.fsck.k9.Account;
import com.fsck.k9.AccountStats;
import com.fsck.k9.K9;
import com.fsck.k9.K9RobolectricTestRunner;
import com.fsck.k9.Preferences;
import com.fsck.k9.helper.Contacts;
import com.fsck.k9.mail.AuthenticationFailedException;
import com.fsck.k9.mail.CertificateValidationException;
import com.fsck.k9.mail.FetchProfile;
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
import com.fsck.k9.mailstore.UnavailableStorageException;
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
import static org.mockito.Matchers.anySet;
import static org.mockito.Matchers.anyString;
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
    private static final String FOLDER_ID = "FolderID";
    private static final String FOLDER_NAME = "Folder";
    private static final String SENT_FOLDER_ID = "SentID";
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
    private LocalFolder errorFolder;
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
    @Captor
    private ArgumentCaptor<List<Message>> messageListCaptor;
    @Captor
    private ArgumentCaptor<List<LocalFolder>> localFolderListCaptor;
    @Captor
    private ArgumentCaptor<FetchProfile> fetchProfileCaptor;
    @Captor
    private ArgumentCaptor<MessageRetrievalListener<LocalMessage>> messageRetrievalListenerCaptor;

    private Context appContext;
    private Set<Flag> reqFlags;
    private Set<Flag> forbiddenFlags;

    private List<Message> remoteMessages;
    @Mock
    private Message remoteOldMessage;
    @Mock
    private Message remoteNewMessage1;
    @Mock
    private Message remoteNewMessage2;
    @Mock
    private LocalMessage localNewMessage1;
    @Mock
    private LocalMessage localNewMessage2;
    @Mock
    private LocalMessage localMessageToSend1;
    private volatile boolean hasFetchedMessage = false;


    @Before
    public void setUp() throws MessagingException {
        ShadowLog.stream = System.out;
        MockitoAnnotations.initMocks(this);
        appContext = ShadowApplication.getInstance().getApplicationContext();

        controller = new MessagingController(appContext, notificationController, contacts, transportProvider);

        configureAccount();
        configureLocalStore();
    }

    @After
    public void tearDown() throws Exception {
        controller.stop();
    }

    @Test
    public void clearFolderSynchronous_shouldOpenFolderForWriting() throws MessagingException {
        controller.clearFolderSynchronous(account, FOLDER_ID, listener);

        verify(localFolder).open(Folder.OPEN_MODE_RW);
    }

    @Test
    public void clearFolderSynchronous_shouldClearAllMessagesInTheFolder() throws MessagingException {
        controller.clearFolderSynchronous(account, FOLDER_ID, listener);

        verify(localFolder).clearAllMessages();
    }

    @Test
    public void clearFolderSynchronous_shouldCloseTheFolder() throws MessagingException {
        controller.clearFolderSynchronous(account, FOLDER_ID, listener);

        verify(localFolder, atLeastOnce()).close();
    }

    @Test(expected = UnavailableAccountException.class)
    public void clearFolderSynchronous_whenStorageUnavailable_shouldThrowUnavailableAccountException() throws MessagingException {
        doThrow(new UnavailableStorageException("Test")).when(localFolder).open(Folder.OPEN_MODE_RW);

        controller.clearFolderSynchronous(account, FOLDER_ID, listener);
    }

    @Test()
    public void clearFolderSynchronous_whenExceptionThrown_shouldAddErrorMessageInDebug() throws MessagingException {
        if (K9.isDebug()) {
            doThrow(new RuntimeException("Test")).when(localFolder).open(Folder.OPEN_MODE_RW);

            controller.clearFolderSynchronous(account, FOLDER_ID, listener);

            verify(errorFolder).appendMessages(any(List.class));
        }
    }

    @Test()
    public void clearFolderSynchronous_whenExceptionThrown_shouldStillCloseFolder() throws MessagingException {
        doThrow(new RuntimeException("Test")).when(localFolder).open(Folder.OPEN_MODE_RW);

        try {
            controller.clearFolderSynchronous(account, FOLDER_ID, listener);
        } catch (Exception ignored){
        }

        verify(localFolder, atLeastOnce()).close();
    }

    @Test()
    public void clearFolderSynchronous_shouldListFolders() throws MessagingException {
        controller.clearFolderSynchronous(account, FOLDER_ID, listener);

        verify(listener, atLeastOnce()).listFoldersStarted(account);
    }

    @Test
    public void listFoldersSynchronous_shouldNotifyTheListenerListingStarted() throws MessagingException {
        List<LocalFolder> folders = Collections.singletonList(localFolder);
        when(localStore.getFolders(false)).thenReturn(folders);

        controller.listFoldersSynchronous(account, false, listener);

        verify(listener).listFoldersStarted(account);
    }

    @Test
    public void listFoldersSynchronous_shouldNotifyTheListenerOfTheListOfFolders() throws MessagingException {
        List<LocalFolder> folders = Collections.singletonList(localFolder);
        when(localStore.getFolders(false)).thenReturn(folders);

        controller.listFoldersSynchronous(account, false, listener);

        verify(listener).listFolders(eq(account), localFolderListCaptor.capture());
        assertEquals(folders, localFolderListCaptor.getValue());
    }

    @Test
    public void listFoldersSynchronous_shouldNotifyFailureOnException() throws MessagingException {
        when(localStore.getFolders(false)).thenThrow(new MessagingException("Test"));

        controller.listFoldersSynchronous(account, true, listener);

        verify(listener).listFoldersFailed(account, "Test");
    }

    @Test
    public void listFoldersSynchronous_shouldNotNotifyFinishedAfterFailure() throws MessagingException {
        when(localStore.getFolders(false)).thenThrow(new MessagingException("Test"));

        controller.listFoldersSynchronous(account, true, listener);

        verify(listener, never()).listFoldersFinished(account);
    }

    @Test
    public void listFoldersSynchronous_shouldNotifyFinishedAfterSuccess() throws MessagingException {
        List<LocalFolder> folders = Collections.singletonList(localFolder);
        when(localStore.getFolders(false)).thenReturn(folders);

        controller.listFoldersSynchronous(account, false, listener);

        verify(listener).listFoldersFinished(account);
    }

    @Test
    public void refreshRemoteSynchronous_shouldCreateFoldersFromRemote() throws MessagingException {
        configureRemoteStoreWithFolder();
        LocalFolder newLocalFolder = mock(LocalFolder.class);

        List<Folder> folders = Collections.singletonList(remoteFolder);
        when(remoteStore.getFolders(false)).thenAnswer(createAnswer(folders));
        when(remoteFolder.getId()).thenReturn("NewFolder");
        when(localStore.getFolder("NewFolder")).thenReturn(newLocalFolder);

        controller.refreshRemoteSynchronous(account, listener);

        verify(localStore).createFolders(eq(Collections.singletonList(newLocalFolder)), anyInt());
    }

    @Test
    public void refreshRemoteSynchronous_shouldDeleteFoldersNotOnRemote() throws MessagingException {
        configureRemoteStoreWithFolder();
        LocalFolder oldLocalFolder = mock(LocalFolder.class);
        when(oldLocalFolder.getId()).thenReturn("OldLocalFolder");
        when(localStore.getFolders(false))
                .thenReturn(Collections.singletonList(oldLocalFolder));
        List<Folder> folders = Collections.emptyList();
        when(remoteStore.getFolders(false)).thenAnswer(createAnswer(folders));

        controller.refreshRemoteSynchronous(account, listener);

        verify(oldLocalFolder).delete(false);
    }

    @Test
    public void refreshRemoteSynchronous_shouldNotDeleteFoldersOnRemote() throws MessagingException {
        configureRemoteStoreWithFolder();
        when(localStore.getFolders(false))
                .thenReturn(Collections.singletonList(localFolder));
        List<Folder> folders = Collections.singletonList(remoteFolder);
        when(remoteStore.getFolders(false)).thenAnswer(createAnswer(folders));

        controller.refreshRemoteSynchronous(account, listener);

        verify(localFolder, never()).delete(false);
    }

    @Test
    public void refreshRemoteSynchronous_shouldNotDeleteSpecialFoldersNotOnRemote() throws MessagingException {
        configureRemoteStoreWithFolder();
        LocalFolder missingSpecialFolder = mock(LocalFolder.class);
        when(account.isSpecialFolder("Outbox")).thenReturn(true);
        when(missingSpecialFolder.getId()).thenReturn("Outbox");
        when(localStore.getFolders(false))
                .thenReturn(Collections.singletonList(missingSpecialFolder));
        List<Folder> folders = Collections.emptyList();
        when(remoteStore.getFolders(false)).thenAnswer(createAnswer(folders));

        controller.refreshRemoteSynchronous(account, listener);

        verify(missingSpecialFolder, never()).delete(false);
    }

    public static <T> Answer<T> createAnswer(final T value) {
        return new Answer<T>() {
            @Override
            public T answer(InvocationOnMock invocation) throws Throwable {
                return value;
            }
        };
    }

    @Test
    public void refreshRemoteSynchronous_shouldProvideFolderList() throws MessagingException {
        configureRemoteStoreWithFolder();
        List<LocalFolder> folders = Collections.singletonList(localFolder);
        when(localStore.getFolders(false)).thenReturn(folders);

        controller.refreshRemoteSynchronous(account, listener);

        verify(listener).listFolders(account, folders);
    }

    @Test
    public void refreshRemoteSynchronous_shouldNotifyFinishedAfterSuccess() throws MessagingException {
        configureRemoteStoreWithFolder();
        List<LocalFolder> folders = Collections.singletonList(localFolder);
        when(localStore.getFolders(false)).thenReturn(folders);

        controller.refreshRemoteSynchronous(account, listener);

        verify(listener).listFoldersFinished(account);
    }

    @Test
    public void refreshRemoteSynchronous_shouldNotNotifyFinishedAfterFailure() throws MessagingException {
        configureRemoteStoreWithFolder();
        when(localStore.getFolders(false)).thenThrow(new MessagingException("Test"));

        controller.refreshRemoteSynchronous(account, listener);

        verify(listener, never()).listFoldersFinished(account);
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
                eq((String) null), eq((String) null), eq(Collections.singletonList(localMessage)));
    }

    private void setupRemoteSearch() throws Exception {
        setAccountsInPreferences(Collections.singletonMap("1", account));
        configureRemoteStoreWithFolder();

        remoteMessages = new ArrayList<>();
        Collections.addAll(remoteMessages, remoteOldMessage, remoteNewMessage1, remoteNewMessage2);
        List<Message> newRemoteMessages = new ArrayList<>();
        Collections.addAll(newRemoteMessages, remoteNewMessage1, remoteNewMessage2);

        when(remoteOldMessage.getUid()).thenReturn("oldMessageUid");
        when(remoteNewMessage1.getUid()).thenReturn("newMessageUid1");
        when(localNewMessage1.getUid()).thenReturn("newMessageUid1");
        when(remoteNewMessage2.getUid()).thenReturn("newMessageUid2");
        when(localNewMessage2.getUid()).thenReturn("newMessageUid2");
        when(remoteFolder.search(anyString(), anySet(), anySet())).thenReturn(remoteMessages);
        when(localFolder.extractNewMessages(Matchers.<List<Message>>any())).thenReturn(newRemoteMessages);
        when(localFolder.getMessage("newMessageUid1")).thenReturn(localNewMessage1);
        when(localFolder.getMessage("newMessageUid2")).thenAnswer(
            new Answer<LocalMessage>() {
                @Override
                public LocalMessage answer(InvocationOnMock invocation) throws Throwable {
                    if(hasFetchedMessage) {
                        return localNewMessage2;
                    }
                    else
                        return null;
                }
            }
        );
        doAnswer(new Answer<Void>() {
             @Override
             public Void answer(InvocationOnMock invocation) throws Throwable {
                 hasFetchedMessage = true;
                 return null;
             }
        }).when(remoteFolder).fetch(
            Matchers.<List<Message>>eq(Collections.singletonList(remoteNewMessage2)),
            any(FetchProfile.class),
            Matchers.<MessageRetrievalListener>eq(null));
        reqFlags = Collections.singleton(Flag.ANSWERED);
        forbiddenFlags = Collections.singleton(Flag.DELETED);

        when(account.getRemoteSearchNumResults()).thenReturn(50);
    }

    @Test
    public void searchRemoteMessagesSynchronous_shouldNotifyStartedListingRemoteMessages() throws Exception {
        setupRemoteSearch();

        controller.searchRemoteMessagesSynchronous("1", FOLDER_ID, FOLDER_NAME, "query", reqFlags, forbiddenFlags, listener);

        verify(listener).remoteSearchStarted(FOLDER_ID, FOLDER_NAME);
    }

    @Test
    public void searchRemoteMessagesSynchronous_shouldQueryRemoteFolder() throws Exception {
        setupRemoteSearch();

        controller.searchRemoteMessagesSynchronous("1", FOLDER_ID, FOLDER_NAME, "query", reqFlags, forbiddenFlags, listener);

        verify(remoteFolder).search("query", reqFlags, forbiddenFlags);
    }

    @Test
    public void searchRemoteMessagesSynchronous_shouldAskLocalFolderToDetermineNewMessages() throws Exception {
        setupRemoteSearch();

        controller.searchRemoteMessagesSynchronous("1", FOLDER_ID, FOLDER_NAME, "query", reqFlags, forbiddenFlags, listener);

        verify(localFolder).extractNewMessages(remoteMessages);
    }

    @Test
    public void searchRemoteMessagesSynchronous_shouldTryAndGetNewMessages() throws Exception {
        setupRemoteSearch();

        controller.searchRemoteMessagesSynchronous("1", FOLDER_ID, FOLDER_NAME, "query", reqFlags, forbiddenFlags, listener);

        verify(localFolder).getMessage("newMessageUid1");
    }

    @Test
    public void searchRemoteMessagesSynchronous_shouldNotTryAndGetOldMessages() throws Exception {
        setupRemoteSearch();

        controller.searchRemoteMessagesSynchronous("1", FOLDER_ID, FOLDER_NAME, "query", reqFlags, forbiddenFlags, listener);

        verify(localFolder, never()).getMessage("oldMessageUid");
    }

    @Test
    public void searchRemoteMessagesSynchronous_shouldFetchNewMessages() throws Exception {
        setupRemoteSearch();

        controller.searchRemoteMessagesSynchronous("1", FOLDER_ID, FOLDER_NAME, "query", reqFlags, forbiddenFlags, listener);

        verify(remoteFolder, times(2)).fetch(eq(Collections.singletonList(remoteNewMessage2)),
                fetchProfileCaptor.capture(), Matchers.<MessageRetrievalListener>eq(null));
    }

    @Test
    public void searchRemoteMessagesSynchronous_shouldNotFetchExistingMessages() throws Exception {
        setupRemoteSearch();

        controller.searchRemoteMessagesSynchronous("1", FOLDER_ID, FOLDER_NAME, "query", reqFlags, forbiddenFlags, listener);

        verify(remoteFolder, never()).fetch(eq(Collections.singletonList(remoteNewMessage1)),
                fetchProfileCaptor.capture(), Matchers.<MessageRetrievalListener>eq(null));
    }

    @Test
    public void searchRemoteMessagesSynchronous_shouldNotifyOnFailure() throws Exception {
        setupRemoteSearch();
        when(account.getRemoteStore()).thenThrow(new MessagingException("Test"));

        controller.searchRemoteMessagesSynchronous("1", FOLDER_ID, FOLDER_NAME, "query", reqFlags, forbiddenFlags, listener);

        verify(listener).remoteSearchFailed(null, null, "Test");
    }

    @Test
    public void searchRemoteMessagesSynchronous_shouldNotifyOnFinish() throws Exception {
        setupRemoteSearch();
        when(account.getRemoteStore()).thenThrow(new MessagingException("Test"));

        controller.searchRemoteMessagesSynchronous("1", FOLDER_ID, FOLDER_NAME, "query", reqFlags, forbiddenFlags, listener);

        verify(listener).remoteSearchFinished(FOLDER_ID, FOLDER_NAME, 0, 50, Collections.<Message>emptyList());
    }

    @Test
    public void sendPendingMessagesSynchronous_withNonExistentOutbox_shouldNotStartSync() throws MessagingException {
        when(account.getOutboxFolderId()).thenReturn(FOLDER_ID);
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

        verify(listener).synchronizeMailboxProgress(account, "SentID", "Sent", 0, 1);
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

        verify(listener).synchronizeMailboxProgress(account, "SentID", "Sent", 1, 1);
    }

    @Test
    public void sendPendingMessagesSynchronous_shouldUpdateProgress() throws MessagingException {
        setupAccountWithMessageToSend();

        controller.sendPendingMessagesSynchronous(account);

        verify(listener).synchronizeMailboxProgress(account, "SentID", "Sent", 1, 1);
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
    public void synchronizeMailboxSynchronous_withOneMessageInRemoteFolder_shouldFinishWithoutError()
            throws Exception {
        messageCountInRemoteFolder(1);

        controller.synchronizeMailboxSynchronous(account, FOLDER_ID, FOLDER_NAME, listener, remoteFolder);

        verify(listener).synchronizeMailboxFinished(account, FOLDER_ID, FOLDER_NAME, 1, 0);
    }

    @Test
    public void synchronizeMailboxSynchronous_withEmptyRemoteFolder_shouldFinishWithoutError()
            throws Exception {
        messageCountInRemoteFolder(0);

        controller.synchronizeMailboxSynchronous(account, FOLDER_ID, FOLDER_NAME, listener, remoteFolder);

        verify(listener).synchronizeMailboxFinished(account, FOLDER_ID, FOLDER_NAME, 0, 0);
    }

    @Test
    public void synchronizeMailboxSynchronous_withNegativeMessageCountInRemoteFolder_shouldFinishWithError()
            throws Exception {
        messageCountInRemoteFolder(-1);

        controller.synchronizeMailboxSynchronous(account, FOLDER_ID, FOLDER_NAME, listener, remoteFolder);

        verify(listener).synchronizeMailboxFailed(account, FOLDER_ID, FOLDER_NAME,
                "Exception: Message count -1 for folder FolderID");
    }

    @Test
    public void synchronizeMailboxSynchronous_withRemoteFolderProvided_shouldNotOpenRemoteFolder() throws Exception {
        messageCountInRemoteFolder(1);

        controller.synchronizeMailboxSynchronous(account, FOLDER_ID, FOLDER_NAME, listener, remoteFolder);

        verify(remoteFolder, never()).open(Folder.OPEN_MODE_RW);
    }

    @Test
    public void synchronizeMailboxSynchronous_withNoRemoteFolderProvided_shouldOpenRemoteFolderFromStore()
            throws Exception {
        messageCountInRemoteFolder(1);
        configureRemoteStoreWithFolder();

        controller.synchronizeMailboxSynchronous(account, FOLDER_ID, FOLDER_NAME, listener, null);

        verify(remoteFolder).open(Folder.OPEN_MODE_RW);
    }

    @Test
    public void synchronizeMailboxSynchronous_withRemoteFolderProvided_shouldNotCloseRemoteFolder() throws Exception {
        messageCountInRemoteFolder(1);

        controller.synchronizeMailboxSynchronous(account, FOLDER_ID, FOLDER_NAME, listener, remoteFolder);

        verify(remoteFolder, never()).close();
    }

    @Test
    public void synchronizeMailboxSynchronous_withNoRemoteFolderProvided_shouldCloseRemoteFolderFromStore()
            throws Exception {
        messageCountInRemoteFolder(1);
        configureRemoteStoreWithFolder();

        controller.synchronizeMailboxSynchronous(account, FOLDER_ID, FOLDER_NAME, listener, null);

        verify(remoteFolder).close();
    }

    @Test
    public void synchronizeMailboxSynchronous_withAccountPolicySetToExpungeOnPoll_shouldExpungeRemoteFolder()
            throws Exception {
        messageCountInRemoteFolder(1);
        when(account.getExpungePolicy()).thenReturn(Account.Expunge.EXPUNGE_ON_POLL);
        configureRemoteStoreWithFolder();

        controller.synchronizeMailboxSynchronous(account, FOLDER_ID, FOLDER_NAME, listener, null);

        verify(remoteFolder).expunge();
    }

    @Test
    public void synchronizeMailboxSynchronous_withAccountPolicySetToExpungeManually_shouldNotExpungeRemoteFolder()
            throws Exception {
        messageCountInRemoteFolder(1);
        when(account.getExpungePolicy()).thenReturn(Account.Expunge.EXPUNGE_MANUALLY);

        controller.synchronizeMailboxSynchronous(account, FOLDER_ID, FOLDER_NAME, listener, null);

        verify(remoteFolder, never()).expunge();
    }

    @Test
    public void synchronizeMailboxSynchronous_withAccountSetToSyncRemoteDeletions_shouldDeleteLocalCopiesOfDeletedMessages()
            throws Exception {
        messageCountInRemoteFolder(0);
        LocalMessage localCopyOfRemoteDeletedMessage = mock(LocalMessage.class);
        when(account.syncRemoteDeletions()).thenReturn(true);
        when(localFolder.getAllMessagesAndEffectiveDates()).thenReturn(Collections.singletonMap(MESSAGE_UID1, 0L));
        when(localFolder.getMessagesByUids(any(List.class)))
                .thenReturn(Collections.singletonList(localCopyOfRemoteDeletedMessage));

        controller.synchronizeMailboxSynchronous(account, FOLDER_ID, FOLDER_NAME, listener, remoteFolder);

        verify(localFolder).destroyMessages(messageListCaptor.capture());
        assertEquals(localCopyOfRemoteDeletedMessage, messageListCaptor.getValue().get(0));
    }

    @Test
    public void synchronizeMailboxSynchronous_withAccountSetToSyncRemoteDeletions_shouldNotDeleteLocalCopiesOfExistingMessagesAfterEarliestPollDate()
            throws Exception {
        messageCountInRemoteFolder(1);
        Date dateOfEarliestPoll = new Date();
        LocalMessage localMessage = localMessageWithCopyOnServer();
        when(account.syncRemoteDeletions()).thenReturn(true);
        when(account.getEarliestPollDate()).thenReturn(dateOfEarliestPoll);
        when(localMessage.olderThan(dateOfEarliestPoll)).thenReturn(false);
        when(localFolder.getMessages(null)).thenReturn(Collections.singletonList(localMessage));

        controller.synchronizeMailboxSynchronous(account, FOLDER_ID, FOLDER_NAME, listener, remoteFolder);

        verify(localFolder, never()).destroyMessages(messageListCaptor.capture());
    }

    @Test
    public void synchronizeMailboxSynchronous_withAccountSetToSyncRemoteDeletions_shouldDeleteLocalCopiesOfExistingMessagesBeforeEarliestPollDate()
            throws Exception {
        messageCountInRemoteFolder(1);
        LocalMessage localMessage = localMessageWithCopyOnServer();
        Date dateOfEarliestPoll = new Date();
        when(account.syncRemoteDeletions()).thenReturn(true);
        when(account.getEarliestPollDate()).thenReturn(dateOfEarliestPoll);
        when(localMessage.olderThan(dateOfEarliestPoll)).thenReturn(true);
        when(localFolder.getAllMessagesAndEffectiveDates()).thenReturn(Collections.singletonMap(MESSAGE_UID1, 0L));
        when(localFolder.getMessagesByUids(any(List.class))).thenReturn(Collections.singletonList(localMessage));

        controller.synchronizeMailboxSynchronous(account, FOLDER_ID, FOLDER_NAME, listener, remoteFolder);

        verify(localFolder).destroyMessages(messageListCaptor.capture());
        assertEquals(localMessage, messageListCaptor.getValue().get(0));
    }

    @Test
    public void synchronizeMailboxSynchronous_withAccountSetNotToSyncRemoteDeletions_shouldNotDeleteLocalCopiesOfMessages()
            throws Exception {
        messageCountInRemoteFolder(0);
        LocalMessage remoteDeletedMessage = mock(LocalMessage.class);
        when(account.syncRemoteDeletions()).thenReturn(false);
        when(localFolder.getMessages(null)).thenReturn(Collections.singletonList(remoteDeletedMessage));

        controller.synchronizeMailboxSynchronous(account, FOLDER_ID, FOLDER_NAME, listener, remoteFolder);

        verify(localFolder, never()).destroyMessages(messageListCaptor.capture());
    }

    @Test
    public void synchronizeMailboxSynchronous_withAccountSupportingFetchingFlags_shouldFetchUnsychronizedMessagesListAndFlags()
            throws Exception {
        messageCountInRemoteFolder(1);
        hasUnsyncedRemoteMessage();
        when(remoteFolder.supportsFetchingFlags()).thenReturn(true);

        controller.synchronizeMailboxSynchronous(account, FOLDER_ID, FOLDER_NAME, listener, remoteFolder);

        verify(remoteFolder, atLeastOnce()).fetch(any(List.class), fetchProfileCaptor.capture(),
                any(MessageRetrievalListener.class));
        assertTrue(fetchProfileCaptor.getAllValues().get(0).contains(FetchProfile.Item.FLAGS));
        assertTrue(fetchProfileCaptor.getAllValues().get(0).contains(FetchProfile.Item.ENVELOPE));
        assertEquals(2, fetchProfileCaptor.getAllValues().get(0).size());
    }

    @Test
    public void synchronizeMailboxSynchronous_withAccountNotSupportingFetchingFlags_shouldFetchUnsychronizedMessages()
            throws Exception {
        messageCountInRemoteFolder(1);
        hasUnsyncedRemoteMessage();
        when(remoteFolder.supportsFetchingFlags()).thenReturn(false);

        controller.synchronizeMailboxSynchronous(account, FOLDER_ID, FOLDER_NAME, listener, remoteFolder);

        verify(remoteFolder, atLeastOnce()).fetch(any(List.class), fetchProfileCaptor.capture(),
                any(MessageRetrievalListener.class));
        assertEquals(1, fetchProfileCaptor.getAllValues().get(0).size());
        assertTrue(fetchProfileCaptor.getAllValues().get(0).contains(FetchProfile.Item.ENVELOPE));
    }

    @Test
    public void synchronizeMailboxSynchronous_withUnsyncedNewSmallMessage_shouldFetchBodyOfSmallMessage()
            throws Exception {
        Message smallMessage = buildSmallNewMessage();
        messageCountInRemoteFolder(1);
        hasUnsyncedRemoteMessage();
        when(remoteFolder.supportsFetchingFlags()).thenReturn(false);
        respondToFetchEnvelopesWithMessage(smallMessage);

        controller.synchronizeMailboxSynchronous(account, FOLDER_ID, FOLDER_NAME, listener, remoteFolder);

        verify(remoteFolder, atLeast(2)).fetch(any(List.class), fetchProfileCaptor.capture(),
                any(MessageRetrievalListener.class));
        assertEquals(1, fetchProfileCaptor.getAllValues().get(1).size());
        assertTrue(fetchProfileCaptor.getAllValues().get(1).contains(FetchProfile.Item.BODY));
    }

    @Test
    public void synchronizeMailboxSynchronous_withUnsyncedNewSmallMessage_shouldFetchStructureAndLimitedBodyOfLargeMessage()
            throws Exception {
        Message largeMessage = buildLargeNewMessage();
        messageCountInRemoteFolder(1);
        hasUnsyncedRemoteMessage();
        when(remoteFolder.supportsFetchingFlags()).thenReturn(false);
        respondToFetchEnvelopesWithMessage(largeMessage);

        controller.synchronizeMailboxSynchronous(account, FOLDER_ID, FOLDER_NAME, listener, remoteFolder);

        //TODO: Don't bother fetching messages of a size we don't have
        verify(remoteFolder, atLeast(4)).fetch(any(List.class), fetchProfileCaptor.capture(),
                any(MessageRetrievalListener.class));
        assertEquals(1, fetchProfileCaptor.getAllValues().get(2).size());
        assertEquals(FetchProfile.Item.STRUCTURE, fetchProfileCaptor.getAllValues().get(2).get(0));
        assertEquals(1, fetchProfileCaptor.getAllValues().get(3).size());
        assertEquals(FetchProfile.Item.BODY_SANE, fetchProfileCaptor.getAllValues().get(3).get(0));
    }

    private void setupAccountWithMessageToSend() throws MessagingException {
        when(account.getOutboxFolderId()).thenReturn(FOLDER_ID);
        when(account.hasSentFolder()).thenReturn(true);
        when(account.getSentFolderId()).thenReturn(SENT_FOLDER_ID);
        when(localStore.getFolder(SENT_FOLDER_ID)).thenReturn(sentFolder);
        when(sentFolder.getDatabaseId()).thenReturn(1L);
        when(sentFolder.getName()).thenReturn(SENT_FOLDER_NAME);
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
        when(account.getErrorFolderId()).thenReturn(K9.ERROR_FOLDER_ID);
        when(account.getEmail()).thenReturn("user@host.com");
    }

    private void configureLocalStore() throws MessagingException {
        when(localStore.getFolder(FOLDER_ID)).thenReturn(localFolder);
        when(localFolder.getId()).thenReturn(FOLDER_ID);
        when(localStore.getFolder(K9.ERROR_FOLDER_ID)).thenReturn(errorFolder);
        when(localStore.getFolders(false)).thenReturn(Collections.singletonList(localFolder));
    }

    private void configureRemoteStoreWithFolder() throws MessagingException {
        when(account.getRemoteStore()).thenReturn(remoteStore);
        when(remoteStore.getFolder(FOLDER_ID)).thenReturn(remoteFolder);
        when(remoteFolder.getId()).thenReturn(FOLDER_ID);
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
