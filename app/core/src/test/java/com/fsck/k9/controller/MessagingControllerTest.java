package com.fsck.k9.controller;


import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.Context;

import com.fsck.k9.Account;
import com.fsck.k9.Account.SpecialFolderSelection;
import com.fsck.k9.AccountPreferenceSerializer;
import com.fsck.k9.CoreResourceProvider;
import com.fsck.k9.DI;
import com.fsck.k9.K9;
import com.fsck.k9.K9RobolectricTest;
import com.fsck.k9.Preferences;
import com.fsck.k9.backend.BackendManager;
import com.fsck.k9.backend.api.Backend;
import com.fsck.k9.helper.Contacts;
import com.fsck.k9.mail.AuthenticationFailedException;
import com.fsck.k9.mail.CertificateValidationException;
import com.fsck.k9.mail.FetchProfile;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.MessageRetrievalListener;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.mailstore.LocalStore;
import com.fsck.k9.mailstore.LocalStoreProvider;
import com.fsck.k9.mailstore.OutboxState;
import com.fsck.k9.mailstore.OutboxStateRepository;
import com.fsck.k9.mailstore.SendState;
import com.fsck.k9.mailstore.UnavailableStorageException;
import com.fsck.k9.notification.NotificationController;
import com.fsck.k9.notification.NotificationStrategy;
import com.fsck.k9.search.LocalSearch;
import com.fsck.k9.search.SearchAccount;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowLog;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;


@SuppressWarnings("unchecked")
public class MessagingControllerTest extends K9RobolectricTest {
    private static final String FOLDER_NAME = "Folder";
    private static final String SENT_FOLDER_NAME = "Sent";
    private static final int MAXIMUM_SMALL_MESSAGE_SIZE = 1000;
    private static final String ACCOUNT_UUID = "1";

    private MessagingController controller;
    private Account account;
    @Mock
    private BackendManager backendManager;
    @Mock
    private Backend backend;
    @Mock
    private LocalStoreProvider localStoreProvider;
    @Mock
    private Contacts contacts;
    @Mock
    private SimpleMessagingListener listener;
    @Mock
    private LocalSearch search;
    @Mock
    private LocalFolder localFolder;
    @Mock
    private LocalFolder sentFolder;
    @Mock
    private LocalStore localStore;
    @Mock
    private NotificationController notificationController;
    @Mock
    private NotificationStrategy notificationStrategy;
    @Captor
    private ArgumentCaptor<FetchProfile> fetchProfileCaptor;
    @Captor
    private ArgumentCaptor<MessageRetrievalListener<LocalMessage>> messageRetrievalListenerCaptor;

    private Context appContext;
    private Set<Flag> reqFlags;
    private Set<Flag> forbiddenFlags;

    private List<String> remoteMessages;
    @Mock
    private LocalMessage localNewMessage1;
    @Mock
    private LocalMessage localNewMessage2;
    @Mock
    private LocalMessage localMessageToSend1;
    private volatile boolean hasFetchedMessage = false;

    private UnreadMessageCountProvider unreadMessageCountProvider = new UnreadMessageCountProvider() {
        @Override
        public int getUnreadMessageCount(@NotNull SearchAccount searchAccount) {
            return 0;
        }

        @Override
        public int getUnreadMessageCount(@NotNull Account account) {
            return 0;
        }
    };


    @Before
    public void setUp() throws MessagingException {
        ShadowLog.stream = System.out;
        MockitoAnnotations.initMocks(this);
        appContext = RuntimeEnvironment.application;

        controller = new MessagingController(appContext, notificationController, notificationStrategy,
                localStoreProvider, contacts,
                unreadMessageCountProvider, mock(CoreResourceProvider.class), backendManager,
                Collections.<ControllerExtension>emptyList());

        configureAccount();
        configureBackendManager();
        configureLocalStore();
    }

    @After
    public void tearDown() throws Exception {
        removeAccountsFromPreferences();
        controller.stop();
        autoClose();
    }

    @Test
    public void clearFolderSynchronous_shouldOpenFolderForWriting() throws MessagingException {
        controller.clearFolderSynchronous(account, FOLDER_NAME, listener);

        verify(localFolder).open();
    }

