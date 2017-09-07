package com.fsck.k9.activity;


import android.content.Context;

import com.fsck.k9.Account;
import com.fsck.k9.K9RobolectricTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@RunWith(K9RobolectricTestRunner.class)
public class FolderInfoHolderTest {

    private Context context;
    private Account mockAccount;

    @Before
    public void before() {
        context = RuntimeEnvironment.application;
        mockAccount = mock(Account.class);
    }

    @Test
    public void getDisplayName_forUnknownFolder_returnsName() {
        String result = FolderInfoHolder.getDisplayName(context, mockAccount, "FolderID", "Folder");

        assertEquals("Folder", result);
    }

    @Test
    public void getDisplayName_forSpamFolder_returnsNameSpam() {
        when(mockAccount.getSpamFolderId()).thenReturn("FolderID");

        String result = FolderInfoHolder.getDisplayName(context, mockAccount, "FolderID", "Folder");

        assertEquals("Folder (Spam)", result);
    }

    @Test
    public void getDisplayName_forOutboxFolder_returnsOutbox() {
        when(mockAccount.getOutboxFolderId()).thenReturn("FolderID");

        String result = FolderInfoHolder.getDisplayName(context, mockAccount, "FolderID", "Folder");

        assertEquals("Outbox", result);
    }

    @Test
    public void getDisplayName_forInboxFolder_returnsInbox() {
        when(mockAccount.getInboxFolderId()).thenReturn("FolderID");

        String result = FolderInfoHolder.getDisplayName(context, mockAccount, "FolderID", "Folder");

        assertEquals("Inbox", result);
    }

    @Test
    public void getDisplayName_forInboxFolderAlternativeCase_returnsInbox() {
        when(mockAccount.getInboxFolderId()).thenReturn("FOLDERID");

        String result = FolderInfoHolder.getDisplayName(context, mockAccount, "FolderID", "Folder");

        assertEquals("Inbox", result);
    }
}
