package com.fsck.k9.storage.migrations

import android.database.sqlite.SQLiteDatabase

internal object MigrationTo68 {
    @JvmStatic
    fun addOutboxStateTable(db: SQLiteDatabase) {
        createOutboxStateTable(db)
        createOutboxStateEntries(db)
    }

    private fun createOutboxStateTable(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE outbox_state (" +
                "message_id INTEGER PRIMARY KEY NOT NULL REFERENCES messages(id) ON DELETE CASCADE," +
                "send_state TEXT," +
                "number_of_send_attempts INTEGER DEFAULT 0," +
                "error_timestamp INTEGER DEFAULT 0," +
                "error TEXT)",
        )
    }

    private fun createOutboxStateEntries(db: SQLiteDatabase) {
        db.execSQL(
            """
            INSERT INTO outbox_state (message_id, send_state)
              SELECT messages.id, 'ready' FROM folders
                JOIN messages ON (folders.id = messages.folder_id)
                WHERE folders.server_id = 'K9MAIL_INTERNAL_OUTBOX'
            """.trimIndent(),
        )
    }
}
