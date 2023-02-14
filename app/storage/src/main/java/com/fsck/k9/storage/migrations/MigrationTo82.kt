package com.fsck.k9.storage.migrations

import android.database.sqlite.SQLiteDatabase

/**
 * Add 'new_message' column to 'messages' table.
 */
internal class MigrationTo82(private val db: SQLiteDatabase) {
    fun addNewMessageColumn() {
        db.execSQL("ALTER TABLE messages ADD new_message INTEGER DEFAULT 0")

        db.execSQL("DROP INDEX IF EXISTS new_messages")
        db.execSQL("CREATE INDEX IF NOT EXISTS new_messages ON messages(new_message)")

        db.execSQL(
            "CREATE TRIGGER new_message_reset " +
                "AFTER UPDATE OF read ON messages " +
                "FOR EACH ROW WHEN NEW.read = 1 AND NEW.new_message = 1 " +
                "BEGIN " +
                "UPDATE messages SET new_message = 0 WHERE ROWID = NEW.ROWID; " +
                "END",
        )

        // Mark messages with existing notifications as "new"
        db.execSQL("UPDATE messages SET new_message = 1 WHERE id in (SELECT message_id FROM notifications)")
    }
}
