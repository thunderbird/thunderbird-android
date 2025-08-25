package com.fsck.k9.storage.migrations

import android.database.sqlite.SQLiteDatabase

internal class MigrationTo89(private val db: SQLiteDatabase) {
    fun addMessageSizeColumn() {
        db.execSQL("ALTER TABLE messages ADD size INTEGER")
    }
}
