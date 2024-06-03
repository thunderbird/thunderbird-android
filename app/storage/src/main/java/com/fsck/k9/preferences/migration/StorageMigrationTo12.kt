package com.fsck.k9.preferences.migration

import android.database.sqlite.SQLiteDatabase
import com.fsck.k9.ServerSettingsSerializer
import com.fsck.k9.mail.filter.Base64
import com.fsck.k9.preferences.migration.migration12.ImapStoreUriDecoder
import com.fsck.k9.preferences.migration.migration12.Pop3StoreUriDecoder
import com.fsck.k9.preferences.migration.migration12.SmtpTransportUriDecoder
import com.fsck.k9.preferences.migration.migration12.WebDavStoreUriDecoder

/**
 * Convert server settings from the old URI format to the new JSON format
 */
class StorageMigrationTo12(
    private val db: SQLiteDatabase,
    private val migrationsHelper: StorageMigrationsHelper,
) {
    private val serverSettingsSerializer = ServerSettingsSerializer()

    fun removeStoreAndTransportUri() {
        val accountUuidsListValue = migrationsHelper.readValue(db, "accountUuids")
        if (accountUuidsListValue == null || accountUuidsListValue.isEmpty()) {
            return
        }

        val accountUuids = accountUuidsListValue.split(",")
        for (accountUuid in accountUuids) {
            convertStoreUri(accountUuid)
            convertTransportUri(accountUuid)
        }
    }

    private fun convertStoreUri(accountUuid: String) {
        val storeUri = migrationsHelper.readValue(db, "$accountUuid.storeUri")?.base64Decode() ?: return

        val serverSettings = when {
            storeUri.startsWith("imap") -> ImapStoreUriDecoder.decode(storeUri)
            storeUri.startsWith("pop3") -> Pop3StoreUriDecoder.decode(storeUri)
            storeUri.startsWith("webdav") -> WebDavStoreUriDecoder.decode(storeUri)
            else -> error("Unsupported account type")
        }

        val json = serverSettingsSerializer.serialize(serverSettings)

        migrationsHelper.insertValue(db, "$accountUuid.incomingServerSettings", json)
        migrationsHelper.writeValue(db, "$accountUuid.storeUri", null)
    }

    private fun convertTransportUri(accountUuid: String) {
        val transportUri = migrationsHelper.readValue(db, "$accountUuid.transportUri")?.base64Decode() ?: return

        val serverSettings = when {
            transportUri.startsWith("smtp") -> SmtpTransportUriDecoder.decodeSmtpUri(transportUri)
            transportUri.startsWith("webdav") -> WebDavStoreUriDecoder.decode(transportUri)
            else -> error("Unsupported account type")
        }

        val json = serverSettingsSerializer.serialize(serverSettings)

        migrationsHelper.insertValue(db, "$accountUuid.outgoingServerSettings", json)
        migrationsHelper.writeValue(db, "$accountUuid.transportUri", null)
    }

    private fun String.base64Decode() = Base64.decode(this)
}
