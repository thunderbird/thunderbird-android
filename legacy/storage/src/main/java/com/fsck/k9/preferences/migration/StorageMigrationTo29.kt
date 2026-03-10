package com.fsck.k9.preferences.migration

import android.database.sqlite.SQLiteDatabase

private const val OLD_KEY = "account_setup_auto_expand_folder"
private const val NEW_KEY = "auto_select_folder"

class StorageMigrationTo29(
    private val db: SQLiteDatabase,
    private val migrationsHelper: StorageMigrationHelper,
) {
    fun renameAutoSelectFolderPreference() {
        val oldValue = migrationsHelper.readValue(db, OLD_KEY)
        if (oldValue != null) {
            migrationsHelper.insertValue(db, NEW_KEY, oldValue)
            migrationsHelper.writeValue(db, OLD_KEY, null)
        }
    }
}
