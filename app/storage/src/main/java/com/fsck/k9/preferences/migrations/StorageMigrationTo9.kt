package com.fsck.k9.preferences.migrations

import android.database.sqlite.SQLiteDatabase

/**
 * Temporarily disable Push (see GH-4253)
 *
 * Since the plan is to re-enable Push support in the future, we don't actually touch the Push settings. But we
 * configure "poll folders" so folders that have previously used Push will now be polled.
 */
class StorageMigrationTo9(
    private val db: SQLiteDatabase,
    private val migrationsHelper: StorageMigrationsHelper
) {
    fun disablePush() {
        val accountUuidsListValue = migrationsHelper.readValue(db, "accountUuids")
        if (accountUuidsListValue == null || accountUuidsListValue.isEmpty()) {
            return
        }

        val accountUuids = accountUuidsListValue.split(",")
        for (accountUuid in accountUuids) {
            setNewFolderSyncModeForAccount(accountUuid)
        }
    }

    private fun setNewFolderSyncModeForAccount(accountUuid: String) {
        val folderSyncMode = migrationsHelper.readValue(db, "$accountUuid.folderSyncMode")
        val folderPushMode = migrationsHelper.readValue(db, "$accountUuid.folderPushMode")

        val newFolderSyncMode = when {
            folderSyncMode == ALL && folderPushMode == ALL -> ALL
            folderSyncMode == ALL && folderPushMode == FIRST_CLASS -> ALL
            folderSyncMode == ALL && folderPushMode == FIRST_AND_SECOND_CLASS -> ALL
            folderSyncMode == ALL && folderPushMode == NOT_SECOND_CLASS -> ALL
            folderSyncMode == ALL && folderPushMode == NONE -> ALL
            folderSyncMode == FIRST_CLASS && folderPushMode == ALL -> ALL
            folderSyncMode == FIRST_CLASS && folderPushMode == FIRST_CLASS -> FIRST_CLASS
            folderSyncMode == FIRST_CLASS && folderPushMode == FIRST_AND_SECOND_CLASS -> FIRST_AND_SECOND_CLASS
            folderSyncMode == FIRST_CLASS && folderPushMode == NOT_SECOND_CLASS -> NOT_SECOND_CLASS
            folderSyncMode == FIRST_CLASS && folderPushMode == NONE -> FIRST_CLASS
            folderSyncMode == FIRST_AND_SECOND_CLASS && folderPushMode == ALL -> ALL
            folderSyncMode == FIRST_AND_SECOND_CLASS && folderPushMode == FIRST_CLASS -> FIRST_AND_SECOND_CLASS
            folderSyncMode == FIRST_AND_SECOND_CLASS && folderPushMode == FIRST_AND_SECOND_CLASS -> FIRST_AND_SECOND_CLASS
            folderSyncMode == FIRST_AND_SECOND_CLASS && folderPushMode == NOT_SECOND_CLASS -> ALL
            folderSyncMode == FIRST_AND_SECOND_CLASS && folderPushMode == NONE -> FIRST_AND_SECOND_CLASS
            folderSyncMode == NOT_SECOND_CLASS && folderPushMode == ALL -> ALL
            folderSyncMode == NOT_SECOND_CLASS && folderPushMode == FIRST_CLASS -> NOT_SECOND_CLASS
            folderSyncMode == NOT_SECOND_CLASS && folderPushMode == FIRST_AND_SECOND_CLASS -> ALL
            folderSyncMode == NOT_SECOND_CLASS && folderPushMode == NOT_SECOND_CLASS -> NOT_SECOND_CLASS
            folderSyncMode == NOT_SECOND_CLASS && folderPushMode == NONE -> NOT_SECOND_CLASS
            folderSyncMode == NONE && folderPushMode == ALL -> ALL
            folderSyncMode == NONE && folderPushMode == FIRST_CLASS -> FIRST_CLASS
            folderSyncMode == NONE && folderPushMode == FIRST_AND_SECOND_CLASS -> FIRST_AND_SECOND_CLASS
            folderSyncMode == NONE && folderPushMode == NOT_SECOND_CLASS -> NOT_SECOND_CLASS
            folderSyncMode == NONE && folderPushMode == NONE -> NONE
            else -> FIRST_CLASS
        }

        migrationsHelper.writeValue(db, "$accountUuid.folderSyncMode", newFolderSyncMode)

        if (folderPushMode != NONE) {
            migrationsHelper.writeValue(db, "$accountUuid.automaticCheckIntervalMinutes", LOWEST_FREQUENCY_SUPPORTED)
        }
    }

    companion object {
        private const val NONE = "NONE"
        private const val ALL = "ALL"
        private const val FIRST_CLASS = "FIRST_CLASS"
        private const val FIRST_AND_SECOND_CLASS = "FIRST_AND_SECOND_CLASS"
        private const val NOT_SECOND_CLASS = "NOT_SECOND_CLASS"

        private const val LOWEST_FREQUENCY_SUPPORTED = "15"
    }
}
