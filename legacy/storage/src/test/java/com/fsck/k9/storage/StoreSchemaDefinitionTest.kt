package com.fsck.k9.storage

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.text.TextUtils
import app.k9mail.legacy.account.Account
import com.fsck.k9.core.BuildConfig
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.MessagingException
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mailstore.LocalStore
import com.fsck.k9.mailstore.LockableDatabase
import com.fsck.k9.mailstore.LockableDatabase.DbCallback
import com.fsck.k9.mailstore.MigrationsHelper
import com.fsck.k9.mailstore.StorageManager
import java.util.Arrays
import java.util.Collections
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.robolectric.RuntimeEnvironment
import org.robolectric.shadows.ShadowLog

class StoreSchemaDefinitionTest : K9RobolectricTest() {
    private var storeSchemaDefinition: StoreSchemaDefinition? = null

    @Before
    @Throws(MessagingException::class)
    fun setUp() {
        ShadowLog.stream = System.out

        val application = RuntimeEnvironment.getApplication()
        StorageManager.getInstance(application)

        storeSchemaDefinition = createStoreSchemaDefinition()
    }

    @Test
    fun getVersion_shouldReturnCurrentDatabaseVersion() {
        val version = storeSchemaDefinition!!.version

        Assert.assertEquals(StoreSchemaDefinition.DB_VERSION.toLong(), version.toLong())
    }

    @Test
    fun doDbUpgrade_withEmptyDatabase_shouldSetsDatabaseVersion() {
        val database = SQLiteDatabase.create(null)

        storeSchemaDefinition!!.doDbUpgrade(database)

        Assert.assertEquals(StoreSchemaDefinition.DB_VERSION.toLong(), database.version.toLong())
    }

    @Test
    fun doDbUpgrade_withBadDatabase_shouldThrowInDebugBuild() {
        if (BuildConfig.DEBUG) {
            val database = SQLiteDatabase.create(null)
            database.version = 61

            try {
                storeSchemaDefinition!!.doDbUpgrade(database)
                Assert.fail("Expected Error")
            } catch (e: Error) {
                Assert.assertEquals("Exception while upgrading database", e.message)
            }
        }
    }

    @Test
    fun doDbUpgrade_withV61_shouldUpgradeDatabaseToLatestVersion() {
        val database = createV61Database()

        storeSchemaDefinition!!.doDbUpgrade(database)

        Assert.assertEquals(StoreSchemaDefinition.DB_VERSION.toLong(), database.version.toLong())
    }

    @Test
    fun doDbUpgrade_withV61() {
        val database = createV61Database()
        insertMessageWithSubject(database, "Test Email")

        storeSchemaDefinition!!.doDbUpgrade(database)

        assertMessageWithSubjectExists(database, "Test Email")
    }

    @Test
    fun doDbUpgrade_fromV61_shouldResultInSameTables() {
        val newDatabase = createNewDatabase()
        val upgradedDatabase = createV61Database()

        storeSchemaDefinition!!.doDbUpgrade(upgradedDatabase)

        assertDatabaseTablesEquals(newDatabase, upgradedDatabase)
    }

    @Test
    fun doDbUpgrade_fromV61_shouldResultInSameTriggers() {
        val newDatabase = createNewDatabase()
        val upgradedDatabase = createV61Database()

        storeSchemaDefinition!!.doDbUpgrade(upgradedDatabase)

        assertDatabaseTriggersEquals(newDatabase, upgradedDatabase)
    }

    @Test
    fun doDbUpgrade_fromV61_shouldResultInSameIndexes() {
        val newDatabase = createNewDatabase()
        val upgradedDatabase = createV61Database()

        storeSchemaDefinition!!.doDbUpgrade(upgradedDatabase)

        assertDatabaseIndexesEquals(newDatabase, upgradedDatabase)
    }

    private fun createV61Database(): SQLiteDatabase {
        val database = SQLiteDatabase.create(null)
        initV61Database(database)
        return database
    }

