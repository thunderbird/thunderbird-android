package com.fsck.k9.preferences.migration

import android.database.sqlite.SQLiteDatabase

/**
 * Rewrite folder name values of "-NONE-" to `null`
 */
class StorageMigrationTo3(
    private val db: SQLiteDatabase,
    private val migrationsHelper: StorageMigrationsHelper,
) {
    fun rewriteFolderNone() {
        val accountUuidsListValue = migrationsHelper.readValue(db, "accountUuids")
        if (accountUuidsListValue == null || accountUuidsListValue.isEmpty()) {
            return
        }

        val accountUuids = accountUuidsListValue.split(",")
        for (accountUuid in accountUuids) {
            rewriteAccount(accountUuid)
        }
    }

    private fun rewriteAccount(accountUuid: String) {
        rewriteFolderValue("$accountUuid.archiveFolderName")
        rewriteFolderValue("$accountUuid.autoExpandFolderName")
        rewriteFolderValue("$accountUuid.draftsFolderName")
        rewriteFolderValue("$accountUuid.sentFolderName")
        rewriteFolderValue("$accountUuid.spamFolderName")
        rewriteFolderValue("$accountUuid.trashFolderName")
    }

    private fun rewriteFolderValue(key: String) {
        val folderValue = migrationsHelper.readValue(db, key)
        if (folderValue == OLD_FOLDER_VALUE) {
            migrationsHelper.writeValue(db, key, NEW_FOLDER_VALUE)
        }
    }

    companion object {
        private const val OLD_FOLDER_VALUE = "-NONE-"
        private val NEW_FOLDER_VALUE = null
    }
}
