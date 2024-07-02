package com.fsck.k9.preferences.migration

import android.database.sqlite.SQLiteDatabase
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

/**
 * Updates outgoing server settings to use an authentication type value of "NONE" when appropriate.
 */
class StorageMigrationTo25(
    private val db: SQLiteDatabase,
    private val migrationsHelper: StorageMigrationHelper,
) {
    fun convertToAuthTypeNone() {
        val accountUuidsListValue = migrationsHelper.readValue(db, "accountUuids")
        if (accountUuidsListValue.isNullOrEmpty()) {
            return
        }

        val accountUuids = accountUuidsListValue.split(",")
        for (accountUuid in accountUuids) {
            convertToAuthTypeNoneForAccount(accountUuid)
        }
    }

    private fun convertToAuthTypeNoneForAccount(accountUuid: String) {
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
        val username = serverSettings["username"] as? String?

        return if (username.isNullOrEmpty()) {
            serverSettings.toMutableMap().apply {
                fixPortType()
                this["authenticationType"] = "NONE"
                this["username"] = ""
                this["password"] = null
            }
        } else {
            null
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
