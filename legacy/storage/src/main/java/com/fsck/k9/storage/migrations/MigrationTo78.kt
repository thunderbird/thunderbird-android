package com.fsck.k9.storage.migrations

import android.database.sqlite.SQLiteDatabase

/**
 * Set 'server_id' value to NULL for local folders
 */
internal class MigrationTo78(private val db: SQLiteDatabase) {
    fun removeServerIdFromLocalFolders() {
        db.execSQL("UPDATE folders SET server_id = NULL WHERE local_only = 1")
    }
}
