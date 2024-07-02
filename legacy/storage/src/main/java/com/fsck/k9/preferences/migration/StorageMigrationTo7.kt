package com.fsck.k9.preferences.migration

import android.database.sqlite.SQLiteDatabase

/**
 * Rewrite settings to use enum names instead of ordinals.
 */
class StorageMigrationTo7(
    private val db: SQLiteDatabase,
    private val migrationsHelper: StorageMigrationHelper,
) {
    fun rewriteEnumOrdinalsToNames() {
        rewriteTheme()
        rewriteMessageViewTheme()
        rewriteMessageComposeTheme()
    }

    private fun rewriteTheme() {
        val theme = migrationsHelper.readValue(db, "theme")?.toInt()
        val newTheme = if (theme == THEME_ORDINAL_DARK) {
            THEME_DARK
        } else {
            THEME_LIGHT
        }

        migrationsHelper.writeValue(db, "theme", newTheme)
    }

    private fun rewriteMessageViewTheme() {
        rewriteScreenTheme("messageViewTheme")
    }

    private fun rewriteMessageComposeTheme() {
        rewriteScreenTheme("messageComposeTheme")
    }

    private fun rewriteScreenTheme(key: String) {
        val newTheme = when (migrationsHelper.readValue(db, key)?.toInt()) {
            THEME_ORDINAL_DARK -> THEME_DARK
            THEME_ORDINAL_USE_GLOBAL -> THEME_USE_GLOBAL
            else -> THEME_LIGHT
        }

        migrationsHelper.writeValue(db, key, newTheme)
    }

    companion object {
        private const val THEME_ORDINAL_DARK = 1
        private const val THEME_ORDINAL_USE_GLOBAL = 2
        private const val THEME_LIGHT = "LIGHT"
        private const val THEME_DARK = "DARK"
        private const val THEME_USE_GLOBAL = "USE_GLOBAL"
    }
}
