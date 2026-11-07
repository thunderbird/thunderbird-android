package com.fsck.k9.preferences.migration

import android.database.sqlite.SQLiteDatabase

/**
 * Store the "automatic" message format in `messageFormat` and remove the legacy `messageFormatAuto` flag.
 *
 * Previous versions stored "automatic" as `messageFormat=TEXT` plus `messageFormatAuto=true`.
 *
 * - `messageFormat=TEXT` and `messageFormatAuto=true` -> `messageFormat=AUTO`
 * - `messageFormatAuto` is removed for every account
 */
class StorageMigrationTo31(
    private val db: SQLiteDatabase,
    private val migrationsHelper: StorageMigrationHelper,
) {
    fun migrateMessageFormatAuto() {
        val accountUuidsValue = migrationsHelper.readValue(db, "accountUuids")
        if (accountUuidsValue.isNullOrEmpty()) return

        val accountUuids = accountUuidsValue.split(",").filter { it.isNotEmpty() }
        for (accountUuid in accountUuids) {
            migrateAccount(accountUuid)
        }
    }

    private fun migrateAccount(accountUuid: String) {
        val messageFormatKey = "$accountUuid.messageFormat"
        val messageFormatAutoKey = "$accountUuid.messageFormatAuto"

        val messageFormat = migrationsHelper.readValue(db, messageFormatKey)
        val isMessageFormatAuto = migrationsHelper.readValue(db, messageFormatAutoKey).toBoolean()

        if (isMessageFormatAuto && messageFormat == "TEXT") {
            migrationsHelper.writeValue(db, messageFormatKey, "AUTO")
        }

        migrationsHelper.writeValue(db, messageFormatAutoKey, null)
    }
}
