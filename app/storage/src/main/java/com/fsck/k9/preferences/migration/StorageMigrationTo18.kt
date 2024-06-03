package com.fsck.k9.preferences.migration

import android.database.sqlite.SQLiteDatabase

/**
 * Rewrite the per-network type IMAP compression settings to a single setting.
 */
class StorageMigrationTo18(
    private val db: SQLiteDatabase,
    private val migrationsHelper: StorageMigrationsHelper,
) {
    fun rewriteImapCompressionSettings() {
        val accountUuidsListValue = migrationsHelper.readValue(db, "accountUuids")
        if (accountUuidsListValue == null || accountUuidsListValue.isEmpty()) {
            return
        }

        val accountUuids = accountUuidsListValue.split(",")
        for (accountUuid in accountUuids) {
            rewriteImapCompressionSetting(accountUuid)
        }
    }

    private fun rewriteImapCompressionSetting(accountUuid: String) {
        val useCompressionWifi = migrationsHelper.readValue(db, "$accountUuid.useCompression.WIFI").toBoolean()
        val useCompressionMobile = migrationsHelper.readValue(db, "$accountUuid.useCompression.MOBILE").toBoolean()
        val useCompressionOther = migrationsHelper.readValue(db, "$accountUuid.useCompression.OTHER").toBoolean()

        val useCompression = useCompressionWifi && useCompressionMobile && useCompressionOther
        migrationsHelper.writeValue(db, "$accountUuid.useCompression", useCompression.toString())

        migrationsHelper.writeValue(db, "$accountUuid.useCompression.WIFI", null)
        migrationsHelper.writeValue(db, "$accountUuid.useCompression.MOBILE", null)
        migrationsHelper.writeValue(db, "$accountUuid.useCompression.OTHER", null)
    }
}