    private fun initV61Database(db: SQLiteDatabase) {
        db.beginTransaction()

        db.execSQL("DROP TABLE IF EXISTS folders")
        db.execSQL(
            "CREATE TABLE folders (" +
                "id INTEGER PRIMARY KEY," +
                "name TEXT, " +
                "last_updated INTEGER, " +
                "unread_count INTEGER, " +
                "visible_limit INTEGER, " +
                "status TEXT, " +
                "push_state TEXT, " +
                "last_pushed INTEGER, " +
                "flagged_count INTEGER default 0, " +
                "integrate INTEGER, " +
                "top_group INTEGER, " +
                "poll_class TEXT, " +
                "push_class TEXT, " +
                "display_class TEXT, " +
                "notify_class TEXT default 'INHERITED', " +
                "more_messages TEXT default \"unknown\"" +
                ")"
        )

        db.execSQL("CREATE INDEX IF NOT EXISTS folder_name ON folders (name)")
        db.execSQL("DROP TABLE IF EXISTS messages")
        db.execSQL(
            "CREATE TABLE messages (" +
                "id INTEGER PRIMARY KEY, " +
                "deleted INTEGER default 0, " +
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
                "preview_type TEXT default \"none\", " +
                "preview TEXT, " +
                "mime_type TEXT, " +
                "normalized_subject_hash INTEGER, " +
                "empty INTEGER default 0, " +
                "read INTEGER default 0, " +
                "flagged INTEGER default 0, " +
                "answered INTEGER default 0, " +
                "forwarded INTEGER default 0, " +
                "message_part_id INTEGER" +
                ")"
        )

        db.execSQL("DROP TABLE IF EXISTS message_parts")
        db.execSQL(
            "CREATE TABLE message_parts (" +
                "id INTEGER PRIMARY KEY, " +
                "type INTEGER NOT NULL, " +
                "root INTEGER, " +
                "parent INTEGER NOT NULL, " +
                "seq INTEGER NOT NULL, " +
                "mime_type TEXT, " +
                "decoded_body_size INTEGER, " +
                "display_name TEXT, " +
                "header TEXT, " +
                "encoding TEXT, " +
                "charset TEXT, " +
                "data_location INTEGER NOT NULL, " +
                "data BLOB, " +
                "preamble TEXT, " +
                "epilogue TEXT, " +
                "boundary TEXT, " +
                "content_id TEXT, " +
                "server_extra TEXT" +
                ")"
        )

        db.execSQL(
            "CREATE TRIGGER set_message_part_root " +
                "AFTER INSERT ON message_parts " +
                "BEGIN " +
                "UPDATE message_parts SET root=id WHERE root IS NULL AND ROWID = NEW.ROWID; " +
                "END"
        )

        db.execSQL("CREATE INDEX IF NOT EXISTS msg_uid ON messages (uid, folder_id)")
        db.execSQL("DROP INDEX IF EXISTS msg_folder_id")
        db.execSQL("DROP INDEX IF EXISTS msg_folder_id_date")
        db.execSQL("CREATE INDEX IF NOT EXISTS msg_folder_id_deleted_date ON messages (folder_id,deleted,internal_date)")

        db.execSQL("DROP INDEX IF EXISTS msg_empty")
        db.execSQL("CREATE INDEX IF NOT EXISTS msg_empty ON messages (empty)")

        db.execSQL("DROP INDEX IF EXISTS msg_read")
        db.execSQL("CREATE INDEX IF NOT EXISTS msg_read ON messages (read)")

        db.execSQL("DROP INDEX IF EXISTS msg_flagged")
        db.execSQL("CREATE INDEX IF NOT EXISTS msg_flagged ON messages (flagged)")

        db.execSQL("DROP INDEX IF EXISTS msg_composite")
        db.execSQL("CREATE INDEX IF NOT EXISTS msg_composite ON messages (deleted, empty,folder_id,flagged,read)")


        db.execSQL("DROP TABLE IF EXISTS threads")
        db.execSQL(
            "CREATE TABLE threads (" +
                "id INTEGER PRIMARY KEY, " +
                "message_id INTEGER, " +
                "root INTEGER, " +
                "parent INTEGER" +
                ")"
        )

        db.execSQL("DROP INDEX IF EXISTS threads_message_id")
        db.execSQL("CREATE INDEX IF NOT EXISTS threads_message_id ON threads (message_id)")

        db.execSQL("DROP INDEX IF EXISTS threads_root")
        db.execSQL("CREATE INDEX IF NOT EXISTS threads_root ON threads (root)")

        db.execSQL("DROP INDEX IF EXISTS threads_parent")
        db.execSQL("CREATE INDEX IF NOT EXISTS threads_parent ON threads (parent)")

        db.execSQL("DROP TRIGGER IF EXISTS set_thread_root")
        db.execSQL(
            "CREATE TRIGGER set_thread_root " +
                "AFTER INSERT ON threads " +
                "BEGIN " +
                "UPDATE threads SET root=id WHERE root IS NULL AND ROWID = NEW.ROWID; " +
                "END"
        )

        db.execSQL("DROP TABLE IF EXISTS pending_commands")
        db.execSQL(
            "CREATE TABLE pending_commands " +
                "(id INTEGER PRIMARY KEY, command TEXT, data TEXT)"
        )

        db.execSQL("DROP TRIGGER IF EXISTS delete_folder")
        db.execSQL("CREATE TRIGGER delete_folder BEFORE DELETE ON folders BEGIN DELETE FROM messages WHERE old.id = folder_id; END;")

        db.execSQL("DROP TRIGGER IF EXISTS delete_message")
        db.execSQL(
            "CREATE TRIGGER delete_message " +
                "BEFORE DELETE ON messages " +
                "BEGIN " +
                "DELETE FROM message_parts WHERE root = OLD.message_part_id; " +
                "DELETE FROM messages_fulltext WHERE docid = OLD.id; " +
                "END"
        )

        db.execSQL("DROP TABLE IF EXISTS messages_fulltext")
        db.execSQL("CREATE VIRTUAL TABLE messages_fulltext USING fts4 (fulltext)")

        db.version = 61

        db.setTransactionSuccessful()
        db.endTransaction()
    }

