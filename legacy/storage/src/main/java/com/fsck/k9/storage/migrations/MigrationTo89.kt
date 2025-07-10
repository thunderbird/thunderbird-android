package com.fsck.k9.storage.migrations

import android.database.sqlite.SQLiteDatabase
import com.fsck.k9.mailstore.MigrationsHelper

/**
 * Migration to version 89.
 *
 * Adds the `account_id` column to the `folders` and `messages` tables.
 */
internal class MigrationTo89(private val db: SQLiteDatabase, private val migrationsHelper: MigrationsHelper) {
    fun addAccountIdColumn() {
        db.execSQL("ALTER TABLE folders ADD account_id TEXT")
        db.execSQL("ALTER TABLE messages ADD account_id TEXT")

        val accountUuid = migrationsHelper.account.uuid
        db.execSQL("UPDATE messages SET account_id = ?", arrayOf(accountUuid))
        db.execSQL("UPDATE folders SET account_id = ?", arrayOf(accountUuid))

        db.execSQL("CREATE INDEX IF NOT EXISTS folders_account_id ON folders(account_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS messages_account_id ON messages(account_id)")
    }
}
