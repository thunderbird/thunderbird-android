package com.fsck.k9.controller;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.Context;

import com.fsck.k9.Account;
import com.fsck.k9.AccountStats;
import com.fsck.k9.K9RobolectricTestRunner;
import com.fsck.k9.mail.FetchProfile;
import com.fsck.k9.mail.FetchProfile.Item;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessageRetrievalListener;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.mailstore.LocalStore;
import com.fsck.k9.notification.NotificationController;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowLog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith(K9RobolectricTestRunner.class)
public class MessageDownloaderTest {
    @Mock
    private Folder<Message> remoteFolder;
    @Mock
    private Message smallRemoteMessage;
    @Mock
    private LocalMessage smallLocalMessage;
    @Mock
    private Message largeRemoteMessage;
    @Mock
    private LocalMessage largeLocalMessage;
    @Captor
    private ArgumentCaptor<FetchProfile> fetchProfileCaptor;
    @Captor
    private ArgumentCaptor<MessageRetrievalListener<Message>> messageRetrievalListenerCaptor;
    @Mock
    private IMessageController controller;
    @Mock
    private NotificationController notificationController;
    @Mock
    private Account account;
    @Mock
    private AccountStats accountStats;
    @Mock
    private LocalFolder localFolder;
    @Mock
    private LocalStore localStore;
    @Mock
    private MessagingListener listener;

    private Context appContext;
    private MessageDownloader messageDownloader = new MessageDownloader();
    private List<List<Message>> fetchListCapture = new ArrayList<>();

    private static final int MAXIMUM_SMALL_MESSAGE_SIZE = 1000;
    private static final long LARGE_MESSAGE_SIZE = 1001;

    @Before
    public void setUp() throws MessagingException {
        ShadowLog.stream = System.out;
        MockitoAnnotations.initMocks(this);
        appContext = ShadowApplication.getInstance().getApplicationContext();
        configureAccount();
        when(largeRemoteMessage.getUid()).thenReturn("UID");
        when(largeRemoteMessage.getSize()).thenReturn(LARGE_MESSAGE_SIZE);
    }

    private void configureAccount() throws MessagingException {
        when(account.isAvailable(appContext)).thenReturn(true);
        when(account.getLocalStore()).thenReturn(localStore);
        when(account.getStats(any(Context.class))).thenReturn(accountStats);
        when(account.getMaximumAutoDownloadMessageSize()).thenReturn(MAXIMUM_SMALL_MESSAGE_SIZE);
        when(account.getEmail()).thenReturn("user@host.com");
    }

