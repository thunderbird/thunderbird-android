package com.fsck.k9.preferences.migration

import android.database.sqlite.SQLiteDatabase

/**
 * Add `*FolderSelection` values of "MANUAL" for existing accounts (default for new accounts is "AUTOMATIC").
 */
class StorageMigrationTo4(
    private val db: SQLiteDatabase,
    private val migrationsHelper: StorageMigrationHelper,
) {
    fun insertSpecialFolderSelectionValues() {
        val accountUuidsListValue = migrationsHelper.readValue(db, "accountUuids")
        if (accountUuidsListValue == null || accountUuidsListValue.isEmpty()) {
            return
        }

        val accountUuids = accountUuidsListValue.split(",")
        for (accountUuid in accountUuids) {
            insertSpecialFolderSelectionValues(accountUuid)
        }
    }

    private fun insertSpecialFolderSelectionValues(accountUuid: String) {
        migrationsHelper.insertValue(db, "$accountUuid.archiveFolderSelection", "MANUAL")
        migrationsHelper.insertValue(db, "$accountUuid.draftsFolderSelection", "MANUAL")
        migrationsHelper.insertValue(db, "$accountUuid.sentFolderSelection", "MANUAL")
        migrationsHelper.insertValue(db, "$accountUuid.spamFolderSelection", "MANUAL")
        migrationsHelper.insertValue(db, "$accountUuid.trashFolderSelection", "MANUAL")
    }
}
