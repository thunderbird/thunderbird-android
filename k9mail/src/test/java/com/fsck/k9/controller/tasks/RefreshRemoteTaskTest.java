package com.fsck.k9.controller.tasks;


import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.Context;

import com.fsck.k9.Account;
import com.fsck.k9.AccountStats;
import com.fsck.k9.K9RobolectricTestRunner;
import com.fsck.k9.controller.MessagingListener;
import com.fsck.k9.controller.SimpleMessagingListener;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.Store;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.mailstore.LocalStore;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowLog;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@SuppressWarnings("ALL")
@RunWith(K9RobolectricTestRunner.class)
public class RefreshRemoteTaskTest {
    private static final String FOLDER_NAME = "Folder";
    private static final int MAXIMUM_SMALL_MESSAGE_SIZE = 1000;
    @Mock
    private Account account;
    @Mock
    private AccountStats accountStats;
    @Mock
    private SimpleMessagingListener listener;
    @Mock
    private LocalFolder localFolder;
    @Mock
    private Folder remoteFolder;
    @Mock
    private LocalStore localStore;
    @Mock
    private Store remoteStore;

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

    private void configureRemoteStoreWithFolder() throws MessagingException {
        when(account.getRemoteStore()).thenReturn(remoteStore);
        when(remoteStore.getFolder(FOLDER_NAME)).thenReturn(remoteFolder);
        when(remoteFolder.getName()).thenReturn(FOLDER_NAME);
    }

    @Test
    public void refreshRemoteSynchronous_shouldCreateFoldersFromRemote() throws MessagingException {
        configureRemoteStoreWithFolder();
        LocalFolder newLocalFolder = mock(LocalFolder.class);

        List<Folder> folders = Collections.singletonList(remoteFolder);
        when(remoteStore.getPersonalNamespaces(false)).thenAnswer(createAnswer(folders));
        when(remoteFolder.getName()).thenReturn("NewFolder");
        when(localStore.getFolder("NewFolder")).thenReturn(newLocalFolder);

        new RefreshRemoteTask(account, listener, listeners).run();

        verify(localStore).createFolders(eq(Collections.singletonList(newLocalFolder)), anyInt());
    }

    @Test
    public void refreshRemoteSynchronous_shouldDeleteFoldersNotOnRemote() throws MessagingException {
        configureRemoteStoreWithFolder();
        LocalFolder oldLocalFolder = mock(LocalFolder.class);
        when(oldLocalFolder.getName()).thenReturn("OldLocalFolder");
        when(localStore.getPersonalNamespaces(false))
                .thenReturn(Collections.singletonList(oldLocalFolder));
        List<Folder> folders = Collections.emptyList();
        when(remoteStore.getPersonalNamespaces(false)).thenAnswer(createAnswer(folders));

        new RefreshRemoteTask(account, listener, listeners).run();

        verify(oldLocalFolder).delete(false);
    }

    @Test
    public void refreshRemoteSynchronous_shouldNotDeleteFoldersOnRemote() throws MessagingException {
        configureRemoteStoreWithFolder();
        when(localStore.getPersonalNamespaces(false))
                .thenReturn(Collections.singletonList(localFolder));
        List<Folder> folders = Collections.singletonList(remoteFolder);
        when(remoteStore.getPersonalNamespaces(false)).thenAnswer(createAnswer(folders));

        new RefreshRemoteTask(account, listener, listeners).run();

        verify(localFolder, never()).delete(false);
    }

    @Test
    public void refreshRemoteSynchronous_shouldNotDeleteSpecialFoldersNotOnRemote() throws MessagingException {
        configureRemoteStoreWithFolder();
        LocalFolder missingSpecialFolder = mock(LocalFolder.class);
        when(account.isSpecialFolder("Outbox")).thenReturn(true);
        when(missingSpecialFolder.getName()).thenReturn("Outbox");
        when(localStore.getPersonalNamespaces(false))
                .thenReturn(Collections.singletonList(missingSpecialFolder));
        List<Folder> folders = Collections.emptyList();
        when(remoteStore.getPersonalNamespaces(false)).thenAnswer(createAnswer(folders));

        new RefreshRemoteTask(account, listener, listeners).run();

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
        when(localStore.getPersonalNamespaces(false)).thenReturn(folders);

        new RefreshRemoteTask(account, listener, listeners).run();

        verify(listener).listFolders(account, folders);
    }

    @Test
    public void refreshRemoteSynchronous_shouldNotifyFinishedAfterSuccess() throws MessagingException {
        configureRemoteStoreWithFolder();
        List<LocalFolder> folders = Collections.singletonList(localFolder);
        when(localStore.getPersonalNamespaces(false)).thenReturn(folders);

        new RefreshRemoteTask(account, listener, listeners).run();

        verify(listener).listFoldersFinished(account);
    }

    @Test
    public void refreshRemoteSynchronous_shouldNotNotifyFinishedAfterFailure() throws MessagingException {
        configureRemoteStoreWithFolder();
        when(localStore.getPersonalNamespaces(false)).thenThrow(new MessagingException("Test"));

        new RefreshRemoteTask(account, listener, listeners).run();

        verify(listener, never()).listFoldersFinished(account);
    }

}