    @SuppressWarnings("unchecked")
    private void configureSmallMessageFetch() throws MessagingException {
        doAnswer(new Answer() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                fetchListCapture.add(new ArrayList<>((List<Message>) invocation.getArguments()[0]));
                MessageRetrievalListener<Message> retrievalListener =
                        (MessageRetrievalListener<Message>) invocation.getArguments()[2];
                retrievalListener.messageStarted("UID", 1, 1);
                retrievalListener.messageFinished(smallRemoteMessage, 1, 1);
                retrievalListener.messagesFinished(1);
                return null;
            }
        }).when(remoteFolder).fetch(
                anyListOf(Message.class), any(FetchProfile.class),
                any(MessageRetrievalListener.class));
        doAnswer(new Answer() {
            @Override
            public LocalMessage answer(InvocationOnMock invocation) throws Throwable {
                ((Runnable) invocation.getArguments()[1]).run();
                return smallLocalMessage;
            }
        }).when(localFolder).storeSmallMessage(eq(smallRemoteMessage), any(Runnable.class));
    }

    @SuppressWarnings("unchecked")
    private void configureLargeMessageFetch() throws MessagingException {
        doAnswer(new Answer() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                fetchListCapture.add(new ArrayList<>((List<Message>) invocation.getArguments()[0]));
                MessageRetrievalListener<Message> retrievalListener =
                        (MessageRetrievalListener<Message>) invocation.getArguments()[2];
                if (retrievalListener != null) {
                    retrievalListener.messageStarted("UID", 1, 1);
                    retrievalListener.messageFinished(largeRemoteMessage, 1, 1);
                    retrievalListener.messagesFinished(1);
                }
                return null;
            }
        }).when(remoteFolder).fetch(
                anyListOf(Message.class),
                any(FetchProfile.class),
                any(MessageRetrievalListener.class));
        doAnswer(new Answer() {
            @Override
            public Map<String, String> answer(InvocationOnMock invocation) throws Throwable {
                when(localFolder.getMessage("UID")).thenReturn(largeLocalMessage);
                return null;
            }
        }).when(localFolder).appendMessages(anyListOf(Message.class));
    }

    @Test
    public void downloadMessages_withSmallRecentUnsyncedVisibleMessage_downloadsEnvelopeForMessage()
            throws MessagingException {
        configureSmallMessageFetch();
        List<Message> inputMessages = Collections.singletonList(smallRemoteMessage);
        Set<MessagingListener> listeners = Collections.singleton(listener);

        messageDownloader.downloadMessages(controller, notificationController,
                appContext, account, remoteFolder, localFolder,
                inputMessages, false, true, listeners);
        verify(remoteFolder, atLeast(1)).fetch(
                anyListOf(Message.class), fetchProfileCaptor.capture(),
                messageRetrievalListenerCaptor.capture());
        assertEquals(Collections.singletonList(smallRemoteMessage), fetchListCapture.get(0));
        assertTrue(fetchProfileCaptor.getAllValues().get(0).contains(Item.ENVELOPE));

    }

    @Test
    public void downloadMessages_withSmallRecentUnsyncedVisibleMessage_downloadsBody()
            throws MessagingException {
        configureSmallMessageFetch();
        List<Message> inputMessages = Collections.singletonList(smallRemoteMessage);
        Set<MessagingListener> listeners = Collections.singleton(listener);

        messageDownloader.downloadMessages(controller, notificationController,
                appContext, account, remoteFolder, localFolder,
                inputMessages, false, true, listeners);
        verify(remoteFolder, times(2)).fetch(
                anyListOf(Message.class), fetchProfileCaptor.capture(),
                messageRetrievalListenerCaptor.capture());
        assertEquals(Collections.singletonList(smallRemoteMessage), fetchListCapture.get(1));
        assertTrue(fetchProfileCaptor.getAllValues().get(1).contains(Item.BODY));
    }

    @Test
    public void downloadMessages_withLargeRecentUnsyncedVisibleMessage_downloadsEnvelopeForMessage()
            throws MessagingException {
        configureLargeMessageFetch();
        List<Message> inputMessages = Collections.singletonList(largeRemoteMessage);
        Set<MessagingListener> listeners = Collections.singleton(listener);

        messageDownloader.downloadMessages(controller, notificationController,
                appContext, account, remoteFolder, localFolder,
                inputMessages, false, true, listeners);
        verify(remoteFolder, atLeast(1)).fetch(
                anyListOf(Message.class), fetchProfileCaptor.capture(),
                messageRetrievalListenerCaptor.capture());
        assertEquals(Collections.singletonList(largeRemoteMessage), fetchListCapture.get(0));
        assertTrue(fetchProfileCaptor.getAllValues().get(0).contains(Item.ENVELOPE));

    }

    @Test
    public void downloadMessages_withLargeRecentUnsyncedVisibleMessage_downloadsBody() throws MessagingException {
        configureLargeMessageFetch();
        List<Message> inputMessages = Collections.singletonList(largeRemoteMessage);
        Set<MessagingListener> listeners = Collections.singleton(listener);

        messageDownloader.downloadMessages(controller, notificationController,
                appContext, account, remoteFolder, localFolder,
                inputMessages, false, true, listeners);
        verify(remoteFolder, times(3)).fetch(
                anyListOf(Message.class),
                fetchProfileCaptor.capture(),
                messageRetrievalListenerCaptor.capture());
        assertEquals(Collections.singletonList(largeRemoteMessage), fetchListCapture.get(1));
        assertTrue(fetchProfileCaptor.getAllValues().get(1).contains(Item.STRUCTURE));
        assertTrue(fetchProfileCaptor.getAllValues().get(2).contains(Item.BODY_SANE));
    }
}
