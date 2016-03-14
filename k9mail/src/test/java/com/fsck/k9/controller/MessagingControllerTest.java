package com.fsck.k9.controller;


import android.content.Context;

import com.fsck.k9.Account;
import com.fsck.k9.AccountStats;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Store;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.mailstore.LocalStore;
import com.fsck.k9.notification.NotificationController;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith(RobolectricTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml", sdk = 21)
public class MessagingControllerTest {
    private static final String FOLDER_NAME = "Folder";


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

    private void messageCountInRemoteFolder(int value) throws MessagingException {
        when(remoteFolder.getMessageCount()).thenReturn(value);
    }

    private void configureAccount() throws MessagingException {
        when(account.getLocalStore()).thenReturn(localStore);
        when(account.getStats(any(Context.class))).thenReturn(accountStats);
    }

    private void configureLocalStore() {
        when(localStore.getFolder(FOLDER_NAME)).thenReturn(localFolder);
    }
}
