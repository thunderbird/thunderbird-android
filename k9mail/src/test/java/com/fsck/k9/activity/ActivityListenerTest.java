package com.fsck.k9.activity;


import android.content.Context;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.K9RobolectricTestRunner;
import com.fsck.k9.mail.Message;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@RunWith(K9RobolectricTestRunner.class)
public class ActivityListenerTest {
    private static final String FOLDER_ID = "folder";
    private static final String FOLDER_NAME = "folderName";
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
        activityListener.synchronizeMailboxStarted(account, FOLDER_ID, FOLDER_NAME);
        activityListener.folderStatusChanged(account, FOLDER_ID, FOLDER_NAME, COUNT);

        String operation = activityListener.getOperation(context);

        assertEquals("Poll account:folderName", operation);
    }

    @Test
    public void getOperation__whenSynchronizeMailboxStarted() {
        activityListener.synchronizeMailboxStarted(account, FOLDER_ID, FOLDER_NAME);

        String operation = activityListener.getOperation(context);

        assertEquals("Poll account:folderName", operation);
    }

    @Test
    public void getOperation__whenSynchronizeMailboxProgress_shouldResultInValidStatus() {
        activityListener.synchronizeMailboxStarted(account, FOLDER_ID, FOLDER_NAME);
        activityListener.synchronizeMailboxProgress(account, FOLDER_ID, FOLDER_NAME, 1, 2);

        String operation = activityListener.getOperation(context);

        assertEquals("Poll account:folderName 1/2", operation);
    }

    @Test
    public void getOperation__whenSynchronizeMailboxFailed_shouldResultInValidStatus() {
        activityListener.synchronizeMailboxStarted(account, FOLDER_ID, FOLDER_NAME);
        activityListener.synchronizeMailboxFailed(account, FOLDER_ID, FOLDER_NAME, ERROR_MESSAGE);

        String operation = activityListener.getOperation(context);

        if (K9.isDebug()) {
            assertEquals("Polling and pushing disabled", operation);
        } else {
            assertEquals("Syncing disabled", operation);
        }
    }

    @Test
    public void getOperation__whenSynchronizeMailboxFailedAfterHeadersStarted_shouldResultInValidStatus() {
        activityListener.synchronizeMailboxStarted(account, FOLDER_ID, FOLDER_NAME);
        activityListener.synchronizeMailboxHeadersStarted(account, FOLDER_ID, FOLDER_NAME);
        activityListener.synchronizeMailboxFailed(account, FOLDER_ID, FOLDER_NAME, ERROR_MESSAGE);

        String operation = activityListener.getOperation(context);

        if (K9.isDebug()) {
            assertEquals("Polling and pushing disabled", operation);
        } else {
            assertEquals("Syncing disabled", operation);
        }
    }

    @Test
    public void getOperation__whenSynchronizeMailboxFinished() {
        activityListener.synchronizeMailboxStarted(account, FOLDER_ID, FOLDER_NAME);
        activityListener.synchronizeMailboxFinished(account, FOLDER_ID, FOLDER_NAME, COUNT, COUNT);

        String operation = activityListener.getOperation(context);

        if (K9.isDebug()) {
            assertEquals("Polling and pushing disabled", operation);
        } else {
            assertEquals("Syncing disabled", operation);
        }
    }

    @Test
    public void getOperation__whenSynchronizeMailboxHeadersStarted_shouldResultInValidStatus() {
        activityListener.synchronizeMailboxHeadersStarted(account, FOLDER_ID, FOLDER_NAME);

        String operation = activityListener.getOperation(context);

        assertEquals("Fetching headers account:folderName", operation);
    }

    @Test
    public void getOperation__whenSynchronizeMailboxHeadersProgress() {
        activityListener.synchronizeMailboxHeadersStarted(account, FOLDER_ID, FOLDER_NAME);
        activityListener.synchronizeMailboxHeadersProgress(account, FOLDER_ID, FOLDER_NAME, 2, 3);

        String operation = activityListener.getOperation(context);

        assertEquals("Fetching headers account:folderName 2/3", operation);
    }

    @Test
    public void getOperation__whenSynchronizeMailboxHeadersFinished() {
        activityListener.synchronizeMailboxHeadersStarted(account, FOLDER_ID, FOLDER_NAME);
        activityListener.synchronizeMailboxHeadersFinished(account, FOLDER_ID, FOLDER_NAME, COUNT, COUNT);

        String operation = activityListener.getOperation(context);

        assertEquals("", operation);
    }

    @Test
    public void getOperation__whenSynchronizeMailboxNewMessage() {
        activityListener.synchronizeMailboxStarted(account, FOLDER_ID, FOLDER_NAME);
        activityListener.synchronizeMailboxNewMessage(account, FOLDER_ID, FOLDER_NAME, message);

        String operation = activityListener.getOperation(context);

        assertEquals("Poll account:folderName", operation);
    }

    private Account createAccount() {
        Account account = mock(Account.class);
        when(account.getDescription()).thenReturn("account");
        return account;
    }
}
