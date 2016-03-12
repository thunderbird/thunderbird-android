package com.fsck.k9.controller;

import android.content.Context;

import com.fsck.k9.Account;
import com.fsck.k9.AccountStats;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mailstore.LocalFolder;
import com.fsck.k9.mailstore.LocalStore;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml", sdk = 21)
public class MessagingControllerTest {

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
    private String folderName = "Folder";

    @Before
    public void before() throws MessagingException {
        MockitoAnnotations.initMocks(this);
        controller = MessagingController.getInstance(
                ShadowApplication.getInstance().getApplicationContext());
        when(account.getLocalStore()).thenReturn(localStore);
        when(account.getStats(any(Context.class))).thenReturn(accountStats);
        when(localStore.getFolder(folderName)).thenReturn(localFolder);
    }

    private CountDownLatch setupLatchOnSyncMailboxStarted() {
        final CountDownLatch latch = new CountDownLatch(1);
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                latch.countDown();
                return null;
            }
        }).when(listener).synchronizeMailboxStarted(account, folderName);
        return latch;
    }

    private CountDownLatch setupLatchOnSyncMailboxFinished(int totalMsgs, int newMsgs) {
        final CountDownLatch commandFinished = new CountDownLatch(1);
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                commandFinished.countDown();
                return null;
            }
        }).when(listener).synchronizeMailboxFinished(account, folderName, totalMsgs, newMsgs);
        return commandFinished;
    }

    private CountDownLatch setupLatchOnSyncMailboxFailed(String message) {
        final CountDownLatch latch = new CountDownLatch(1);
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                latch.countDown();
                return null;
            }
        }).when(listener).synchronizeMailboxFailed(account, folderName, message);
        return latch;
    }

    private void checkLatchFinished(CountDownLatch latch) throws InterruptedException {
        assertTrue(latch.await(1, TimeUnit.SECONDS));
    }

    @Test
    public void can_synchronize_folder_with_message() throws InterruptedException, MessagingException {
        final CountDownLatch commandStarted = setupLatchOnSyncMailboxStarted();
        final CountDownLatch commandFinished = setupLatchOnSyncMailboxFinished(1,0);
        when(remoteFolder.getMessageCount()).thenReturn(1);
        Thread thread = new Thread(controller);
        thread.start();
        controller.synchronizeMailbox(account, folderName, listener, remoteFolder);
        checkLatchFinished(commandStarted);
        checkLatchFinished(commandFinished);
        verify(localFolder).setStatus(null);
    }

    @Test
    public void can_synchronize_empty_folder() throws InterruptedException, MessagingException {
        final CountDownLatch commandStarted = setupLatchOnSyncMailboxStarted();
        final CountDownLatch commandFinished = setupLatchOnSyncMailboxFinished(0,0);
        when(remoteFolder.getMessageCount()).thenReturn(0);

        Thread thread = new Thread(controller);
        thread.start();
        controller.synchronizeMailbox(account, folderName, listener, remoteFolder);
        checkLatchFinished(commandStarted);
        checkLatchFinished(commandFinished);
        verify(localFolder).setStatus(null);
    }

    @Test
    public void cant_synchronize_folder_with_negative_message_count() throws InterruptedException, MessagingException {
        final CountDownLatch commandStarted = setupLatchOnSyncMailboxStarted();
        final CountDownLatch commandFailed = setupLatchOnSyncMailboxFailed(anyString());
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                commandFailed.countDown();
                return null;
            }
        }).when(listener).synchronizeMailboxFailed(eq(account), eq("Folder"), anyString());
        when(remoteFolder.getMessageCount()).thenReturn(-1);
        Thread thread = new Thread(controller);
        thread.start();
        controller.synchronizeMailbox(account, "Folder", listener, remoteFolder);
        checkLatchFinished(commandStarted);
        checkLatchFinished(commandFailed);
        verify(localFolder).setStatus("Exception: Message count -1 for folder Folder");
    }
}
