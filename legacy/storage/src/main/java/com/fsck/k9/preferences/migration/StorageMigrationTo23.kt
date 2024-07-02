package com.fsck.k9.preferences.migration

import android.database.sqlite.SQLiteDatabase

/**
 * Rename account setting `sendClientId` to `sendClientInfo`.
 */
class StorageMigrationTo23(
    private val db: SQLiteDatabase,
    private val migrationsHelper: StorageMigrationHelper,
) {
    fun renameSendClientId() {
        val accountUuidsListValue = migrationsHelper.readValue(db, "accountUuids")
        if (accountUuidsListValue.isNullOrEmpty()) {
            return
        }

        val accountUuids = accountUuidsListValue.split(",")
        for (accountUuid in accountUuids) {
            renameSendClientIdForAccount(accountUuid)
        }
    }

    private fun renameSendClientIdForAccount(accountUuid: String) {
        // Write new key with existing value
        val value = migrationsHelper.readValue(db, "$accountUuid.sendClientId")
        migrationsHelper.insertValue(db, "$accountUuid.sendClientInfo", value)

        // Remove old key
        migrationsHelper.writeValue(db, "$accountUuid.sendClientId", null)
    }
}
