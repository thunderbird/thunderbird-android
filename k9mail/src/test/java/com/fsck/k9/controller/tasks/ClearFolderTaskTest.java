package com.fsck.k9.controller.tasks;


import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import android.content.Context;

import com.fsck.k9.Account;
import com.fsck.k9.AccountStats;
import com.fsck.k9.K9RobolectricTestRunner;
import com.fsck.k9.controller.IMessageController;
import com.fsck.k9.controller.MessagingListener;
import com.fsck.k9.controller.SimpleMessagingListener;
import com.fsck.k9.controller.UnavailableAccountException;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.mailstore.LocalStore;
import com.fsck.k9.mailstore.UnavailableStorageException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowLog;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith(K9RobolectricTestRunner.class)
public class ClearFolderTaskTest {
    private static final String FOLDER_NAME = "Folder";
    private static final int MAXIMUM_SMALL_MESSAGE_SIZE = 1000;

    @Mock
    private IMessageController controller;
    @Mock
    private Account account;
    @Mock
    private AccountStats accountStats;
    @Mock
    private SimpleMessagingListener listener;
    @Mock
    private LocalFolder localFolder;
    @Mock
    private LocalStore localStore;

    private Context appContext;
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
        when(localStore.getPersonalNamespaces(false)).thenReturn(
                Collections.singletonList(localFolder));
    }


    @Test
    public void run_shouldOpenFolderForWriting() throws MessagingException {
        new ClearFolderTask(controller, appContext, account, FOLDER_NAME, listener, listeners).run();

        verify(localFolder).open(Folder.OPEN_MODE_RW);
    }

    @Test
    public void run_shouldClearAllMessagesInTheFolder() throws MessagingException {
        new ClearFolderTask(controller, appContext, account, FOLDER_NAME, listener, listeners).run();

        verify(localFolder).clearAllMessages();
    }

    @Test
    public void run_shouldCloseTheFolder() throws MessagingException {
        new ClearFolderTask(controller, appContext, account, FOLDER_NAME, listener, listeners).run();

        verify(localFolder, atLeastOnce()).close();
    }

    @Test(expected = UnavailableAccountException.class)
    public void run_whenStorageUnavailable_shouldThrowUnavailableAccountException() throws MessagingException {
        doThrow(new UnavailableStorageException("Test")).when(localFolder).open(Folder.OPEN_MODE_RW);

        new ClearFolderTask(controller, appContext, account, FOLDER_NAME, listener, listeners).run();
    }

    @Test()
    public void run_whenExceptionThrown_shouldStillCloseFolder() throws MessagingException {
        doThrow(new RuntimeException("Test")).when(localFolder).open(Folder.OPEN_MODE_RW);

        try {
            new ClearFolderTask(controller, appContext, account, FOLDER_NAME, listener, listeners).run();
        } catch (Exception ignored){
        }

        verify(localFolder, atLeastOnce()).close();
    }

    @Test()
    public void run_shouldListFolders() throws MessagingException {
        new ClearFolderTask(controller, appContext, account, FOLDER_NAME, listener, listeners).run();

        verify(listener, atLeastOnce()).listFoldersStarted(account);
    }
}
