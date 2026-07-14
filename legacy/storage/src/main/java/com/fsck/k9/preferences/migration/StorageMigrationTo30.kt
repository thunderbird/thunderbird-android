package com.fsck.k9.preferences.migration

import android.database.sqlite.SQLiteDatabase

private const val ANIMATION_KEY = "animations"

/**
 * Migrate the "animations" setting from boolean string to AnimationPreference enum name.
 *
 * - "true" -> "ON" (user explicitly enabled animations)
 * - "false" -> "OFF" (user explicitly disabled animations)
 * - missing -> no change (new default FOLLOW_SYSTEM will apply)
 */
class StorageMigrationTo30(
    private val db: SQLiteDatabase,
    private val migrationsHelper: StorageMigrationHelper,
) {
    fun migrateAnimationSetting() {
        val currentValue = migrationsHelper.readValue(db, ANIMATION_KEY)
        when (currentValue) {
            "true" -> migrationsHelper.writeValue(db, ANIMATION_KEY, "ON")
            "false" -> migrationsHelper.writeValue(db, ANIMATION_KEY, "OFF")
            // If missing or already migrated, do nothing
        }
    }
}
