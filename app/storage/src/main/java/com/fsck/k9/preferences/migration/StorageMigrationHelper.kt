package com.fsck.k9.preferences.migration

import android.database.sqlite.SQLiteDatabase

interface StorageMigrationHelper {
    fun readAllValues(db: SQLiteDatabase): Map<String, String>
    fun readValue(db: SQLiteDatabase, key: String): String?
    fun writeValue(db: SQLiteDatabase, key: String, value: String?)
    fun insertValue(db: SQLiteDatabase, key: String, value: String?)
}
