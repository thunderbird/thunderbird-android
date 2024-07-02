package com.fsck.k9.preferences.migration

import android.database.sqlite.SQLiteDatabase

/**
 * Combine `messageViewReturnToList` and `messageViewShowNext` into `messageViewPostDeleteAction`.
 */
class StorageMigrationTo21(
    private val db: SQLiteDatabase,
    private val migrationsHelper: StorageMigrationHelper,
) {
    fun createPostRemoveNavigationSetting() {
        val messageViewReturnToList = migrationsHelper.readValue(db, "messageViewReturnToList").toBoolean()
        val messageViewShowNext = migrationsHelper.readValue(db, "messageViewShowNext").toBoolean()

        val postRemoveNavigation = when {
            messageViewReturnToList -> "ReturnToMessageList"
            messageViewShowNext -> "ShowNextMessage"
            else -> "ShowPreviousMessage"
        }

        migrationsHelper.writeValue(db, "messageViewPostDeleteAction", postRemoveNavigation)
        migrationsHelper.writeValue(db, "messageViewReturnToList", null)
        migrationsHelper.writeValue(db, "messageViewShowNext", null)
    }
}
