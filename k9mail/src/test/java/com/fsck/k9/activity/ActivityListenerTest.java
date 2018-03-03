package com.fsck.k9.activity;


import android.content.Context;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.mail.Message;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@RunWith(RobolectricTestRunner.class)
public class ActivityListenerTest {
    private static final String FOLDER = "folder";
    private static final String ERROR_MESSAGE = "errorMessage";
    private static final int COUNT = 23;


    private Context context;
    private Account account;
    private Message message;
    private ActivityListener activityListener;

    
    @Before
    public void before() {
        context = RuntimeEnvironment.application;
        account = createAccount();
        message = mock(Message.class);
        
        activityListener = new ActivityListener();
    }

    @Test
    public void getOperation__whenFolderStatusChanged() {
        activityListener.synchronizeMailboxStarted(account, FOLDER);
        activityListener.folderStatusChanged(account, FOLDER, COUNT);

        String operation = activityListener.getOperation(context);

        assertEquals("Poll account:folder", operation);
    }

    @Test
    public void getOperation__whenSynchronizeMailboxStarted() {
        activityListener.synchronizeMailboxStarted(account, FOLDER);

        String operation = activityListener.getOperation(context);

        assertEquals("Poll account:folder", operation);
    }

    @Test
    public void getOperation__whenSynchronizeMailboxProgress_shouldResultInValidStatus() {
        activityListener.synchronizeMailboxStarted(account, FOLDER);
        activityListener.synchronizeMailboxProgress(account, FOLDER, 1, 2);

        String operation = activityListener.getOperation(context);

        assertEquals("Poll account:folder 1/2", operation);
    }

    @Test
    public void getOperation__whenSynchronizeMailboxFailed_shouldResultInValidStatus() {
        activityListener.synchronizeMailboxStarted(account, FOLDER);
        activityListener.synchronizeMailboxFailed(account, FOLDER, ERROR_MESSAGE);

        String operation = activityListener.getOperation(context);

        if (K9.isDebug()) {
            assertEquals("Polling and pushing disabled", operation);
        } else {
            assertEquals("Syncing disabled", operation);
        }
    }

    @Test
    public void getOperation__whenSynchronizeMailboxFailedAfterHeadersStarted_shouldResultInValidStatus() {
        activityListener.synchronizeMailboxStarted(account, FOLDER);
        activityListener.synchronizeMailboxHeadersStarted(account, FOLDER);
        activityListener.synchronizeMailboxFailed(account, FOLDER, ERROR_MESSAGE);

        String operation = activityListener.getOperation(context);

        if (K9.isDebug()) {
            assertEquals("Polling and pushing disabled", operation);
        } else {
            assertEquals("Syncing disabled", operation);
        }
    }

    @Test
    public void getOperation__whenSynchronizeMailboxFinished() {
        activityListener.synchronizeMailboxStarted(account, FOLDER);
        activityListener.synchronizeMailboxFinished(account, FOLDER, COUNT, COUNT);

        String operation = activityListener.getOperation(context);

        if (K9.isDebug()) {
            assertEquals("Polling and pushing disabled", operation);
        } else {
            assertEquals("Syncing disabled", operation);
        }
    }

    @Test
    public void getOperation__whenSynchronizeMailboxHeadersStarted_shouldResultInValidStatus() {
        activityListener.synchronizeMailboxHeadersStarted(account, FOLDER);

        String operation = activityListener.getOperation(context);

        assertEquals("Fetching headers account:folder", operation);
    }

    @Test
    public void getOperation__whenSynchronizeMailboxHeadersProgress() {
        activityListener.synchronizeMailboxHeadersStarted(account, FOLDER);
        activityListener.synchronizeMailboxHeadersProgress(account, FOLDER, 2, 3);

        String operation = activityListener.getOperation(context);

        assertEquals("Fetching headers account:folder 2/3", operation);
    }

    @Test
    public void getOperation__whenSynchronizeMailboxHeadersFinished() {
        activityListener.synchronizeMailboxHeadersStarted(account, FOLDER);
        activityListener.synchronizeMailboxHeadersFinished(account, FOLDER, COUNT, COUNT);

        String operation = activityListener.getOperation(context);

        assertEquals("", operation);
    }

    @Test
    public void getOperation__whenSynchronizeMailboxNewMessage() {
        activityListener.synchronizeMailboxStarted(account, FOLDER);
        activityListener.synchronizeMailboxNewMessage(account, FOLDER, message);

        String operation = activityListener.getOperation(context);

        assertEquals("Poll account:folder", operation);
    }

    private Account createAccount() {
        Account account = mock(Account.class);
        when(account.getDescription()).thenReturn("account");
        return account;
    }
}