    @Test
    public void clearFolderSynchronous_shouldClearAllMessagesInTheFolder() throws MessagingException {
        controller.clearFolderSynchronous(account, FOLDER_NAME, listener);

        verify(localFolder).clearAllMessages();
    }

    @Test
    public void clearFolderSynchronous_shouldCloseTheFolder() throws MessagingException {
        controller.clearFolderSynchronous(account, FOLDER_NAME, listener);

        verify(localFolder, atLeastOnce()).close();
    }

    @Test(expected = UnavailableAccountException.class)
    public void clearFolderSynchronous_whenStorageUnavailable_shouldThrowUnavailableAccountException() throws MessagingException {
        doThrow(new UnavailableStorageException("Test")).when(localFolder).open();

        controller.clearFolderSynchronous(account, FOLDER_NAME, listener);
    }

    @Test()
    public void clearFolderSynchronous_whenExceptionThrown_shouldStillCloseFolder() throws MessagingException {
        doThrow(new RuntimeException("Test")).when(localFolder).open();

        try {
            controller.clearFolderSynchronous(account, FOLDER_NAME, listener);
        } catch (Exception ignored){
        }

        verify(localFolder, atLeastOnce()).close();
    }

    @Test
    public void refreshRemoteSynchronous_shouldCallBackend() throws MessagingException {
        controller.refreshFolderListSynchronous(account);

        verify(backend).refreshFolderList();
    }

    @Test
    public void searchLocalMessagesSynchronous_shouldCallSearchForMessagesOnLocalStore()
            throws Exception {
        setAccountsInPreferences(Collections.singletonMap(ACCOUNT_UUID, account));
        when(search.searchAllAccounts()).thenReturn(true);
        when(search.getAccountUuids()).thenReturn(new String[0]);

        controller.searchLocalMessagesSynchronous(search, listener);

        verify(localStore).searchForMessages(nullable(MessageRetrievalListener.class), eq(search));
    }

    @Test
    public void searchLocalMessagesSynchronous_shouldNotifyWhenStoreFinishesRetrievingAMessage()
            throws Exception {
        setAccountsInPreferences(Collections.singletonMap(ACCOUNT_UUID, account));
        LocalMessage localMessage = mock(LocalMessage.class);
        when(localMessage.getFolder()).thenReturn(localFolder);
        when(search.searchAllAccounts()).thenReturn(true);
        when(search.getAccountUuids()).thenReturn(new String[0]);
        when(localStore.searchForMessages(nullable(MessageRetrievalListener.class), eq(search)))
                .thenThrow(new MessagingException("Test"));

        controller.searchLocalMessagesSynchronous(search, listener);

        verify(localStore).searchForMessages(messageRetrievalListenerCaptor.capture(), eq(search));
        messageRetrievalListenerCaptor.getValue().messageFinished(localMessage, 1, 1);
        verify(listener).listLocalMessagesAddMessages(eq(account),
                eq((String) null), eq(Collections.singletonList(localMessage)));
    }

