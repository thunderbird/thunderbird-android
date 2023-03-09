package com.fsck.k9.storage.migrations

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.core.database.getLongOrNull
import app.k9mail.core.android.common.database.map
import com.fsck.k9.mailstore.MigrationsHelper

private const val EXTRA_HIGHEST_KNOWN_UID = "imapHighestKnownUid"

/**
 * Write the highest known IMAP message UID to the 'folder_extra_values' table.
 */
internal class MigrationTo83(private val db: SQLiteDatabase, private val migrationsHelper: MigrationsHelper) {
    fun rewriteHighestKnownUid() {
        if (migrationsHelper.account.incomingServerSettings.type != "imap") return

        val highestKnownUids = db.rawQuery(
            "SELECT folder_id, MAX(CAST(uid AS INTEGER)) FROM messages GROUP BY folder_id",
            null,
        ).use { cursor ->
            cursor.map {
                it.getLong(0) to it.getLongOrNull(1)
            }.toMap()
        }

        for ((folderId, highestKnownUid) in highestKnownUids) {
            if (highestKnownUid != null && highestKnownUid > 0L) {
                rewriteHighestKnownUid(folderId, highestKnownUid)
            }
        }
    }

    private fun rewriteHighestKnownUid(folderId: Long, highestKnownUid: Long) {
        val contentValues = ContentValues().apply {
            put("folder_id", folderId)
            put("name", EXTRA_HIGHEST_KNOWN_UID)
            put("value_integer", highestKnownUid)
        }
        db.insertWithOnConflict("folder_extra_values", null, contentValues, SQLiteDatabase.CONFLICT_REPLACE)
    }
}
