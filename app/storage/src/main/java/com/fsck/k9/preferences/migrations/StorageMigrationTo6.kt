package com.fsck.k9.preferences.migrations

import android.database.sqlite.SQLiteDatabase

/**
 * Perform legacy conversions that previously lived in `K9`.
 */
class StorageMigrationTo6(
    private val db: SQLiteDatabase,
    private val migrationsHelper: StorageMigrationsHelper
) {
    fun performLegacyMigrations() {
        rewriteKeyguardPrivacy()
        rewriteTheme()
    }

    private fun rewriteKeyguardPrivacy() {
        val notificationHideSubject = migrationsHelper.readValue(db, "notificationHideSubject")
        if (notificationHideSubject == null) {
            val keyguardPrivacy = migrationsHelper.readValue(db, "keyguardPrivacy")
            if (keyguardPrivacy?.toBoolean() == true) {
                migrationsHelper.writeValue(db, "notificationHideSubject", "WHEN_LOCKED")
            } else {
                migrationsHelper.writeValue(db, "notificationHideSubject", "NEVER")
            }
        }
    }

    private fun rewriteTheme() {
        val theme = migrationsHelper.readValue(db, "theme")?.toInt()

        // We used to save the resource ID of the theme. So convert that to the new format if necessary.
        val newTheme = if (theme == THEME_ORDINAL_DARK || theme == android.R.style.Theme) {
            THEME_ORDINAL_DARK
        } else {
            THEME_ORDINAL_LIGHT
        }

        migrationsHelper.writeValue(db, "theme", newTheme.toString())
    }

    companion object {
        private const val THEME_ORDINAL_LIGHT = 0
        private const val THEME_ORDINAL_DARK = 1
    }
}
