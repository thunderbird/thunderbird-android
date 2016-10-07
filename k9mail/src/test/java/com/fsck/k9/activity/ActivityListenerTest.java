package com.fsck.k9.activity;

import com.fsck.k9.Account;
import com.fsck.k9.mail.Message;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@RunWith(RobolectricTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml", sdk = 21)
public class ActivityListenerTest {

    private ActivityListener activityListener;
    private Account account;
    private String folder;
    private String errorMessage;
    private Message message;
    private int count;

    @Before
    public void before() {
        account = mock(Account.class);
        when(account.getDescription()).thenReturn("account");
        folder = "folder";
        errorMessage = "errorMessage";
        message = mock(Message.class);
        count = 1;
        activityListener = new ActivityListener();
    }

    @Test
    public void folderStatusChanged__shouldResultInValidStatus() {
        activityListener.synchronizeMailboxStarted(account, folder);
        activityListener.folderStatusChanged(account, folder, count);

        String operation = activityListener.getOperation(RuntimeEnvironment.application);

        assertEquals("Poll account:folder", operation);
    }

    @Test
    public void synchronizeMailboxStarted__shouldResultInValidStatus() {
        activityListener.synchronizeMailboxStarted(account, folder);

        String operation = activityListener.getOperation(RuntimeEnvironment.application);

        assertEquals("Poll account:folder", operation);
    }

    @Test
    public void synchronizeMailboxProgress_shouldResultInValidStatus() {
        activityListener.synchronizeMailboxStarted(account, folder);
        activityListener.synchronizeMailboxProgress(account, folder, count, count);

        String operation = activityListener.getOperation(RuntimeEnvironment.application);

        assertEquals("Poll account:folder"+"\\u0020"+"1/1", operation);
    }

    @Test
    public void synchronizeMailboxFailed_shouldResultInValidStatus() {
        activityListener.synchronizeMailboxStarted(account, folder);
        activityListener.synchronizeMailboxFailed(account, folder, errorMessage);

        String operation = activityListener.getOperation(RuntimeEnvironment.application);

        assertNotEquals("Poll account:folder", operation);
        assertNotNull(operation);
    }

    @Test
    public void synchronizeMailboxFinished__shouldResultInValidStatus() {
        activityListener.synchronizeMailboxStarted(account, folder);
        activityListener.synchronizeMailboxFinished(account, folder, count, count);

        String operation = activityListener.getOperation(RuntimeEnvironment.application);

        assertNotNull(operation);
    }

    @Test
    public void synchronizeMailboxHeadersStarted_shouldResultInValidStatus() {
        activityListener.synchronizeMailboxHeadersStarted(account, folder);

        String operation = activityListener.getOperation(RuntimeEnvironment.application);

        assertEquals("Fetching headers account:folder", operation);
    }

    @Test
    public void synchronizeMailboxHeadersProgress__shouldResultInValidStatus() {
        activityListener.synchronizeMailboxHeadersStarted(account, folder);
        activityListener.synchronizeMailboxHeadersProgress(account, folder, count, count);

        String operation = activityListener.getOperation(RuntimeEnvironment.application);

        assertEquals("Fetching headers account:folder"+"\\u0020"+"1/1", operation);
    }

    @Test
    public void synchronizeMailboxHeadersFinished__shouldResultInValidStatus() {
        activityListener.synchronizeMailboxHeadersStarted(account, folder);
        activityListener.synchronizeMailboxHeadersFinished(account, folder, count, count);

        String operation = activityListener.getOperation(RuntimeEnvironment.application);

        assertEquals("", operation);
    }

    @Test
    public void synchronizeMailboxAddOrUpdateMessage__shouldResultInValidStatus() {
        activityListener.synchronizeMailboxStarted(account, folder);
        activityListener.synchronizeMailboxAddOrUpdateMessage(account, folder, message);

        String operation = activityListener.getOperation(RuntimeEnvironment.application);

        assertEquals("Poll account:folder", operation);
    }

    @Test
    public void synchronizeMailboxNewMessage__shouldResultInValidStatus() {
        activityListener.synchronizeMailboxStarted(account, folder);
        activityListener.synchronizeMailboxNewMessage(account, folder, message);

        String operation = activityListener.getOperation(RuntimeEnvironment.application);

        assertEquals("Poll account:folder", operation);
    }
}
