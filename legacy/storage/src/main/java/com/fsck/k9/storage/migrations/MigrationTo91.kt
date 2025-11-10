package com.fsck.k9.storage.migrations

import android.database.sqlite.SQLiteDatabase
import com.fsck.k9.mailstore.MigrationsHelper

/**
 * Migration to version 91. Was planed as version 89 but got skipped, to do 90 first
 *
 * Adds the `account_id` column to the `folders` and `messages` tables.
 */
internal class MigrationTo91(private val db: SQLiteDatabase, private val migrationsHelper: MigrationsHelper) {

    fun columnExists(tableName: String, columnName: String): Boolean {
        val cursor = db.rawQuery("PRAGMA table_info($tableName)", null)
        cursor.use {
            while (it.moveToNext()) {
                val currentColumnName = it.getString(it.getColumnIndexOrThrow("name"))
                if (currentColumnName == columnName) {
                    return true
                }
            }
        }
        return false
    }

    fun addAccountIdColumn() {
        if (!columnExists("folders", "account_id")) {
            db.execSQL("ALTER TABLE folders ADD account_id TEXT")
        }

        if (!columnExists("messages", "account_id")) {
            db.execSQL("ALTER TABLE messages ADD account_id TEXT")
        }

        val accountUuid = migrationsHelper.account.uuid
        db.execSQL("UPDATE messages SET account_id = ?", arrayOf(accountUuid))
        db.execSQL("UPDATE folders SET account_id = ?", arrayOf(accountUuid))

        db.execSQL("CREATE INDEX IF NOT EXISTS folders_account_id ON folders(account_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS messages_account_id ON messages(account_id)")
    }
}
