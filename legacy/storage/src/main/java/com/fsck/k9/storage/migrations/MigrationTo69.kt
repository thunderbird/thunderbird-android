package com.fsck.k9.storage.migrations

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.fsck.k9.mail.Flag
import com.squareup.moshi.Moshi

internal class MigrationTo69(private val db: SQLiteDatabase) {
    fun createPendingDelete() {
        val moshi = Moshi.Builder().build()
        val pendingSetFlagAdapter = moshi.adapter(LegacyPendingSetFlag::class.java)

        val pendingSetFlagsToConvert = mutableMapOf<Long, LegacyPendingSetFlag>()

        db.rawQuery("SELECT id, data FROM pending_commands WHERE command = 'set_flag'", null).use { cursor ->
            while (cursor.moveToNext()) {
                val databaseId = cursor.getLong(0)
                val data = cursor.getString(1)

                val pendingSetFlag = pendingSetFlagAdapter.fromJson(data) ?: error("Can't deserialize pending command")
                if (pendingSetFlag.flag == Flag.DELETED && pendingSetFlag.newState) {
                    pendingSetFlagsToConvert[databaseId] = pendingSetFlag
                }
            }
        }

        val pendingDeleteAdapter = moshi.adapter(LegacyPendingDelete::class.java)

        for ((databaseId, pendingSetFlag) in pendingSetFlagsToConvert) {
            val pendingDelete = LegacyPendingDelete.create(pendingSetFlag.folder, pendingSetFlag.uids)
            val contentValues = ContentValues().apply {
                put("command", "delete")
                put("data", pendingDeleteAdapter.toJson(pendingDelete))
            }

            db.update("pending_commands", contentValues, "id = ?", arrayOf(databaseId.toString()))
        }
    }
}
