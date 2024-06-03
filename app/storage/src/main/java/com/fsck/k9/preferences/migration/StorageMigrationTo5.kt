package com.fsck.k9.preferences.migration

import android.database.sqlite.SQLiteDatabase

/**
 * Rewrite frequencies lower than LOWEST_FREQUENCY_SUPPORTED
 */
class StorageMigrationTo5(
    private val db: SQLiteDatabase,
    private val migrationsHelper: StorageMigrationsHelper,
) {
    fun fixMailCheckFrequencies() {
        val accountUuidsListValue = migrationsHelper.readValue(db, "accountUuids")
        if (accountUuidsListValue == null || accountUuidsListValue.isEmpty()) {
            return
        }

        val accountUuids = accountUuidsListValue.split(",")
        for (accountUuid in accountUuids) {
            fixFrequencyForAccount(accountUuid)
        }
    }

    private fun fixFrequencyForAccount(accountUuid: String) {
        val key = "$accountUuid.automaticCheckIntervalMinutes"
        val frequencyValue = migrationsHelper.readValue(db, key)?.toIntOrNull()
        if (frequencyValue != null && frequencyValue > -1 && frequencyValue < LOWEST_FREQUENCY_SUPPORTED) {
            migrationsHelper.writeValue(db, key, LOWEST_FREQUENCY_SUPPORTED.toString())
        }
    }

    companion object {
        // see: https://github.com/evernote/android-job/wiki/FAQ#why-cant-an-interval-be-smaller-than-15-minutes-for-periodic-jobs
        private const val LOWEST_FREQUENCY_SUPPORTED = 15
    }
}
