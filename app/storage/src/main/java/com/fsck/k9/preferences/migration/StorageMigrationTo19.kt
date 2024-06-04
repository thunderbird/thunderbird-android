package com.fsck.k9.preferences.migration

import android.database.sqlite.SQLiteDatabase
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

/**
 * Mark Gmail accounts.
 *
 * Gmail has stopped allowing clients to use the Google account password to authenticate via IMAP/POP3/SMTP. We want to
 * automatically update the server settings to use OAuth 2.0 for users who can no longer access Gmail because of the
 * change. However, we don't want to touch accounts that are using an app-specific password and still work fine. Since
 * we can't distinguish an app-specific password from an account password, we only switch accounts to using OAuth after
 * an authentication failure. We usually want to avoid automatically changing the user's settings like this. So we only
 * do it for existing accounts, and only after the first authentication failure.
 */
class StorageMigrationTo19(
    private val db: SQLiteDatabase,
    private val migrationsHelper: StorageMigrationHelper,
) {
    fun markGmailAccounts() {
        val accountUuidsListValue = migrationsHelper.readValue(db, "accountUuids")
        if (accountUuidsListValue == null || accountUuidsListValue.isEmpty()) {
            return
        }

        val accountUuids = accountUuidsListValue.split(",")
        for (accountUuid in accountUuids) {
            markIfGmailAccount(accountUuid)
        }
    }

    private fun markIfGmailAccount(accountUuid: String) {
        val incomingServerSettingsJson = migrationsHelper.readValue(db, "$accountUuid.incomingServerSettings") ?: return
        val outgoingServerSettingsJson = migrationsHelper.readValue(db, "$accountUuid.outgoingServerSettings") ?: return

        val moshi = Moshi.Builder().build()
        val adapter = moshi.adapter<Map<String, Any?>>(
            Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java),
        )

        val incomingServerSettings = adapter.fromJson(incomingServerSettingsJson) ?: return
        val outgoingServerSettings = adapter.fromJson(outgoingServerSettingsJson) ?: return

        if (incomingServerSettings["type"] == "imap" &&
            incomingServerSettings["host"] in setOf("imap.gmail.com", "imap.googlemail.com") &&
            incomingServerSettings["authenticationType"] != "XOAUTH2" ||
            outgoingServerSettings["host"] in setOf("smtp.gmail.com", "smtp.googlemail.com") &&
            outgoingServerSettings["authenticationType"] != "XOAUTH2"
        ) {
            migrationsHelper.insertValue(db, "$accountUuid.migrateToOAuth", "true")
        }
    }
}
