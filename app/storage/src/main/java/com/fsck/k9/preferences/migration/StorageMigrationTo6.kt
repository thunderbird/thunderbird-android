package com.fsck.k9.preferences.migration

import android.database.sqlite.SQLiteDatabase

/**
 * Perform legacy conversions that previously lived in `K9`.
 */
class StorageMigrationTo6(
    private val db: SQLiteDatabase,
    private val migrationsHelper: StorageMigrationsHelper,
) {
    fun performLegacyMigrations() {
        rewriteTheme()
        migrateOpenPgpGlobalToAccountSettings()
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

    private fun migrateOpenPgpGlobalToAccountSettings() {
        val accountUuidsListValue = migrationsHelper.readValue(db, "accountUuids")
        if (accountUuidsListValue == null || accountUuidsListValue.isEmpty()) {
            return
        }

        val openPgpProvider = migrationsHelper.readValue(db, "openPgpProvider") ?: ""
        val openPgpSupportSignOnly = migrationsHelper.readValue(db, "openPgpSupportSignOnly")?.toBoolean() ?: false
        val openPgpHideSignOnly = (!openPgpSupportSignOnly).toString()

        val accountUuids = accountUuidsListValue.split(",")
        for (accountUuid in accountUuids) {
            migrationsHelper.writeValue(db, "$accountUuid.openPgpProvider", openPgpProvider)
            migrationsHelper.writeValue(db, "$accountUuid.openPgpHideSignOnly", openPgpHideSignOnly)
        }

        migrationsHelper.writeValue(db, "openPgpProvider", null)
        migrationsHelper.writeValue(db, "openPgpSupportSignOnly", null)
    }

    companion object {
        private const val THEME_ORDINAL_LIGHT = 0
        private const val THEME_ORDINAL_DARK = 1
    }
}
