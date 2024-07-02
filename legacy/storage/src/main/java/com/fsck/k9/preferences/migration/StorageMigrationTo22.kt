package com.fsck.k9.preferences.migration

import android.database.sqlite.SQLiteDatabase
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

/**
 * Fix server settings by removing line breaks from username and password.
 */
class StorageMigrationTo22(
    private val db: SQLiteDatabase,
    private val migrationsHelper: StorageMigrationHelper,
) {
    fun fixServerSettings() {
        val accountUuidsListValue = migrationsHelper.readValue(db, "accountUuids")
        if (accountUuidsListValue.isNullOrEmpty()) {
            return
        }

        val accountUuids = accountUuidsListValue.split(",")
        for (accountUuid in accountUuids) {
            fixServerSettingsForAccount(accountUuid)
        }
    }

    private fun fixServerSettingsForAccount(accountUuid: String) {
        val incomingServerSettingsJson = migrationsHelper.readValue(db, "$accountUuid.incomingServerSettings") ?: return
        val outgoingServerSettingsJson = migrationsHelper.readValue(db, "$accountUuid.outgoingServerSettings") ?: return

        val adapter = createJsonAdapter()

        adapter.fromJson(incomingServerSettingsJson)?.let { settings ->
            createFixedServerSettings(settings)?.let { newSettings ->
                val json = adapter.toJson(newSettings)
                migrationsHelper.writeValue(db, "$accountUuid.incomingServerSettings", json)
            }
        }

        adapter.fromJson(outgoingServerSettingsJson)?.let { settings ->
            createFixedServerSettings(settings)?.let { newSettings ->
                val json = adapter.toJson(newSettings)
                migrationsHelper.writeValue(db, "$accountUuid.outgoingServerSettings", json)
            }
        }
    }

    private fun createFixedServerSettings(serverSettings: Map<String, Any?>): Map<String, Any?>? {
        val username = serverSettings["username"] as? String
        val password = serverSettings["password"] as? String
        val newUsername = username?.stripLineBreaks()
        val newPassword = password?.stripLineBreaks()

        return if (username != newUsername || password != newPassword) {
            serverSettings.toMutableMap().apply {
                this["username"] = newUsername
                this["password"] = newPassword

                // This is so we don't end up with a port value of e.g. "993.0". It would still work, but it looks odd.
                this["port"] = (serverSettings["port"] as? Double)?.toInt()
            }
        } else {
            null
        }
    }

    private fun createJsonAdapter(): JsonAdapter<Map<String, Any?>> {
        val moshi = Moshi.Builder().build()

        return moshi.adapter<Map<String, Any?>>(
            Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java),
        ).serializeNulls()
    }
}

private val LINE_BREAK = "[\\r\\n]".toRegex()

private fun String.stripLineBreaks() = replace(LINE_BREAK, replacement = "")
