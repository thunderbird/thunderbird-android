package com.fsck.k9.storage

import android.database.sqlite.SQLiteDatabase
import androidx.core.content.contentValuesOf
import app.k9mail.core.android.common.database.map
import app.k9mail.legacy.account.FolderMode
import app.k9mail.legacy.account.LegacyAccount
import assertk.Assert
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotEqualTo
import assertk.assertions.isTrue
import assertk.assertions.support.expected
import assertk.assertions.support.show
import com.fsck.k9.core.BuildConfig
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mailstore.MigrationsHelper
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.shadows.ShadowLog

class StoreSchemaDefinitionTest : RobolectricTest() {
    private val storeSchemaDefinition = createStoreSchemaDefinition()

    @Before
    fun setUp() {
        ShadowLog.stream = System.out
    }

    @Test
    fun `getVersion() should return current database version`() {
        assertThat(storeSchemaDefinition.version).isEqualTo(StoreSchemaDefinition.DB_VERSION)
    }

    @Test
    fun `doDbUpgrade() with empty database should set database version`() {
        val database = SQLiteDatabase.create(null)

        storeSchemaDefinition.doDbUpgrade(database)

        assertThat(database.version).isEqualTo(StoreSchemaDefinition.DB_VERSION)
    }

    @Test
    fun `doDbUpgrade() with bad database should throw in debug build`() {
        if (BuildConfig.DEBUG) {
            val database = SQLiteDatabase.create(null).apply {
                version = 61
            }

            assertFailure {
                storeSchemaDefinition.doDbUpgrade(database)
            }.isInstanceOf<Error>()
                .hasMessage("Exception while upgrading database")
        }
    }

    @Test
    fun `doDbUpgrade() with v61 database should upgrade database to latest version`() {
        val database = createV61Database()

        storeSchemaDefinition.doDbUpgrade(database)

        assertThat(database.version).isEqualTo(StoreSchemaDefinition.DB_VERSION)
    }

    @Test
    fun `doDbUpgrade() with v61 database containing a message`() {
        val database = createV61Database()
        insertMessageWithSubject(database, "Test Email")

        storeSchemaDefinition.doDbUpgrade(database)

        assertMessageWithSubjectExists(database, "Test Email")
    }

    @Test
    fun `doDbUpgrade() from v61 database should result in structure compatible to a fresh install`() {
        val newDatabase = createNewDatabase()
        val upgradedDatabase = createV61Database()

        storeSchemaDefinition.doDbUpgrade(upgradedDatabase)

        assertDatabaseTablesCompatible(newDatabase, upgradedDatabase)
        assertDatabaseTriggersEquals(newDatabase, upgradedDatabase)
        assertDatabaseIndexesEquals(newDatabase, upgradedDatabase)
    }

    private fun createV61Database(): SQLiteDatabase {
        return SQLiteDatabase.create(null).apply {
            initV61Database(this)
        }
    }

