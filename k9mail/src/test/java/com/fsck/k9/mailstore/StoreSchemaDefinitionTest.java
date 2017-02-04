package com.fsck.k9.mailstore;


import java.util.ArrayList;
import java.util.Collections;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.fsck.k9.Account;
import com.fsck.k9.BuildConfig;
import com.fsck.k9.GlobalsHelper;
import com.fsck.k9.K9;
import com.fsck.k9.mail.MessagingException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class StoreSchemaDefinitionTest {
    private StoreSchemaDefinition ssd;
    private LocalStore localStore;
    private Account account;
    private LockableDatabase storeDb;


    @Before
    public void before() throws MessagingException {
        ShadowLog.stream = System.out;
        localStore = mock(LocalStore.class);
        account = mock(Account.class);
        storeDb = mock(LockableDatabase.class);
        when(storeDb.execute(anyBoolean(), any(LockableDatabase.DbCallback.class))).thenReturn(false);

        localStore.database = storeDb;

        when(localStore.getAccount()).thenReturn(account);
        when(account.getInboxFolderName()).thenReturn("Inbox");
        when(account.getLocalStorageProviderId()).thenReturn(StorageManager.InternalStorageProvider.ID);
        ssd = new StoreSchemaDefinition(localStore);
        GlobalsHelper.setContext(RuntimeEnvironment.application);
        K9.app = RuntimeEnvironment.application;
        StorageManager.getInstance(RuntimeEnvironment.application);
    }

    @Test
    public void getVersion__returnsDBVersion() {
        int result = ssd.getVersion();

        assertEquals(LocalStore.DB_VERSION, result);
    }

    @Test
    public void doDbUpgrade_withEmptyDB_setsDBVersion() {
        SQLiteDatabase sqliteDb = SQLiteDatabase.create(null);

        ssd.doDbUpgrade(sqliteDb);

        assertEquals(LocalStore.DB_VERSION, sqliteDb.getVersion());
    }

    @Test
    public void doDbUpgrade_withBadDatabase_throwsErrorInDebug() {
        if (BuildConfig.DEBUG) {
            SQLiteDatabase sqliteDb = SQLiteDatabase.create(null);
            sqliteDb.setVersion(29);

            try {
                ssd.doDbUpgrade(sqliteDb);
                fail("Expected Error");
            } catch (Error e) {
                assertEquals("Exception while upgrading database", e.getMessage());
            }
        }
    }

    @Test
    public void doDbUpgrade_withV29_upgradesDBToLatestVersion() {
        SQLiteDatabase sqliteDb = SQLiteDatabase.create(null);
        createV29Database(sqliteDb);

        ssd.doDbUpgrade(sqliteDb);

        assertEquals(LocalStore.DB_VERSION, sqliteDb.getVersion());
    }

    @Test
    public void doDbUpgrade_withV29_upgradesDB() {
        SQLiteDatabase sqliteDb = SQLiteDatabase.create(null);
        createV29Database(sqliteDb);
        ContentValues data = new ContentValues();
        data.put("subject", "Test Email");

        System.out.println(sqliteDb.getVersion());

        long returnVal = sqliteDb.insert("messages", null, data);

        if (returnVal == -1) {
            fail("Error occured");
        }

        ssd.doDbUpgrade(sqliteDb);
        Cursor c = sqliteDb.query("messages", new String[] { "subject" }, null, null, null, null, null);
        boolean isNotEmpty = c.moveToFirst();
        assertTrue(isNotEmpty);
    }

    @Test
    public void doDbUpgrade_fromV29_resultsInSameTables() {
        SQLiteDatabase sqliteDbNew = SQLiteDatabase.create(null);
        ssd.doDbUpgrade(sqliteDbNew);
        ArrayList<String> tablesInNewDatabase = tablesInDatabase(sqliteDbNew);
        Collections.sort(tablesInNewDatabase);

        SQLiteDatabase sqliteDbUpgrade = SQLiteDatabase.create(null);
        createV29Database(sqliteDbUpgrade);
        ssd.doDbUpgrade(sqliteDbUpgrade);
        ArrayList<String> tablesInUpgradedDatabase = tablesInDatabase(sqliteDbUpgrade);
        Collections.sort(tablesInUpgradedDatabase);

        assertEquals(tablesInNewDatabase, tablesInUpgradedDatabase);
    }

    @Test
    public void doDbUpgrade_fromV29_resultsInSameTriggers() {
        SQLiteDatabase sqliteDbNew = SQLiteDatabase.create(null);
        ssd.doDbUpgrade(sqliteDbNew);
        ArrayList<String> triggersInNewDatabase = triggersInDatabase(sqliteDbNew);
        Collections.sort(triggersInNewDatabase);

        SQLiteDatabase sqliteDbUpgrade = SQLiteDatabase.create(null);
        createV29Database(sqliteDbUpgrade);
        ssd.doDbUpgrade(sqliteDbUpgrade);
        ArrayList<String> triggersInUpgradedDatabase = triggersInDatabase(sqliteDbUpgrade);
        Collections.sort(triggersInUpgradedDatabase);

        assertEquals(triggersInNewDatabase, triggersInUpgradedDatabase);
    }

    @Test
    public void doDbUpgrade_fromV29_resultsInSameIndexes() {
        SQLiteDatabase sqliteDbNew = SQLiteDatabase.create(null);
        ssd.doDbUpgrade(sqliteDbNew);
        ArrayList<String> indexesInNewDatabase = indexesInDatabase(sqliteDbNew);
        Collections.sort(indexesInNewDatabase);

        SQLiteDatabase sqliteDbUpgrade = SQLiteDatabase.create(null);
        createV29Database(sqliteDbUpgrade);
        ssd.doDbUpgrade(sqliteDbUpgrade);
        ArrayList<String> indexesInUpgradedDatabase = indexesInDatabase(sqliteDbUpgrade);
        Collections.sort(indexesInUpgradedDatabase);

        assertEquals(indexesInNewDatabase, indexesInUpgradedDatabase);
    }

    private void createV29Database(SQLiteDatabase db) {
        /*
         * There is no precise definition of a v29 database. This function approximates it by creating a database
         * that could be upgraded to the latest database as of v58
         */

        db.beginTransaction();
        db.execSQL("CREATE TABLE folders (" +
                "id INTEGER PRIMARY KEY," +
                "name TEXT, " +
                "last_updated INTEGER, " +
                "unread_count INTEGER, " +
                "visible_limit INTEGER, " +
                "status TEXT, " +
                "push_state TEXT, " +
                "last_pushed INTEGER " +
                ")");

        db.execSQL("CREATE INDEX folder_name ON folders (name)");
        db.execSQL("CREATE TABLE messages (" +
                "id INTEGER PRIMARY KEY, " +
                "folder_id INTEGER, " +
                "uid TEXT, " +
                "subject TEXT, " +
                "date INTEGER, " +
                "flags TEXT, " +
                "sender_list TEXT, " +
                "to_list TEXT, " +
                "cc_list TEXT, " +
                "bcc_list TEXT, " +
                "reply_to_list TEXT, " +
                "attachment_count INTEGER, " +
                "internal_date INTEGER, " +
                "message_id TEXT, " +
                "html_content TEXT, " +
                "text_content TEXT, " +
                "preview_type TEXT default \"none\", " +
                "message_part_id INTEGER" +
                ")");

        db.execSQL("CREATE TABLE attachments (id INTEGER PRIMARY KEY, " +
                "size INTEGER, " +
                "name TEXT, " +
                "mime_type TEXT, " +
                "store_data TEXT, " +
                "content_uri TEXT, " +
                "message_id INTEGER" +
                ")");
        db.execSQL("CREATE TABLE headers (id INTEGER PRIMARY KEY, " +
                "name TEXT, " +
                "value TEXT, " +
                "message_id INTEGER" +
                ")");

        db.execSQL("CREATE INDEX msg_uid ON messages (uid, folder_id)");

        db.execSQL("CREATE TABLE threads (" +
                "id INTEGER PRIMARY KEY, " +
                "message_id INTEGER, " +
                "root INTEGER, " +
                "parent INTEGER" +
                ")");
        db.execSQL("CREATE INDEX threads_message_id ON threads (message_id)");

        db.execSQL("CREATE INDEX threads_root ON threads (root)");

        db.execSQL("CREATE INDEX threads_parent ON threads (parent)");

        db.execSQL("CREATE TRIGGER set_thread_root " +
                "AFTER INSERT ON threads " +
                "BEGIN " +
                "UPDATE threads SET root=id WHERE root IS NULL AND ROWID = NEW.ROWID; " +
                "END");

        db.execSQL("CREATE TABLE pending_commands " +
                "(id INTEGER PRIMARY KEY, command TEXT, arguments TEXT)");

        db.execSQL("CREATE TRIGGER delete_folder BEFORE DELETE ON folders BEGIN DELETE FROM messages WHERE old.id = folder_id; END;");

        db.execSQL("CREATE TRIGGER delete_message " +
                "BEFORE DELETE ON messages " +
                "BEGIN " +
                "DELETE FROM message_parts WHERE root = OLD.message_part_id; " +
                "DELETE FROM messages_fulltext WHERE docid = OLD.id; " +
                "END");

        db.setVersion(29);

        db.setTransactionSuccessful();

        db.endTransaction();
    }

    private ArrayList<String> tablesInDatabase(SQLiteDatabase db) {
        return objectsInDatabase(db, "table");
    }

    private ArrayList<String> triggersInDatabase(SQLiteDatabase db) {
        return objectsInDatabase(db, "trigger");
    }

    private ArrayList<String> indexesInDatabase(SQLiteDatabase db) {
        return objectsInDatabase(db, "index");
    }

    private ArrayList<String> objectsInDatabase(SQLiteDatabase db, String type) {
        ArrayList<String> tables = new ArrayList<>();
        Cursor c = db.rawQuery("SELECT name FROM sqlite_master WHERE type = '" + type + "'", null);
        if (c.moveToFirst()) {
            while (!c.isAfterLast()) {
                tables.add(c.getString(c.getColumnIndex("name")));
                c.moveToNext();
            }
        }
        return tables;
    }
}