    private fun assertMessageWithSubjectExists(database: SQLiteDatabase, subject: String) {
        val cursor = database.query("messages", arrayOf("subject"), null, null, null, null, null)
        try {
            Assert.assertTrue(cursor.moveToFirst())
            Assert.assertEquals(subject, cursor.getString(0))
        } finally {
            cursor.close()
        }
    }

    private fun assertDatabaseTablesEquals(expected: SQLiteDatabase, actual: SQLiteDatabase) {
        val tablesInNewDatabase = tablesInDatabase(expected)
        Collections.sort(tablesInNewDatabase)

        val tablesInUpgradedDatabase = tablesInDatabase(actual)
        Collections.sort(tablesInUpgradedDatabase)

        Assert.assertEquals(tablesInNewDatabase, tablesInUpgradedDatabase)
    }

    private fun assertDatabaseTriggersEquals(expected: SQLiteDatabase, actual: SQLiteDatabase) {
        val triggersInNewDatabase = triggersInDatabase(expected)
        Collections.sort(triggersInNewDatabase)

        val triggersInUpgradedDatabase = triggersInDatabase(actual)
        Collections.sort(triggersInUpgradedDatabase)

        Assert.assertEquals(triggersInNewDatabase, triggersInUpgradedDatabase)
    }

    private fun assertDatabaseIndexesEquals(expected: SQLiteDatabase, actual: SQLiteDatabase) {
        val indexesInNewDatabase = indexesInDatabase(expected)
        Collections.sort(indexesInNewDatabase)

        val indexesInUpgradedDatabase = indexesInDatabase(actual)
        Collections.sort(indexesInUpgradedDatabase)

        Assert.assertEquals(indexesInNewDatabase, indexesInUpgradedDatabase)
    }

    private fun tablesInDatabase(db: SQLiteDatabase): List<String> {
        return objectsInDatabase(db, "table")
    }

