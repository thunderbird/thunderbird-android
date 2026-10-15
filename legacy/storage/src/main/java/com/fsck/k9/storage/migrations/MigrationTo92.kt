package com.fsck.k9.storage.migrations

import android.database.sqlite.SQLiteDatabase

/**
 * Removes the `integrate` column ("Include in Unified Inbox") from the `folders` table.
 *
 * The unified inbox now shows each account's inbox. SQLite on older Android versions can't drop a column, so the table
 * is rebuilt.
 */
internal class MigrationTo92(private val db: SQLiteDatabase) {
    fun removeIntegrateColumn() {
        db.execSQL("ALTER TABLE folders RENAME TO folders_old")
        createFoldersTable()
        copyFoldersData()
        db.execSQL("DROP TABLE folders_old")
        createFoldersIndexes()
        createFoldersTriggers()
    }

    private fun createFoldersTable() {
        db.execSQL(
            "CREATE TABLE folders (" +
                "id INTEGER PRIMARY KEY," +
                "name TEXT, " +
                "last_updated INTEGER, " +
                "unread_count INTEGER, " +
                "visible_limit INTEGER, " +
                "status TEXT, " +
                "flagged_count INTEGER default 0, " +
                "top_group INTEGER, " +
                "sync_enabled INTEGER DEFAULT 0, " +
                "push_enabled INTEGER DEFAULT 0, " +
                "visible INTEGER DEFAULT 1, " +
                "notifications_enabled INTEGER DEFAULT 0, " +
                "more_messages TEXT default \"unknown\", " +
                "server_id TEXT, " +
                "local_only INTEGER, " +
                "type TEXT DEFAULT \"regular\", " +
                "account_id TEXT" +
                ")",
        )
    }

    private fun copyFoldersData() {
        db.execSQL(
            """
            INSERT INTO folders
            SELECT
                id,
                name,
                last_updated,
                unread_count,
                visible_limit,
                status,
                flagged_count,
                top_group,
                sync_enabled,
                push_enabled,
                visible,
                notifications_enabled,
                more_messages,
                server_id,
                local_only,
                type,
                account_id
            FROM folders_old
            """.trimIndent(),
        )
    }

    private fun createFoldersIndexes() {
        db.execSQL("DROP INDEX IF EXISTS folder_server_id")
        db.execSQL("CREATE INDEX folder_server_id ON folders (server_id)")
        db.execSQL("DROP INDEX IF EXISTS folders_account_id")
        db.execSQL("CREATE INDEX folders_account_id ON folders(account_id)")
    }

    private fun createFoldersTriggers() {
        db.execSQL("DROP TRIGGER IF EXISTS delete_folder")
        db.execSQL(
            "CREATE TRIGGER delete_folder " +
                "BEFORE DELETE ON folders " +
                "BEGIN " +
                "DELETE FROM messages WHERE old.id = folder_id; " +
                "END;",
        )

        db.execSQL("DROP TRIGGER IF EXISTS delete_folder_extra_values")
        db.execSQL(
            "CREATE TRIGGER delete_folder_extra_values " +
                "BEFORE DELETE ON folders " +
                "BEGIN " +
                "DELETE FROM folder_extra_values WHERE old.id = folder_id; " +
                "END;",
        )
    }
}
