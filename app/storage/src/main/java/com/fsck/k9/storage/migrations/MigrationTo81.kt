package com.fsck.k9.storage.migrations

import android.database.sqlite.SQLiteDatabase

/**
 * Add 'notifications' table to keep track of notifications.
 */
internal class MigrationTo81(private val db: SQLiteDatabase) {
    fun addNotificationsTable() {
        db.execSQL("DROP TABLE IF EXISTS notifications")
        db.execSQL(
            "CREATE TABLE notifications (" +
                "message_id INTEGER PRIMARY KEY NOT NULL REFERENCES messages(id) ON DELETE CASCADE," +
                "notification_id INTEGER UNIQUE," +
                "timestamp INTEGER NOT NULL" +
                ")",
        )

        db.execSQL("DROP INDEX IF EXISTS notifications_timestamp")
        db.execSQL("CREATE INDEX IF NOT EXISTS notifications_timestamp ON notifications(timestamp)")
    }
}
