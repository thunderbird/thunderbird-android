package com.fsck.k9.controller.tasks;


import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.Context;

import com.fsck.k9.Account;
import com.fsck.k9.AccountStats;
import com.fsck.k9.K9RobolectricTestRunner;
import com.fsck.k9.controller.IMessageController;
import com.fsck.k9.controller.MessagingListener;
import com.fsck.k9.controller.SimpleMessagingListener;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.mailstore.LocalStore;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowLog;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith(K9RobolectricTestRunner.class)
public class ListFoldersTaskTest {
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
    @Captor
    private ArgumentCaptor<List<LocalFolder>> localFolderListCaptor;
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
        when(localStore.getPersonalNamespaces(false)).thenReturn(Collections.singletonList(localFolder));
    }

    @Test
    public void run_shouldNotifyTheListenerListingStarted() throws MessagingException {
        List<LocalFolder> folders = Collections.singletonList(localFolder);
        when(localStore.getPersonalNamespaces(false)).thenReturn(folders);

        new ListFoldersTask(controller, appContext, account, false, listener, listeners).run();

        verify(listener).listFoldersStarted(account);
    }

    @Test
    public void run_withUnavailableAccount_shouldNotUseAccountLocalStore() throws MessagingException {
        when(account.isAvailable(appContext)).thenReturn(false);

        new ListFoldersTask(controller, appContext, account, false, listener, listeners).run();

        verify(account, never()).getLocalStore();
    }

    @Test
    public void run_withNoFolders_shouldInstructControllerToRefreshRemote() throws MessagingException {
        when(localStore.getPersonalNamespaces(false)).thenReturn(Collections.<LocalFolder>emptyList());

        new ListFoldersTask(controller, appContext, account, false, listener, listeners).run();

        verify(controller).doRefreshRemote(account, listener);
    }

    @Test
    public void run_shouldNotifyTheListenerOfTheListOfFolders() throws MessagingException {
        List<LocalFolder> folders = Collections.singletonList(localFolder);
        when(localStore.getPersonalNamespaces(false)).thenReturn(folders);

        new ListFoldersTask(controller, appContext, account, false, listener, listeners).run();

        verify(listener).listFolders(eq(account), localFolderListCaptor.capture());
        assertEquals(folders, localFolderListCaptor.getValue());
    }

    @Test
    public void run_shouldNotifyFailureOnException() throws MessagingException {
        when(localStore.getPersonalNamespaces(false)).thenThrow(new MessagingException("Test"));

        new ListFoldersTask(controller, appContext, account, true, listener, listeners).run();

        verify(listener).listFoldersFailed(account, "Test");
    }

    @Test
    public void run_shouldNotNotifyFinishedAfterFailure() throws MessagingException {
        when(localStore.getPersonalNamespaces(false)).thenThrow(new MessagingException("Test"));

        new ListFoldersTask(controller, appContext, account, true, listener, listeners).run();

        verify(listener, never()).listFoldersFinished(account);
    }

    @Test
    public void run_shouldNotifyFinishedAfterSuccess() throws MessagingException {
        List<LocalFolder> folders = Collections.singletonList(localFolder);
        when(localStore.getPersonalNamespaces(false)).thenReturn(folders);

        new ListFoldersTask(controller, appContext, account, false, listener, listeners).run();

        verify(listener).listFoldersFinished(account);
    }

}
