package com.fsck.k9.storage.migrations

import android.database.sqlite.SQLiteDatabase

/**
 * Add 'threads' table to 'delete_message' trigger
 */
internal class MigrationTo79(private val db: SQLiteDatabase) {
    fun updateDeleteMessageTrigger() {
        db.execSQL("DROP TRIGGER IF EXISTS delete_message")
        db.execSQL(
            "CREATE TRIGGER delete_message " +
                "BEFORE DELETE ON messages " +
                "BEGIN " +
                "DELETE FROM message_parts WHERE root = OLD.message_part_id; " +
                "DELETE FROM messages_fulltext WHERE docid = OLD.id; " +
                "DELETE FROM threads WHERE message_id = OLD.id; " +
                "END",
        )
    }
}
