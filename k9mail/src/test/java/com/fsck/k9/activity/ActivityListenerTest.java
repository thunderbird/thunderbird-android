package com.fsck.k9.activity;

import android.content.Context;

import com.fsck.k9.Account;
import com.fsck.k9.mail.Message;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@RunWith(RobolectricTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml", sdk = 21)
public class ActivityListenerTest {

    private static final Account ACCOUNT = mock(Account.class);
    private static final String FOLDER = "folder";
    private static final String ERROR_MESSAGE = "errorMessage";
    private static final Message MESSAGE = mock(Message.class);
    private static final int COUNT = 1;
    private ActivityListener activityListener;
    private Context context;

    @Before
    public void before() {
        when(ACCOUNT.getDescription()).thenReturn("account");
        activityListener = new ActivityListener();
        context = RuntimeEnvironment.application;
    }

    @Test
    public void getOperation__whenFolderStatusChanged() {
        activityListener.synchronizeMailboxStarted(ACCOUNT, FOLDER);
        activityListener.folderStatusChanged(ACCOUNT, FOLDER, COUNT);

        String operation = activityListener.getOperation(context);

        assertEquals("Poll account:folder", operation);
    }

    @Test
    public void getOperation__whenSynchronizeMailboxStarted() {
        activityListener.synchronizeMailboxStarted(ACCOUNT, FOLDER);

        String operation = activityListener.getOperation(context);

        assertEquals("Poll account:folder", operation);
    }

    @Test
    public void getOperation__whenSynchronizeMailboxProgress_shouldResultInValidStatus() {
        activityListener.synchronizeMailboxStarted(ACCOUNT, FOLDER);
        activityListener.synchronizeMailboxProgress(ACCOUNT, FOLDER, COUNT, COUNT);

        String operation = activityListener.getOperation(context);

        assertEquals("Poll account:folder\u00201/1", operation);
    }

    @Test
    public void getOperation__whenSynchronizeMailboxFailed_shouldResultInValidStatus() {
        activityListener.synchronizeMailboxStarted(ACCOUNT, FOLDER);
        activityListener.synchronizeMailboxFailed(ACCOUNT, FOLDER, ERROR_MESSAGE);

        String operation = activityListener.getOperation(context);

        assertEquals("Syncing disabled", operation);
    }

    @Test
    public void getOperation__whenSynchronizeMailboxFinished() {
        activityListener.synchronizeMailboxStarted(ACCOUNT, FOLDER);
        activityListener.synchronizeMailboxFinished(ACCOUNT, FOLDER, COUNT, COUNT);

        String operation = activityListener.getOperation(context);

        assertEquals("Syncing disabled", operation);
    }

    @Test
    public void getOperation__whenSynchronizeMailboxHeadersStarted_shouldResultInValidStatus() {
        activityListener.synchronizeMailboxHeadersStarted(ACCOUNT, FOLDER);

        String operation = activityListener.getOperation(context);

        assertEquals("Fetching headers account:folder", operation);
    }

    @Test
    public void getOperation__whenSynchronizeMailboxHeadersProgress() {
        activityListener.synchronizeMailboxHeadersStarted(ACCOUNT, FOLDER);
        activityListener.synchronizeMailboxHeadersProgress(ACCOUNT, FOLDER, COUNT, COUNT);

        String operation = activityListener.getOperation(context);

        assertEquals("Fetching headers account:folder\u00201/1", operation);
    }

    @Test
    public void getOperation__whenSynchronizeMailboxHeadersFinished() {
        activityListener.synchronizeMailboxHeadersStarted(ACCOUNT, FOLDER);
        activityListener.synchronizeMailboxHeadersFinished(ACCOUNT, FOLDER, COUNT, COUNT);

        String operation = activityListener.getOperation(context);

        assertEquals("", operation);
    }

    @Test
    public void getOperation__whenSynchronizeMailboxAddOrUpdateMessage() {
        activityListener.synchronizeMailboxStarted(ACCOUNT, FOLDER);
        activityListener.synchronizeMailboxAddOrUpdateMessage(ACCOUNT, FOLDER, MESSAGE);

        String operation = activityListener.getOperation(context);

        assertEquals("Poll account:folder", operation);
    }

    @Test
    public void getOperation__whenSynchronizeMailboxNewMessage() {
        activityListener.synchronizeMailboxStarted(ACCOUNT, FOLDER);
        activityListener.synchronizeMailboxNewMessage(ACCOUNT, FOLDER, MESSAGE);

        String operation = activityListener.getOperation(context);

        assertEquals("Poll account:folder", operation);
    }
}
