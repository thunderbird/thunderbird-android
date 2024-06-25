package com.fsck.k9.preferences.migration

import android.database.sqlite.SQLiteDatabase
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

/**
 * Clean up the authentication type in outgoing server settings.
 *
 * Replaces the authentication value "AUTOMATIC" with "PLAIN" when TLS is used, "CRAM_MD5" otherwise.
 * Replaces the authentication value "LOGIN" with "PLAIN".
 */
class StorageMigrationTo24(
    private val db: SQLiteDatabase,
    private val migrationsHelper: StorageMigrationHelper,
) {
    fun removeLegacyAuthenticationModes() {
        val accountUuidsListValue = migrationsHelper.readValue(db, "accountUuids")
        if (accountUuidsListValue.isNullOrEmpty()) {
            return
        }

        val accountUuids = accountUuidsListValue.split(",")
        for (accountUuid in accountUuids) {
            removeLegacyAuthenticationModesForAccount(accountUuid)
        }
    }

    private fun removeLegacyAuthenticationModesForAccount(accountUuid: String) {
        val outgoingServerSettingsJson = migrationsHelper.readValue(db, "$accountUuid.outgoingServerSettings") ?: return

        val adapter = createJsonAdapter()

        adapter.fromJson(outgoingServerSettingsJson)?.let { settings ->
            createUpdatedServerSettings(settings)?.let { newSettings ->
                val json = adapter.toJson(newSettings)
                migrationsHelper.writeValue(db, "$accountUuid.outgoingServerSettings", json)
            }
        }
    }

    private fun createUpdatedServerSettings(serverSettings: Map<String, Any?>): Map<String, Any?>? {
        val isSecure = serverSettings["connectionSecurity"] == "STARTTLS_REQUIRED" ||
            serverSettings["connectionSecurity"] == "SSL_TLS_REQUIRED"

        return when (serverSettings["authenticationType"]) {
            "AUTOMATIC" -> {
                serverSettings.toMutableMap().apply {
                    fixPortType()
                    this["authenticationType"] = if (isSecure) "PLAIN" else "CRAM_MD5"
                }
            }

            "LOGIN" -> {
                serverSettings.toMutableMap().apply {
                    fixPortType()
                    this["authenticationType"] = "PLAIN"
                }
            }

            else -> {
                null
            }
        }
    }

    private fun MutableMap<String, Any?>.fixPortType() {
        // This is so we don't end up with a port value of e.g. "993.0". It would still work, but it looks odd.
        this["port"] = (this["port"] as? Double)?.toInt()
    }

    private fun createJsonAdapter(): JsonAdapter<Map<String, Any?>> {
        val moshi = Moshi.Builder().build()

        return moshi.adapter<Map<String, Any?>>(
            Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java),
        ).serializeNulls()
    }
}
