package com.fsck.k9.preferences.migration

import android.database.sqlite.SQLiteDatabase
import com.fsck.k9.preferences.upgrader.GeneralSettingsUpgraderTo31

/**
 * Convert old value for message view content font size to new format.
 *
 * This change in formats has been made a long time ago. But never in a migration. So it's possible there are still
 * installations out there that have the old version in the database. And they would work just fine, because this
 * conversion was done when loading font size values.
 */
class StorageMigrationTo11(
    private val db: SQLiteDatabase,
    private val migrationsHelper: StorageMigrationsHelper,
) {
    fun upgradeMessageViewContentFontSize() {
        val newFontSizeValue = migrationsHelper.readValue(db, "fontSizeMessageViewContentPercent")
        if (newFontSizeValue != null) return

        val oldFontSizeValue = migrationsHelper.readValue(db, "fontSizeMessageViewContent")?.toIntOrNull() ?: 3
        val fontSizeValue = GeneralSettingsUpgraderTo31.convertFromOldSize(oldFontSizeValue)
        migrationsHelper.writeValue(db, "fontSizeMessageViewContentPercent", fontSizeValue.toString())
        migrationsHelper.writeValue(db, "fontSizeMessageViewContent", null)
    }
}
