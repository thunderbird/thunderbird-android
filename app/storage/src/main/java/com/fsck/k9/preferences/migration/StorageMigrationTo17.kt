package com.fsck.k9.preferences.migration

import android.database.sqlite.SQLiteDatabase

/**
 * Rewrite 'led' and 'ledColor' values to 'notificationLight'.
 */
class StorageMigrationTo17(
    private val db: SQLiteDatabase,
    private val migrationsHelper: StorageMigrationHelper,
) {
    fun rewriteNotificationLightSettings() {
        val accountUuidsListValue = migrationsHelper.readValue(db, "accountUuids")
        if (accountUuidsListValue == null || accountUuidsListValue.isEmpty()) {
            return
        }

        val accountUuids = accountUuidsListValue.split(",")
        for (accountUuid in accountUuids) {
            rewriteNotificationLightSettings(accountUuid)
        }
    }

    private fun rewriteNotificationLightSettings(accountUuid: String) {
        val isLedEnabled = migrationsHelper.readValue(db, "$accountUuid.led").toBoolean()
        val ledColor = migrationsHelper.readValue(db, "$accountUuid.ledColor")?.toInt() ?: 0
        val accountColor = migrationsHelper.readValue(db, "$accountUuid.chipColor")?.toInt() ?: 0

        val notificationLight = convertToNotificationLightValue(isLedEnabled, ledColor, accountColor)

        migrationsHelper.writeValue(db, "$accountUuid.notificationLight", notificationLight)
        migrationsHelper.writeValue(db, "$accountUuid.led", null)
        migrationsHelper.writeValue(db, "$accountUuid.ledColor", null)
    }

    private fun convertToNotificationLightValue(isLedEnabled: Boolean, ledColor: Int, accountColor: Int): String {
        if (!isLedEnabled) return "Disabled"

        return when (ledColor.rgb) {
            accountColor.rgb -> "AccountColor"
            0xFFFFFF -> "White"
            0xFF0000 -> "Red"
            0x00FF00 -> "Green"
            0x0000FF -> "Blue"
            0xFFFF00 -> "Yellow"
            0x00FFFF -> "Cyan"
            0xFF00FF -> "Magenta"
            else -> "SystemDefaultColor"
        }
    }

    private val Int.rgb
        get() = this and 0x00FFFFFF
}
