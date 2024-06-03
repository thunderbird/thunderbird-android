package com.fsck.k9.preferences.migration

import android.database.sqlite.SQLiteDatabase

private const val DEFAULT_IDLE_REFRESH_MINUTES = 24
private const val MINIMUM_IDLE_REFRESH_MINUTES = 2

/**
 * Rewrite 'idleRefreshMinutes' to make sure the minimum value is 2 minutes.
 */
class StorageMigrationTo15(
    private val db: SQLiteDatabase,
    private val migrationsHelper: StorageMigrationsHelper,
) {
    fun rewriteIdleRefreshInterval() {
        val accountUuidsListValue = migrationsHelper.readValue(db, "accountUuids")
        if (accountUuidsListValue == null || accountUuidsListValue.isEmpty()) {
            return
        }

        val accountUuids = accountUuidsListValue.split(",")
        for (accountUuid in accountUuids) {
            rewriteIdleRefreshInterval(accountUuid)
        }
    }

    private fun rewriteIdleRefreshInterval(accountUuid: String) {
        val idleRefreshMinutes = migrationsHelper.readValue(db, "$accountUuid.idleRefreshMinutes")?.toIntOrNull()
            ?: DEFAULT_IDLE_REFRESH_MINUTES

        val newIdleRefreshMinutes = idleRefreshMinutes.coerceAtLeast(MINIMUM_IDLE_REFRESH_MINUTES)
        if (newIdleRefreshMinutes != idleRefreshMinutes) {
            migrationsHelper.writeValue(db, "$accountUuid.idleRefreshMinutes", newIdleRefreshMinutes.toString())
        }
    }
}
