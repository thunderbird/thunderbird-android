package com.fsck.k9.mailstore;


import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;

import com.fsck.k9.K9RobolectricTestRunner;
import com.fsck.k9.mail.Folder.FolderClass;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mailstore.LocalFolder.MoreMessages;
import com.fsck.k9.mailstore.LockableDatabase.DbCallback;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static junit.framework.Assert.assertEquals;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;


@SuppressWarnings("WeakerAccess")
@RunWith(K9RobolectricTestRunner.class)
public class LocalFolderTest {
    static final long FOLDER_DATABASE_ID = 123L;
    static final String[] FOLDER_COLS = {
            "folders.id", "name", "visible_limit", "last_updated", "status", "push_state", "last_pushed",
                "integrate", "top_group", "poll_class", "push_class", "display_class", "notify_class", "more_messages" };
    static final String MSG_UID_1 = "uid1";
    static final String MSG_UID_2 = "uid2";


    static final String FOLDER_NAME = "folnam";
    static final Integer FOLDER_VISIBLE_LIMIT = 100;
    static final Long FOLDER_LAST_CHECKED = 1000L;
    static final String FOLDER_STATUS = "folderstatus";
    static final String FOLDER_PUSH_STATE = "pushstate";
    static final Long FOLDER_LAST_PUSHED = 1010L;
    static final Integer FOLDER_INTEGRATE = 0;
    static final Integer FOLDER_TOP_GROUP = 0;
    static final String FOLDER_SYNC_CLASS = FolderClass.NO_CLASS.toString();
    static final String FOLDER_PUSH_CLASS = FolderClass.NO_CLASS.toString();
    static final String FOLDER_DISPLAY_CLASS = FolderClass.NO_CLASS.toString();
    static final String FOLDER_NOTIFY_CLASS = FolderClass.NO_CLASS.toString();
    static final String FOLDER_MORE_MESSAGES = MoreMessages.FALSE.getDatabaseName();


    LocalFolder localFolder;
    LocalStore localStore;
    LockableDatabase database;


    @Before
    public void setUp() throws Exception {
        database = mock(LockableDatabase.class);

        localStore = mock(LocalStore.class);
        when(localStore.getDatabase()).thenReturn(database);

        localFolder = new LocalFolder(localStore, FOLDER_DATABASE_ID);
        localFolder = spy(localFolder);
    }

    private void openFolderWithParams(LocalFolder folder, int folderVisibleLimit) throws MessagingException {
        MatrixCursor cursor = new MatrixCursor(FOLDER_COLS);
        cursor.addRow(new Object[] {
                FOLDER_DATABASE_ID, FOLDER_NAME, folderVisibleLimit, FOLDER_LAST_CHECKED, FOLDER_STATUS,
                FOLDER_PUSH_STATE, FOLDER_LAST_PUSHED, FOLDER_INTEGRATE, FOLDER_TOP_GROUP, FOLDER_SYNC_CLASS,
                FOLDER_PUSH_CLASS, FOLDER_DISPLAY_CLASS, FOLDER_NOTIFY_CLASS, FOLDER_MORE_MESSAGES
        });
        cursor.moveToFirst();

        folder.open(cursor);
    }


    @Test
    public void openFolder() throws Exception {
        openFolderWithParams(localFolder, FOLDER_VISIBLE_LIMIT);

        assertEquals(FOLDER_DATABASE_ID, localFolder.getDatabaseId());
        assertEquals(FOLDER_NAME, localFolder.getName());
        assertEquals((int) FOLDER_VISIBLE_LIMIT, localFolder.getVisibleLimit());
        assertEquals((long) FOLDER_LAST_CHECKED, localFolder.getLastChecked());
        assertEquals(FOLDER_STATUS, localFolder.getStatus());
        assertEquals(FOLDER_PUSH_STATE, localFolder.getPushState());
        assertEquals((long) FOLDER_LAST_PUSHED, localFolder.getLastPush());
        assertEquals(false, localFolder.isIntegrate());
        assertEquals(false, localFolder.isInTopGroup());
        assertEquals(FolderClass.NO_CLASS, localFolder.getSyncClass());
        assertEquals(FolderClass.NO_CLASS, localFolder.getPushClass());
        assertEquals(FolderClass.NO_CLASS, localFolder.getDisplayClass());
        assertEquals(FolderClass.NO_CLASS, localFolder.getNotifyClass());
        assertEquals(MoreMessages.FALSE, localFolder.getMoreMessages());
    }

    @Test
    public void purgeToVisibleLimit() throws Exception {
        openFolderWithParams(localFolder, FOLDER_VISIBLE_LIMIT);

        LocalMessage localMessageToDestroy1 = mock(LocalMessage.class);
        LocalMessage localMessageToDestroy2 = mock(LocalMessage.class);

        MatrixCursor uidsToDeleteCursor = new MatrixCursor(new String[] { "uid" } );
        uidsToDeleteCursor.addRow(new String[] { MSG_UID_1 });
        uidsToDeleteCursor.addRow(new String[] { MSG_UID_2 });

        final SQLiteDatabase returnSingleUidMockDatabase = mock(SQLiteDatabase.class);
        when(returnSingleUidMockDatabase.rawQuery(contains("LIMIT -1 OFFSET ?"), aryEq(new String[] {
                Long.toString(FOLDER_DATABASE_ID), Integer.toString(FOLDER_VISIBLE_LIMIT)
        }))).thenReturn(uidsToDeleteCursor);

        // we expect the first callback to query for uids, and the subsequent two to query for LocalMessages
        when(database.execute(eq(false), any(DbCallback.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                DbCallback callback = invocation.getArgumentAt(1, DbCallback.class);
                callback.doDbWork(returnSingleUidMockDatabase);
                return null;
            }
        }).thenReturn(localMessageToDestroy1, localMessageToDestroy2);

        MessageRemovalListener messageRemovalListener = mock(MessageRemovalListener.class);


        localFolder.purgeToVisibleLimit(messageRemovalListener);


        verify(messageRemovalListener).messageRemoved(localMessageToDestroy1);
        verify(messageRemovalListener).messageRemoved(localMessageToDestroy2);
        verify(localFolder).destroyMessage(localMessageToDestroy1);
        verify(localFolder).destroyMessage(localMessageToDestroy2);
    }

    @Test
    public void purgeToVisibleLimit_withNoLimit() throws Exception {
        openFolderWithParams(localFolder, 0);

        localFolder.purgeToVisibleLimit(null);

        verifyNoMoreInteractions(database);
    }
}
