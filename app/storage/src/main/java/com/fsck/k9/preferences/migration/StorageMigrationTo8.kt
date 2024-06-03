package com.fsck.k9.preferences.migration

import android.database.sqlite.SQLiteDatabase

/**
 * Rewrite theme setting to use `FOLLOW_SYSTEM` when it's currently set to `LIGHT`.
 */
class StorageMigrationTo8(
    private val db: SQLiteDatabase,
    private val migrationsHelper: StorageMigrationsHelper,
) {
    fun rewriteTheme() {
        val theme = migrationsHelper.readValue(db, "theme")
        if (theme == THEME_LIGHT) {
            migrationsHelper.writeValue(db, "theme", THEME_FOLLOW_SYSTEM)
        }
    }

    companion object {
        private const val THEME_LIGHT = "LIGHT"
        private const val THEME_FOLLOW_SYSTEM = "FOLLOW_SYSTEM"
    }
}
