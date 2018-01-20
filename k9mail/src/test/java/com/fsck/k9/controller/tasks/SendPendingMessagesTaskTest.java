package com.fsck.k9.controller.tasks;


import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import android.content.Context;

import com.fsck.k9.Account;
import com.fsck.k9.AccountStats;
import com.fsck.k9.K9;
import com.fsck.k9.K9RobolectricTestRunner;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.controller.MessagingListener;
import com.fsck.k9.controller.SimpleMessagingListener;
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
import com.fsck.k9.notification.NotificationController;
import com.fsck.k9.search.LocalSearch;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowLog;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;


@RunWith(K9RobolectricTestRunner.class)
public class SendPendingMessagesTaskTest {
    private static final String FOLDER_NAME = "Folder";
    private static final String SENT_FOLDER_NAME = "Sent";
    private static final int MAXIMUM_SMALL_MESSAGE_SIZE = 1000;
    private static final String MESSAGE_UID1 = "message-uid1";

    @Mock
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
    private ConcurrentHashMap<String, AtomicInteger> sendCount = new ConcurrentHashMap<>();
    private Set<MessagingListener> listeners = new HashSet<>();


    @Before
    public void setUp() throws MessagingException {
        ShadowLog.stream = System.out;
        MockitoAnnotations.initMocks(this);
        appContext = ShadowApplication.getInstance().getApplicationContext();

        configureAccount();
        configureLocalStore();
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
        listeners.add(listener);
    }

    @Test
    public void sendPendingMessagesSynchronous_withNonExistentOutbox_shouldNotStartSync() throws MessagingException {
        when(account.getOutboxFolderName()).thenReturn(FOLDER_NAME);
        when(localFolder.exists()).thenReturn(false);
        listeners.add(listener);

        new SendPendingMessagesTask(controller, appContext, notificationController,
                transportProvider, sendCount, account, listeners).sendPendingMessagesSynchronous();

        verifyZeroInteractions(listener);
    }

    @Test
    public void sendPendingMessagesSynchronous_shouldCallListenerOnStart() throws MessagingException {
        setupAccountWithMessageToSend();

        new SendPendingMessagesTask(controller, appContext, notificationController,
                transportProvider, sendCount, account, listeners).sendPendingMessagesSynchronous();

        verify(listener).sendPendingMessagesStarted(account);
    }

    @Test
    public void sendPendingMessagesSynchronous_shouldSetProgress() throws MessagingException {
        setupAccountWithMessageToSend();

        new SendPendingMessagesTask(controller, appContext, notificationController,
                transportProvider, sendCount, account, listeners).sendPendingMessagesSynchronous();

        verify(listener).synchronizeMailboxProgress(account, "Sent", 0, 1);
    }

    @Test
    public void sendPendingMessagesSynchronous_shouldSendMessageUsingTransport() throws MessagingException {
        setupAccountWithMessageToSend();

        new SendPendingMessagesTask(controller, appContext, notificationController,
                transportProvider, sendCount, account, listeners).sendPendingMessagesSynchronous();

        verify(transport).sendMessage(localMessageToSend1);
    }

    @Test
    public void sendPendingMessagesSynchronous_shouldSetAndRemoveSendInProgressFlag() throws MessagingException {
        setupAccountWithMessageToSend();

        new SendPendingMessagesTask(controller, appContext, notificationController,
                transportProvider, sendCount, account, listeners).sendPendingMessagesSynchronous();

        InOrder ordering = inOrder(localMessageToSend1, transport);
        ordering.verify(localMessageToSend1).setFlag(Flag.X_SEND_IN_PROGRESS, true);
        ordering.verify(transport).sendMessage(localMessageToSend1);
        ordering.verify(localMessageToSend1).setFlag(Flag.X_SEND_IN_PROGRESS, false);
    }

    @Test
    public void sendPendingMessagesSynchronous_shouldMarkSentMessageAsSeen() throws MessagingException {
        setupAccountWithMessageToSend();

        new SendPendingMessagesTask(controller, appContext, notificationController,
                transportProvider, sendCount, account, listeners).sendPendingMessagesSynchronous();

        verify(localMessageToSend1).setFlag(Flag.SEEN, true);
    }

    @Test
    public void sendPendingMessagesSynchronous_whenMessageSentSuccesfully_shouldUpdateProgress() throws MessagingException {
        setupAccountWithMessageToSend();

        new SendPendingMessagesTask(controller, appContext, notificationController,
                transportProvider, sendCount, account, listeners).sendPendingMessagesSynchronous();

        verify(listener).synchronizeMailboxProgress(account, "Sent", 1, 1);
    }

    @Test
    public void sendPendingMessagesSynchronous_shouldUpdateProgress() throws MessagingException {
        setupAccountWithMessageToSend();

        new SendPendingMessagesTask(controller, appContext, notificationController,
                transportProvider, sendCount, account, listeners).sendPendingMessagesSynchronous();

        verify(listener).synchronizeMailboxProgress(account, "Sent", 1, 1);
    }

    @Test
    public void sendPendingMessagesSynchronous_withAuthenticationFailure_shouldNotify() throws MessagingException {
        setupAccountWithMessageToSend();
        doThrow(new AuthenticationFailedException("Test")).when(transport).sendMessage(localMessageToSend1);

        new SendPendingMessagesTask(controller, appContext, notificationController,
                transportProvider, sendCount, account, listeners).sendPendingMessagesSynchronous();

        verify(notificationController).showAuthenticationErrorNotification(account, false);
    }

    @Test
    public void sendPendingMessagesSynchronous_withCertificateFailure_shouldNotify() throws MessagingException {
        setupAccountWithMessageToSend();
        doThrow(new CertificateValidationException("Test")).when(transport).sendMessage(localMessageToSend1);

        new SendPendingMessagesTask(controller, appContext, notificationController,
                transportProvider, sendCount, account, listeners).sendPendingMessagesSynchronous();

        verify(notificationController).showCertificateErrorNotification(account, false);
    }

    @Test
    public void sendPendingMessagesSynchronous_shouldCallListenerOnCompletion() throws MessagingException {
        setupAccountWithMessageToSend();

        new SendPendingMessagesTask(controller, appContext, notificationController,
                transportProvider, sendCount, account, listeners).sendPendingMessagesSynchronous();

        verify(listener).sendPendingMessagesCompleted(account);
    }



}
