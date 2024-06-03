package com.fsck.k9.preferences.migration

import android.database.sqlite.SQLiteDatabase

/**
 * Remove saved folder settings
 *
 * Folder settings are now only written to 'Storage' when settings are imported. The saved settings will be used when
 * folders are first created and then the saved settings are discarded.
 * Since this wasn't the procedure in earlier app versions, we remove any saved folder settings in this migration.
 */
class StorageMigrationTo10(
    private val db: SQLiteDatabase,
    private val migrationsHelper: StorageMigrationsHelper,
) {
    private val folderKeyPattern = Regex("[^.]+\\..+\\.([^.]+)")
    private val folderSettingKeys = setOf(
        "displayMode",
        "notifyMode",
        "syncMode",
        "pushMode",
        "inTopGroup",
        "integrate",
    )

    fun removeSavedFolderSettings() {
        val loadedValues = migrationsHelper.readAllValues(db)

        for (key in loadedValues.keys) {
            val matches = folderKeyPattern.matchEntire(key) ?: continue
            val folderKey = matches.groupValues[1]
            if (folderKey in folderSettingKeys) {
                migrationsHelper.writeValue(db, key, null)
            }
        }
    }
}
