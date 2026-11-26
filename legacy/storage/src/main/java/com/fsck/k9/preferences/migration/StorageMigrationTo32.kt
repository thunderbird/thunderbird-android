package com.fsck.k9.preferences.migration

import android.database.sqlite.SQLiteDatabase

class StorageMigrationTo32(
    private val db: SQLiteDatabase,
    private val migrationsHelper: StorageMigrationHelper,
) {

    private val fundingKeys = listOf(
        "fundingReminderReferenceTimestamp",
        "fundingReminderShownTimestamp",
        "fundingActivityCounterInMillis",
    )

    fun migrateK9FundingRemoval() {
        fundingKeys.forEach { removeKey(it) }
    }

    private fun removeKey(key: String) {
        migrationsHelper.writeValue(db, key, null)
    }
}
