package com.fsck.k9.preferences.migrations

import android.database.sqlite.SQLiteDatabase
import android.graphics.Color

/**
 * Change default value of {@code registeredNameColor} from {@code 0xFF00008F} to {@code -638932)} (#F6402C).
 */
class StorageMigrationTo16(
    private val db: SQLiteDatabase,
    private val migrationsHelper: StorageMigrationsHelper
) {
    fun rewriteIdleRefreshInterval() {
        val registeredNameColorValue = migrationsHelper.readValue(db, "registeredNameColor")?.toInt()
        if (registeredNameColorValue != null && registeredNameColorValue == -0xffff71) {
            migrationsHelper.writeValue(db, "registeredNameColor", "-638932" ) // #F6402C
        }
    }
}
