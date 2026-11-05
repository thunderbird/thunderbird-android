package com.fsck.k9.storage.migrations

import android.database.sqlite.SQLiteDatabase
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsOnly
import assertk.assertions.doesNotContain
import assertk.assertions.isEqualTo
import assertk.assertions.isZero
import com.fsck.k9.storage.messages.FolderEntry
import com.fsck.k9.storage.messages.readFolders
import kotlin.test.Test
import org.junit.After
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MigrationTo92Test {
    private val database = createDatabaseVersion91()
    private val migration = MigrationTo92(database)

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `should remove the integrate column and keep all other folder data`() {
        // Arrange
        database.execSQL(
            """
            INSERT INTO folders (id, name, last_updated, unread_count, visible_limit, status, flagged_count, integrate,
                top_group, sync_enabled, push_enabled, visible, notifications_enabled, more_messages, server_id,
                local_only, type, account_id)
            VALUES (7, 'Inbox', 123, 4, 25, 'ok', 2, 1, 1, 1, 1, 0, 1, 'true', 'INBOX', 0, 'inbox', 'account')
            """.trimIndent(),
        )

        // Act
        migration.removeIntegrateColumn()

        // Assert
        assertThat(database.columnNames("folders")).doesNotContain("integrate")
        assertThat(database.readFolders()).containsExactly(
            FolderEntry(
                id = 7,
                name = "Inbox",
                type = "inbox",
                serverId = "INBOX",
                isLocalOnly = 0,
                inTopGroup = 1,
                visible = 0,
                syncEnabled = 1,
                notificationsEnabled = 1,
                pushEnabled = 1,
                lastUpdated = 123,
                unreadCount = 4,
                visibleLimit = 25,
                status = "ok",
                flaggedCount = 2,
                moreMessages = "true",
                accountId = "account",
            ),
        )
    }

    @Test
    fun `should recreate the folder indexes`() {
        migration.removeIntegrateColumn()

        assertThat(database.indexNames("folders")).containsOnly("folder_server_id", "folders_account_id")
    }

    @Test
    fun `deleting a folder should still delete its messages and extra values`() {
        // Arrange
        database.execSQL("INSERT INTO folders (id, name) VALUES (1, 'Folder')")
        database.execSQL("INSERT INTO messages (id, folder_id) VALUES (10, 1)")
        database.execSQL("INSERT INTO folder_extra_values (folder_id, name, value_text) VALUES (1, 'key', 'value')")
        migration.removeIntegrateColumn()

        // Act
        database.execSQL("DELETE FROM folders WHERE id = 1")

        // Assert
        assertThat(database.count("messages")).isZero()
        assertThat(database.count("folder_extra_values")).isZero()
    }

    private fun SQLiteDatabase.columnNames(table: String): List<String> =
        rawQuery("PRAGMA table_info($table)", null).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
            }
        }

    private fun SQLiteDatabase.indexNames(table: String): List<String> =
        rawQuery("SELECT name FROM sqlite_master WHERE type = 'index' AND tbl_name = ?", arrayOf(table)).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.getString(0))
            }
        }

    private fun SQLiteDatabase.count(table: String): Int =
        rawQuery("SELECT COUNT(*) FROM $table", null).use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }

    private fun createDatabaseVersion91(): SQLiteDatabase {
        return SQLiteDatabase.create(null).apply {
            execSQL(
                """
                CREATE TABLE folders (
                    id INTEGER PRIMARY KEY,
                    name TEXT,
                    last_updated INTEGER,
                    unread_count INTEGER,
                    visible_limit INTEGER,
                    status TEXT,
                    flagged_count INTEGER default 0,
                    integrate INTEGER,
                    top_group INTEGER,
                    sync_enabled INTEGER DEFAULT 0,
                    push_enabled INTEGER DEFAULT 0,
                    visible INTEGER DEFAULT 1,
                    notifications_enabled INTEGER DEFAULT 0,
                    more_messages TEXT default "unknown",
                    server_id TEXT,
                    local_only INTEGER,
                    type TEXT DEFAULT "regular",
                    account_id TEXT
                )
                """.trimIndent(),
            )
            execSQL("CREATE INDEX folder_server_id ON folders (server_id)")
            execSQL("CREATE INDEX folders_account_id ON folders(account_id)")
            execSQL("CREATE TABLE messages (id INTEGER PRIMARY KEY, folder_id INTEGER)")
            execSQL(
                "CREATE TABLE folder_extra_values (folder_id INTEGER NOT NULL, name TEXT NOT NULL, " +
                    "value_text TEXT, value_integer INTEGER, PRIMARY KEY (folder_id, name))",
            )
        }
    }
}
