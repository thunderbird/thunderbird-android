package com.fsck.k9.preferences.migration

import android.database.sqlite.SQLiteDatabase

/**
 * Change default value of `registeredNameColor` to have enough contrast in both the light and dark theme.
 */
class StorageMigrationTo16(
    private val db: SQLiteDatabase,
    private val migrationsHelper: StorageMigrationsHelper,
) {
    fun changeDefaultRegisteredNameColor() {
        val registeredNameColorValue = migrationsHelper.readValue(db, "registeredNameColor")?.toInt()
        if (registeredNameColorValue == 0xFF00008F.toInt()) {
            migrationsHelper.writeValue(db, "registeredNameColor", 0xFF1093F5.toInt().toString())
        }
    }
}
