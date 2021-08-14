package com.fsck.k9.storage.migrations

import android.database.sqlite.SQLiteDatabase

/**
 * Rewrite 'last_update' column to NULL when the value is 0
 */
internal class MigrationTo80(private val db: SQLiteDatabase) {
    fun rewriteLastUpdatedColumn() {
        db.execSQL("UPDATE folders SET last_updated = NULL WHERE last_updated = 0")
    }
}
