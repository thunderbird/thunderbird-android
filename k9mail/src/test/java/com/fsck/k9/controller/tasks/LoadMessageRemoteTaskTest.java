package com.fsck.k9.controller.tasks;


import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.Context;

import com.fsck.k9.Account;
import com.fsck.k9.AccountStats;
import com.fsck.k9.K9RobolectricTestRunner;
import com.fsck.k9.controller.MessageDownloader;
import com.fsck.k9.controller.MessagingController;
import com.fsck.k9.controller.MessagingListener;
import com.fsck.k9.controller.SimpleMessagingListener;
import com.fsck.k9.helper.Contacts;
import com.fsck.k9.mail.FetchProfile;
import com.fsck.k9.mail.FetchProfile.Item;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.MessageRetrievalListener;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Store;
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
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowLog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith(K9RobolectricTestRunner.class)
public class LoadMessageRemoteTaskTest {
    private static final String FOLDER_NAME = "Folder";
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
    private SearchResultsLoader searchResultsLoader;
    @Mock
    private MessageDownloader messageDownloader;
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

    private void configureRemoteStoreWithFolder() throws MessagingException {
        when(account.getRemoteStore()).thenReturn(remoteStore);
        when(remoteStore.getFolder(FOLDER_NAME)).thenReturn(remoteFolder);
        when(remoteFolder.getName()).thenReturn(FOLDER_NAME);
    }

    @Test
    public void loadMessageRemoteSynchronous_shouldOpenLocalFolder() throws Exception {
        new LoadMessageRemoteTask(controller, appContext, notificationController, listeners, messageDownloader,
                account, FOLDER_NAME, "1", listener,
                false).run();

        verify(localFolder).open(Folder.OPEN_MODE_RW);
    }

    @Test
    public void loadMessageRemoteSynchronous_shouldGetMessageFromLocalFolder() throws Exception {
        new LoadMessageRemoteTask(controller, appContext, notificationController, listeners, messageDownloader,
                account, FOLDER_NAME, "1", listener,
                false).run();

        verify(localFolder).getMessage("1");
    }

    @Test
    public void loadMessageRemoteSynchronous_shouldNotOpenRemoteFolder_forLocalUid() throws Exception {
        new LoadMessageRemoteTask(controller, appContext, notificationController, listeners, messageDownloader,
                account, FOLDER_NAME, "K9LOCAL:1", listener,
                false).run();

        verify(remoteFolder, never()).open(anyInt());
    }

    @Test
    public void loadMessageRemoteSynchronous_shouldOpenRemoteFolder_forRemoteUid() throws Exception {
        configureRemoteStoreWithFolder();
        new LoadMessageRemoteTask(controller, appContext, notificationController, listeners, messageDownloader,
                account, FOLDER_NAME, "1", listener,
                false).run();

        verify(remoteFolder).open(Folder.OPEN_MODE_RW);
    }

    @Test
    public void loadMessageRemoteSynchronous_shouldGetMessageUsingRemoteUid() throws Exception {
        configureRemoteStoreWithFolder();
        new LoadMessageRemoteTask(controller, appContext, notificationController, listeners, messageDownloader,
                account, FOLDER_NAME, "1", listener,
                false).run();

        verify(remoteFolder).getMessage("1");
    }

    @Test
    public void loadMessageRemoteSynchronous_withLoadPartialFromSearchFalse_shouldFetchBodyAndFlags()
            throws Exception {
        configureRemoteStoreWithFolder();
        when(localFolder.getMessage("1")).thenReturn(localNewMessage1);
        when(remoteFolder.getMessage("1")).thenReturn(remoteNewMessage1);
        new LoadMessageRemoteTask(controller, appContext, notificationController, listeners, messageDownloader,
                account, FOLDER_NAME, "1", listener,
                false).run();

        verify(remoteFolder).fetch(eq(Collections.singletonList(remoteNewMessage1)),
                fetchProfileCaptor.capture(), Matchers.<MessageRetrievalListener>eq(null));
        assertTrue(fetchProfileCaptor.getValue().contains(Item.BODY));
        assertTrue(fetchProfileCaptor.getValue().contains(Item.FLAGS));
        assertEquals(2, fetchProfileCaptor.getValue().size());
    }

    @Test
    public void loadMessageRemoteSynchronous_withLoadPartialFromSearchTrue_shouldDownloadMessages()
            throws Exception {
        configureRemoteStoreWithFolder();
        when(localFolder.getMessage("1")).thenReturn(localNewMessage1);
        when(localNewMessage1.getUid()).thenReturn("1");
        when(remoteFolder.getMessage("1")).thenReturn(remoteNewMessage1);
        when(remoteNewMessage1.getUid()).thenReturn("1");
        new LoadMessageRemoteTask(controller, appContext, notificationController, listeners, messageDownloader,
                account, FOLDER_NAME, "1", listener,
                true).run();

        verify(messageDownloader).downloadMessages(controller, notificationController, appContext,
                account, remoteFolder, localFolder, Collections.singletonList(remoteNewMessage1),
                false, false, listeners);
    }

}
