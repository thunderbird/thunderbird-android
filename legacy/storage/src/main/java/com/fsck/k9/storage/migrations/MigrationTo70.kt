package com.fsck.k9.storage.migrations

import android.database.sqlite.SQLiteDatabase
import com.fsck.k9.mail.FolderClass

internal class MigrationTo70(private val db: SQLiteDatabase) {
    fun removePushState() {
        renameFoldersTable()
        createNewFoldersTable()
        copyFoldersData()
        dropOldFoldersTable()
        recreateFoldersIndex()
        recreateFoldersTriggers()
    }

    private fun renameFoldersTable() {
        db.execSQL("ALTER TABLE folders RENAME TO folders_old")
    }

    private fun createNewFoldersTable() {
        db.execSQL(
            "CREATE TABLE folders (" +
                "id INTEGER PRIMARY KEY," +
                "name TEXT, " +
                "last_updated INTEGER, " +
                "unread_count INTEGER, " +
                "visible_limit INTEGER, " +
                "status TEXT, " +
                "flagged_count INTEGER default 0, " +
                "integrate INTEGER, " +
                "top_group INTEGER, " +
                "poll_class TEXT, " +
                "push_class TEXT, " +
                "display_class TEXT, " +
                "notify_class TEXT default '" + FolderClass.INHERITED.name + "', " +
                "more_messages TEXT default \"unknown\", " +
                "server_id TEXT, " +
                "local_only INTEGER, " +
                "type TEXT DEFAULT \"regular\"" +
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
                integrate,
                top_group,
                poll_class,
                push_class,
                display_class,
                notify_class,
                more_messages,
                server_id,
                local_only,
                type
            FROM folders_old 
            """.trimIndent(),
        )
    }

    private fun dropOldFoldersTable() {
        db.execSQL("DROP TABLE folders_old")
    }

    private fun recreateFoldersIndex() {
        db.execSQL("DROP INDEX IF EXISTS folder_server_id")
        db.execSQL("CREATE INDEX folder_server_id ON folders (server_id)")
    }

    private fun recreateFoldersTriggers() {
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
