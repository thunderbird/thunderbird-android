package com.fsck.k9.storage.migrations

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase

/**
 * Make sure local Outbox folder has correct 'server_id' value
 */
internal class MigrationTo77(private val db: SQLiteDatabase) {
    fun cleanUpOutboxServerId() {
        val values = ContentValues().apply {
            put("server_id", "K9MAIL_INTERNAL_OUTBOX")
        }

        db.update("folders", values, "name = 'Outbox' AND local_only = 1", null)
    }
}
