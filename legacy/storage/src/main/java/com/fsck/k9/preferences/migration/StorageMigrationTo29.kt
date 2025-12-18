package com.fsck.k9.preferences.migration

import android.database.sqlite.SQLiteDatabase
import net.thunderbird.feature.mail.folder.api.ArchiveGranularity

/**
 * Migration to add archive granularity setting for existing accounts.
 * Uses MIGRATION_DEFAULT (SINGLE_ARCHIVE_FOLDER) for backward compatibility.
 */
class StorageMigrationTo29(
    private val db: SQLiteDatabase,
    private val migrationsHelper: StorageMigrationHelper,
) {
    fun setDefaultArchiveGranularity() {
        val accountUuidsValue = migrationsHelper.readValue(db, "accountUuids")
        if (accountUuidsValue.isNullOrEmpty()) {
            return
        }

        val accountUuids = accountUuidsValue.split(",")
        for (accountUuid in accountUuids) {
            setArchiveGranularityForAccount(accountUuid)
        }
    }

    private fun setArchiveGranularityForAccount(accountUuid: String) {
        val existingValue = migrationsHelper.readValue(db, "$accountUuid.$ARCHIVE_GRANULARITY_KEY")
        if (existingValue == null) {
            migrationsHelper.insertValue(
                db,
                "$accountUuid.$ARCHIVE_GRANULARITY_KEY",
                ArchiveGranularity.MIGRATION_DEFAULT.name,
            )
        }
    }

    private companion object {
        const val ARCHIVE_GRANULARITY_KEY = "archiveGranularity"
    }
}