    private void setupRemoteSearch() throws Exception {
        setAccountsInPreferences(Collections.singletonMap(ACCOUNT_UUID, account));

        remoteMessages = new ArrayList<>();
        Collections.addAll(remoteMessages, "oldMessageUid", "newMessageUid1", "newMessageUid2");
        List<String> newRemoteMessages = new ArrayList<>();
        Collections.addAll(newRemoteMessages, "newMessageUid1", "newMessageUid2");

        when(localNewMessage1.getUid()).thenReturn("newMessageUid1");
        when(localNewMessage2.getUid()).thenReturn("newMessageUid2");
        when(backend.search(eq(FOLDER_NAME), anyString(), nullable(Set.class), nullable(Set.class)))
                .thenReturn(remoteMessages);
        when(localFolder.extractNewMessages(ArgumentMatchers.<String>anyList())).thenReturn(newRemoteMessages);
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
        }).when(backend).fetchMessage(
            eq(FOLDER_NAME),
            eq("newMessageUid2"),
            any(FetchProfile.class));
        reqFlags = Collections.singleton(Flag.ANSWERED);
        forbiddenFlags = Collections.singleton(Flag.DELETED);

        account.setRemoteSearchNumResults(50);
    }

    @Test
    public void searchRemoteMessagesSynchronous_shouldNotifyStartedListingRemoteMessages() throws Exception {
        setupRemoteSearch();

        controller.searchRemoteMessagesSynchronous(ACCOUNT_UUID, FOLDER_NAME, "query", reqFlags, forbiddenFlags, listener);

        verify(listener).remoteSearchStarted(FOLDER_NAME);
    }

    @Test
    public void searchRemoteMessagesSynchronous_shouldQueryRemoteFolder() throws Exception {
        setupRemoteSearch();

        controller.searchRemoteMessagesSynchronous(ACCOUNT_UUID, FOLDER_NAME, "query", reqFlags, forbiddenFlags, listener);

        verify(backend).search(FOLDER_NAME, "query", reqFlags, forbiddenFlags);
    }

    @Test
    public void searchRemoteMessagesSynchronous_shouldAskLocalFolderToDetermineNewMessages() throws Exception {
        setupRemoteSearch();

        controller.searchRemoteMessagesSynchronous(ACCOUNT_UUID, FOLDER_NAME, "query", reqFlags, forbiddenFlags, listener);

        verify(localFolder).extractNewMessages(remoteMessages);
    }

    @Test
    public void searchRemoteMessagesSynchronous_shouldTryAndGetNewMessages() throws Exception {
        setupRemoteSearch();

        controller.searchRemoteMessagesSynchronous(ACCOUNT_UUID, FOLDER_NAME, "query", reqFlags, forbiddenFlags, listener);

        verify(localFolder).getMessage("newMessageUid1");
    }

    @Test
    public void searchRemoteMessagesSynchronous_shouldNotTryAndGetOldMessages() throws Exception {
        setupRemoteSearch();

        controller.searchRemoteMessagesSynchronous(ACCOUNT_UUID, FOLDER_NAME, "query", reqFlags, forbiddenFlags, listener);

        verify(localFolder, never()).getMessage("oldMessageUid");
    }

    @Test
    public void searchRemoteMessagesSynchronous_shouldFetchNewMessages() throws Exception {
        setupRemoteSearch();

        controller.searchRemoteMessagesSynchronous(ACCOUNT_UUID, FOLDER_NAME, "query", reqFlags, forbiddenFlags, listener);

        verify(backend).fetchMessage(eq(FOLDER_NAME), eq("newMessageUid2"), fetchProfileCaptor.capture());
    }

    @Test
    public void searchRemoteMessagesSynchronous_shouldNotFetchExistingMessages() throws Exception {
        setupRemoteSearch();

        controller.searchRemoteMessagesSynchronous(ACCOUNT_UUID, FOLDER_NAME, "query", reqFlags, forbiddenFlags, listener);

        verify(backend, never()).fetchMessage(eq(FOLDER_NAME), eq("newMessageUid1"), fetchProfileCaptor.capture());
    }

    @Test
    public void searchRemoteMessagesSynchronous_shouldNotifyOnFailure() throws Exception {
        setupRemoteSearch();
        when(backend.search(anyString(), anyString(), nullable(Set.class), nullable(Set.class)))
                .thenThrow(new MessagingException("Test"));

        controller.searchRemoteMessagesSynchronous(ACCOUNT_UUID, FOLDER_NAME, "query", reqFlags, forbiddenFlags, listener);

        verify(listener).remoteSearchFailed(null, "Test");
    }

    @Test
    public void searchRemoteMessagesSynchronous_shouldNotifyOnFinish() throws Exception {
        setupRemoteSearch();
        when(backend.search(anyString(), nullable(String.class), nullable(Set.class), nullable(Set.class)))
                .thenThrow(new MessagingException("Test"));

        controller.searchRemoteMessagesSynchronous(ACCOUNT_UUID, FOLDER_NAME, "query", reqFlags, forbiddenFlags, listener);

        verify(listener).remoteSearchFinished(FOLDER_NAME, 0, 50, Collections.<String>emptyList());
    }

    @Test
    public void sendPendingMessagesSynchronous_withNonExistentOutbox_shouldNotStartSync() throws MessagingException {
        when(account.getOutboxFolder()).thenReturn(FOLDER_NAME);
        when(localFolder.exists()).thenReturn(false);
        controller.addListener(listener);

        controller.sendPendingMessagesSynchronous(account);

        verifyZeroInteractions(listener);
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

        verify(backend).sendMessage(localMessageToSend1);
    }

    @Test
    public void sendPendingMessagesSynchronous_shouldSetAndRemoveSendInProgressFlag() throws MessagingException {
        setupAccountWithMessageToSend();

        controller.sendPendingMessagesSynchronous(account);

        InOrder ordering = inOrder(localMessageToSend1, backend);
        ordering.verify(localMessageToSend1).setFlag(Flag.X_SEND_IN_PROGRESS, true);
        ordering.verify(backend).sendMessage(localMessageToSend1);
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
        doThrow(new AuthenticationFailedException("Test")).when(backend).sendMessage(localMessageToSend1);

        controller.sendPendingMessagesSynchronous(account);

        verify(notificationController).showAuthenticationErrorNotification(account, false);
    }

    @Test
    public void sendPendingMessagesSynchronous_withCertificateFailure_shouldNotify() throws MessagingException {
        setupAccountWithMessageToSend();
        doThrow(new CertificateValidationException("Test")).when(backend).sendMessage(localMessageToSend1);

        controller.sendPendingMessagesSynchronous(account);

        verify(notificationController).showCertificateErrorNotification(account, false);
    }

    private void setupAccountWithMessageToSend() throws MessagingException {
        when(account.getOutboxFolder()).thenReturn(FOLDER_NAME);
        account.setSentFolder(SENT_FOLDER_NAME, SpecialFolderSelection.AUTOMATIC);
        when(localStore.getFolder(SENT_FOLDER_NAME)).thenReturn(sentFolder);
        when(sentFolder.getDatabaseId()).thenReturn(1L);
        when(localFolder.exists()).thenReturn(true);
        when(localFolder.getMessages(null)).thenReturn(Collections.singletonList(localMessageToSend1));
        when(localMessageToSend1.getUid()).thenReturn("localMessageToSend1");
        when(localMessageToSend1.getDatabaseId()).thenReturn(42L);
        when(localMessageToSend1.getHeader(K9.IDENTITY_HEADER)).thenReturn(new String[]{});

        OutboxState outboxState = new OutboxState(SendState.READY, 0, null, 0);
        OutboxStateRepository outboxStateRepository = mock(OutboxStateRepository.class);
        when(outboxStateRepository.getOutboxState(42L)).thenReturn(outboxState);

        when(localStore.getOutboxStateRepository()).thenReturn(outboxStateRepository);
        controller.addListener(listener);
    }

    private void configureBackendManager() {
        when(backendManager.getBackend(account)).thenReturn(backend);
    }

    private void configureAccount() throws MessagingException {
        // TODO use simple account object without mocks
        account = spy(new Account(ACCOUNT_UUID));
        DI.get(AccountPreferenceSerializer.class).loadDefaults(account);
        account.setMaximumAutoDownloadMessageSize(MAXIMUM_SMALL_MESSAGE_SIZE);
        account.setEmail("user@host.com");
        Mockito.doReturn(true).when(account).isAvailable(appContext);
    }

    private void configureLocalStore() throws MessagingException {
        when(localStore.getFolder(FOLDER_NAME)).thenReturn(localFolder);
        when(localFolder.getServerId()).thenReturn(FOLDER_NAME);
        when(localStore.getPersonalNamespaces(false)).thenReturn(Collections.singletonList(localFolder));
        when(localStoreProvider.getInstance(account)).thenReturn(localStore);
    }

    private void setAccountsInPreferences(Map<String, Account> newAccounts)
            throws Exception {
        // TODO this affects other tests, try to get rid of it
        Field accounts = Preferences.class.getDeclaredField("accounts");
        accounts.setAccessible(true);
        accounts.set(Preferences.getPreferences(appContext), newAccounts);

        Field accountsInOrder = Preferences.class.getDeclaredField("accountsInOrder");
        accountsInOrder.setAccessible(true);
        ArrayList<Account> newAccountsInOrder = new ArrayList<>(newAccounts.values());
        accountsInOrder.set(Preferences.getPreferences(appContext), newAccountsInOrder);
    }

    private void removeAccountsFromPreferences() {
        Preferences.getPreferences(appContext).clearAccounts();
    }
}