    private fun triggersInDatabase(db: SQLiteDatabase): List<String> {
        return objectsInDatabase(db, "trigger")
    }

    private fun indexesInDatabase(db: SQLiteDatabase): List<String> {
        return objectsInDatabase(db, "index")
    }

    private fun objectsInDatabase(db: SQLiteDatabase, type: String): List<String> {
        val databaseObjects: MutableList<String> = ArrayList()
        val cursor = db.rawQuery(
            "SELECT sql FROM sqlite_master WHERE type = ? AND sql IS NOT NULL",
            arrayOf(type)
        )
        try {
            while (cursor.moveToNext()) {
                val sql = cursor.getString(cursor.getColumnIndex("sql"))
                val resortedSql = if ("table" == type) sortTableColumns(sql) else sql
                databaseObjects.add(resortedSql)
            }
        } finally {
            cursor.close()
        }

        return databaseObjects
    }

    private fun sortTableColumns(sql: String): String {
        val positionOfColumnDefinitions = sql.indexOf('(')
        val columnDefinitionsSql = sql.substring(positionOfColumnDefinitions + 1, sql.length - 1)
        val columnDefinitions =
            columnDefinitionsSql.split(" *, *(?![^(]*\\))".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        Arrays.sort(columnDefinitions)

        val sqlPrefix = sql.substring(0, positionOfColumnDefinitions + 1)
        val sortedColumnDefinitionsSql = TextUtils.join(", ", columnDefinitions)
        return "$sqlPrefix$sortedColumnDefinitionsSql)"
    }

    private fun insertMessageWithSubject(database: SQLiteDatabase, subject: String) {
        val data = ContentValues()
        data.put("subject", subject)
        val rowId = database.insert("messages", null, data)
        Assert.assertNotEquals(-1, rowId)
    }

    @Throws(MessagingException::class)
    private fun createStoreSchemaDefinition(): StoreSchemaDefinition {
        val account = createAccount()
        val lockableDatabase = createLockableDatabase()
        val localStore = Mockito.mock(LocalStore::class.java)
        Mockito.`when`(localStore.database).thenReturn(lockableDatabase)

        val migrationsHelper: MigrationsHelper = object : MigrationsHelper {
            override fun getAccount(): Account {
                return account
            }

            override fun saveAccount() {
                // Do nothing
            }
        }

        return StoreSchemaDefinition(migrationsHelper)
    }

    @Throws(MessagingException::class)
    private fun createLockableDatabase(): LockableDatabase {
        val lockableDatabase = Mockito.mock(LockableDatabase::class.java)
        Mockito.`when`(
            lockableDatabase.execute(
                ArgumentMatchers.anyBoolean(), ArgumentMatchers.any<DbCallback<Any>>()
            )
        ).thenReturn(false)
        return lockableDatabase
    }

    private fun createAccount(): Account {
        val account = Mockito.mock(Account::class.java)
        Mockito.`when`(account.legacyInboxFolder).thenReturn("Inbox")
        Mockito.`when`(account.importedTrashFolder).thenReturn("Trash")
        Mockito.`when`(account.importedDraftsFolder).thenReturn("Drafts")
        Mockito.`when`(account.importedSpamFolder).thenReturn("Spam")
        Mockito.`when`(account.importedSentFolder).thenReturn("Sent")
        Mockito.`when`(account.importedArchiveFolder).thenReturn(null)
        Mockito.`when`(account.localStorageProviderId).thenReturn(StorageManager.InternalStorageProvider.ID)

        val incomingServerSettings = ServerSettings(
            "dummy", "", -1, ConnectionSecurity.NONE,
            AuthType.PLAIN, "", "", null
        )
        Mockito.`when`(account.incomingServerSettings).thenReturn(incomingServerSettings)
        return account
    }

    private fun createNewDatabase(): SQLiteDatabase {
        val database = SQLiteDatabase.create(null)
        storeSchemaDefinition!!.doDbUpgrade(database)
        return database
    }
}