    @Suppress("LongMethod")
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
                ")",
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
                ")",
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
                ")",
        )

        db.execSQL(
            "CREATE TRIGGER set_message_part_root " +
                "AFTER INSERT ON message_parts " +
                "BEGIN " +
                "UPDATE message_parts SET root=id WHERE root IS NULL AND ROWID = NEW.ROWID; " +
                "END",
        )

        db.execSQL("CREATE INDEX IF NOT EXISTS msg_uid ON messages (uid, folder_id)")
        db.execSQL("DROP INDEX IF EXISTS msg_folder_id")
        db.execSQL("DROP INDEX IF EXISTS msg_folder_id_date")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS msg_folder_id_deleted_date ON messages (folder_id,deleted,internal_date)",
        )

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
                ")",
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
                "END",
        )

        db.execSQL("DROP TABLE IF EXISTS pending_commands")
        db.execSQL(
            "CREATE TABLE pending_commands " +
                "(id INTEGER PRIMARY KEY, command TEXT, data TEXT)",
        )

        db.execSQL("DROP TRIGGER IF EXISTS delete_folder")
        db.execSQL(
            "CREATE TRIGGER delete_folder BEFORE DELETE ON folders BEGIN " +
                "DELETE FROM messages WHERE old.id = folder_id; " +
                "END;",
        )

        db.execSQL("DROP TRIGGER IF EXISTS delete_message")
        db.execSQL(
            "CREATE TRIGGER delete_message " +
                "BEFORE DELETE ON messages " +
                "BEGIN " +
                "DELETE FROM message_parts WHERE root = OLD.message_part_id; " +
                "DELETE FROM messages_fulltext WHERE docid = OLD.id; " +
                "END",
        )

        db.execSQL("DROP TABLE IF EXISTS messages_fulltext")
        db.execSQL("CREATE VIRTUAL TABLE messages_fulltext USING fts4 (fulltext)")

        db.version = 61

        db.setTransactionSuccessful()
        db.endTransaction()
    }

    private fun assertMessageWithSubjectExists(database: SQLiteDatabase, subject: String) {
        database.query("messages", arrayOf("subject"), null, null, null, null, null).use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.getString(0)).isEqualTo(subject)
        }
    }

    private fun assertDatabaseTablesCompatible(expected: SQLiteDatabase, actual: SQLiteDatabase) {
        // Since not all supported Android versions ship with a SQLite version that supports dropping columns, we don't
        // check for equivalence. Instead we make sure the columns that are present in a new database are also present
        // in an upgraded database.
        val tablesInNewDatabase = tablesInDatabase(expected)
        val tablesInUpgradedDatabase = tablesInDatabase(actual)

        assertThat(tablesInUpgradedDatabase.keys.sorted()).isEqualTo(tablesInNewDatabase.keys.sorted())

        for ((tableName, newTable) in tablesInNewDatabase) {
            val upgradedTable = tablesInUpgradedDatabase[tableName]!!

            assertThat(upgradedTable).isCompatibleTo(newTable)
        }
    }

    private fun Assert<DatabaseTableInfo>.isCompatibleTo(expected: DatabaseTableInfo) = given { actual ->
        if (actual.isVirtualTable != expected.isVirtualTable) {
            expected("table '${actual.tableName}' to be a virtual table")
        }

        if (actual.usingClause != expected.usingClause) {
            expected("table '${actual.tableName}' to have USING clause: ${show(expected.usingClause)}")
        }

        for (newColumnDefinition in expected.columnDefinitions) {
            if (newColumnDefinition !in actual.columnDefinitions) {
                expected(
                    "table '${actual.tableName}' to contain ${show(newColumnDefinition)} " +
                        "but was ${show(actual.columnDefinitions)}",
                )
            }
        }
    }

    private fun assertDatabaseTriggersEquals(expected: SQLiteDatabase, actual: SQLiteDatabase) {
        val triggersInNewDatabase = triggersInDatabase(expected).sorted()
        val triggersInUpgradedDatabase = triggersInDatabase(actual).sorted()

        assertThat(triggersInUpgradedDatabase).isEqualTo(triggersInNewDatabase)
    }

    private fun assertDatabaseIndexesEquals(expected: SQLiteDatabase, actual: SQLiteDatabase) {
        val indexesInNewDatabase = indexesInDatabase(expected).sorted()
        val indexesInUpgradedDatabase = indexesInDatabase(actual).sorted()

        assertThat(indexesInUpgradedDatabase).isEqualTo(indexesInNewDatabase)
    }

    private fun tablesInDatabase(db: SQLiteDatabase): Map<String, DatabaseTableInfo> {
        return objectsInDatabase(db, "table")
            .map { extractColumns(it) }
            .associateBy { it.tableName }
    }

    private fun triggersInDatabase(db: SQLiteDatabase): List<String> {
        return objectsInDatabase(db, "trigger")
    }

    private fun indexesInDatabase(db: SQLiteDatabase): List<String> {
        return objectsInDatabase(db, "index")
    }

    private fun objectsInDatabase(db: SQLiteDatabase, type: String): List<String> {
        return db.rawQuery(
            "SELECT sql FROM sqlite_master WHERE type = ? AND sql IS NOT NULL",
            arrayOf(type),
        ).use { cursor ->
            cursor.map {
                cursor.getString(cursor.getColumnIndex("sql"))
            }
        }
    }

    private fun extractColumns(sql: String): DatabaseTableInfo {
        val matchResult = """CREATE (VIRTUAL)?\s*TABLE\s*('[^']+'|[^ ]+)\s*(USING [^ ]+)?\s*\((.+)\)""".toRegex()
            .matchEntire(sql) ?: error("Can't parse SQL: $sql")

        val isVirtualTable = matchResult.groups[1] != null
        val tableName = matchResult.groups[2]!!.value.removeSurrounding("'")
        val usingClause = matchResult.groups[3]?.value
        val columnDefinitionsSql = matchResult.groups[4]!!.value
        val columnDefinitions = columnDefinitionsSql
            .split(" *, *(?![^(]*\\))".toRegex())
            .dropLastWhile { it.isEmpty() }
            .sorted()

        return DatabaseTableInfo(tableName, isVirtualTable, usingClause, columnDefinitions)
    }

    private fun insertMessageWithSubject(database: SQLiteDatabase, subject: String) {
        val data = contentValuesOf(
            "subject" to subject,
        )

        val rowId = database.insert("messages", null, data)

        assertThat(rowId).isNotEqualTo(-1L)
    }

    private fun createStoreSchemaDefinition(): StoreSchemaDefinition {
        val account = createAccount()
        val migrationsHelper = object : MigrationsHelper {
            override fun getAccount(): LegacyAccount {
                return account
            }

            override fun saveAccount() {
                // Do nothing
            }
        }

        return StoreSchemaDefinition(migrationsHelper)
    }

    private fun createAccount(): LegacyAccount {
        return mock<LegacyAccount> {
            on { legacyInboxFolder } doReturn "Inbox"
            on { importedTrashFolder } doReturn "Trash"
            on { importedDraftsFolder } doReturn "Drafts"
            on { importedSpamFolder } doReturn "Spam"
            on { importedSentFolder } doReturn "Sent"
            on { importedArchiveFolder } doReturn null

            on { incomingServerSettings } doReturn ServerSettings(
                type = "dummy",
                host = "",
                port = -1,
                connectionSecurity = ConnectionSecurity.NONE,
                authenticationType = AuthType.PLAIN,
                username = "",
                password = "",
                clientCertificateAlias = null,
            )

            on { folderNotifyNewMailMode } doReturn FolderMode.ALL
            on { folderPushMode } doReturn FolderMode.ALL
            on { folderSyncMode } doReturn FolderMode.ALL
            on { folderDisplayMode } doReturn FolderMode.ALL
        }
    }

    private fun createNewDatabase(): SQLiteDatabase {
        return SQLiteDatabase.create(null).also { database ->
            storeSchemaDefinition.doDbUpgrade(database)
        }
    }
}

private data class DatabaseTableInfo(
    val tableName: String,
    val isVirtualTable: Boolean,
    val usingClause: String?,
    val columnDefinitions: List<String>,
)
