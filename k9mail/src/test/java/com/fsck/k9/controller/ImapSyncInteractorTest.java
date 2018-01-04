package com.fsck.k9.controller;


import com.fsck.k9.Account;
import com.fsck.k9.Account.Expunge;
import com.fsck.k9.K9RobolectricTestRunner;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.store.RemoteStore;
import com.fsck.k9.mail.store.imap.ImapFolder;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.mailstore.LocalStore;
import com.fsck.k9.notification.NotificationController;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static java.util.Collections.singleton;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
@RunWith(K9RobolectricTestRunner.class)
public class ImapSyncInteractorTest {

    private static final String FOLDER_NAME = "Folder";

    private ImapSyncInteractor syncInteractor;

    @Mock
    private Account account;
    @Mock
    private LocalFolder localFolder;
    @Mock
    private ImapFolder imapFolder;
    @Mock
    private MessagingListener listener;
    @Mock
    private MessagingController controller;
    @Mock
    private FlagSyncHelper flagSyncHelper;
    @Mock
    private MessageDownloader messageDownloader;
    @Mock
    private NotificationController notificationController;
    @Mock
    private SyncHelper syncHelper;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        syncInteractor = new ImapSyncInteractor(syncHelper, flagSyncHelper, controller, messageDownloader,
                notificationController);

        configureLocalStoreWithFolder();
        configureLocalFolder();
        configureImapStoreWithFolder(imapFolder);
        configureImapFolder();
        when(syncHelper.verifyOrCreateRemoteSpecialFolder(account, FOLDER_NAME, imapFolder, listener, controller))
                .thenReturn(true);
        when(controller.getListeners(listener)).thenReturn(singleton(listener));
    }

    @Test
    public void performSync_withProcessPendingCommandsSynchronousThrowingException_shouldFinishWithError()
            throws Exception {
        doThrow(MessagingException.class).when(controller).processPendingCommandsSynchronous(account);

        syncInteractor.performSync(account, FOLDER_NAME, listener, null);

        verify(listener).synchronizeMailboxFailed(eq(account), eq(FOLDER_NAME), anyString());
    }
    @Test
    public void performSync_withImapFolder_shouldFinishWithoutError() throws Exception {
        syncInteractor.performSync(account, FOLDER_NAME, listener, null);

        verify(listener).synchronizeMailboxFinished(eq(account), eq(FOLDER_NAME), anyInt(), anyInt());
    }

    @Test
    public void performSync_withNonImapFolder_shouldFinishWithError() throws Exception {
        configureImapStoreWithFolder(mock(Folder.class));

        syncInteractor.performSync(account, FOLDER_NAME, listener, null);

        verify(listener).synchronizeMailboxFailed(eq(account), eq(FOLDER_NAME), anyString());
    }

    @Test
    public void performSync_withRemoteFolderCreationFailed_shouldExitWithoutOpeningRemoteFolder() throws Exception {
        when(syncHelper.verifyOrCreateRemoteSpecialFolder(account, FOLDER_NAME, imapFolder, listener, controller))
                .thenReturn(false);

        syncInteractor.performSync(account, FOLDER_NAME, listener, null);

        verify(imapFolder, never()).open(anyInt());
    }

    @Test
    public void performSync_withAccountNotSetToExpungeOnPoll_shouldNotExpungeRemoteFolder() throws Exception {
        when(account.getExpungePolicy()).thenReturn(Expunge.EXPUNGE_MANUALLY);

        syncInteractor.performSync(account, FOLDER_NAME, listener, null);

        verify(imapFolder, never()).expunge();
    }

    @Test
    public void performSync_withNegativeMessageCount_shouldFinishWithError() throws Exception {
        when(imapFolder.getMessageCount()).thenReturn(-1);

        syncInteractor.performSync(account, FOLDER_NAME, listener, null);

        verify(listener).synchronizeMailboxFailed(eq(account), eq(FOLDER_NAME), anyString());
    }

    @Test
    public void performSync_afterCompletion_shouldCloseFolders() throws Exception {
        syncInteractor.performSync(account, FOLDER_NAME, listener, null);

        verify(localFolder).close();
        verify(imapFolder).close();
    }

    private void configureLocalStoreWithFolder() throws MessagingException {
        LocalStore localStore = mock(LocalStore.class);
        when(account.getLocalStore()).thenReturn(localStore);
        when(localStore.getFolder(FOLDER_NAME)).thenReturn(localFolder);
    }

    private void configureLocalFolder() throws Exception {
        when(localFolder.getName()).thenReturn(FOLDER_NAME);
    }

    private void configureImapStoreWithFolder(Folder folder) throws MessagingException {
        RemoteStore remoteStore = mock(RemoteStore.class);
        when(account.getRemoteStore()).thenReturn(remoteStore);
        when(remoteStore.getFolder(FOLDER_NAME)).thenReturn(folder);
    }

    private void configureImapFolder() throws Exception {
        when(imapFolder.getName()).thenReturn(FOLDER_NAME);
    }
}