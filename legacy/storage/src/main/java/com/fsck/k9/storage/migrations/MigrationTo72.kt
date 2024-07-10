package com.fsck.k9.storage.migrations

import android.database.sqlite.SQLiteDatabase

internal class MigrationTo72(private val db: SQLiteDatabase) {
    fun createMessagePartsRootIndex() {
        db.execSQL("DROP INDEX IF EXISTS message_parts_root")
        db.execSQL("CREATE INDEX IF NOT EXISTS message_parts_root ON message_parts (root)")
    }
}
