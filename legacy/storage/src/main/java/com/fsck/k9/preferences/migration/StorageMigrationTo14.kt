package com.fsck.k9.preferences.migration

import android.database.sqlite.SQLiteDatabase
import net.thunderbird.core.common.mail.Protocols
import net.thunderbird.feature.account.storage.legacy.serializer.ServerSettingsDtoSerializer

/**
 * Rewrite 'folderPushMode' value of non-IMAP accounts to 'NONE'.
 */
class StorageMigrationTo14(
    private val db: SQLiteDatabase,
    private val migrationsHelper: StorageMigrationHelper,
) {
    private val serverSettingsDtoSerializer = ServerSettingsDtoSerializer()

    fun disablePushFoldersForNonImapAccounts() {
        val accountUuidsListValue = migrationsHelper.readValue(db, "accountUuids")
        if (accountUuidsListValue == null || accountUuidsListValue.isEmpty()) {
            return
        }

        val accountUuids = accountUuidsListValue.split(",")
        for (accountUuid in accountUuids) {
            disablePushFolders(accountUuid)
        }
    }

    private fun disablePushFolders(accountUuid: String) {
        val json = migrationsHelper.readValue(db, "$accountUuid.incomingServerSettings") ?: return
        val serverSettings = serverSettingsDtoSerializer.deserialize(json)
        if (serverSettings.type != Protocols.IMAP) {
            migrationsHelper.writeValue(db, "$accountUuid.folderPushMode", "NONE")
        }
    }
}
