package com.fsck.k9.preferences

import android.content.Context
import com.fsck.k9.Account
import com.fsck.k9.AccountPreferenceSerializer.Companion.ACCOUNT_DESCRIPTION_KEY
import com.fsck.k9.AccountPreferenceSerializer.Companion.INCOMING_SERVER_SETTINGS_KEY
import com.fsck.k9.AccountPreferenceSerializer.Companion.OUTGOING_SERVER_SETTINGS_KEY
import com.fsck.k9.Core
import com.fsck.k9.Preferences
import com.fsck.k9.ServerSettingsSerializer
import com.fsck.k9.mail.AuthType
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mailstore.SpecialLocalFoldersCreator
import java.util.UUID
import kotlinx.datetime.Clock

internal class AccountSettingsWriter(
    private val preferences: Preferences,
    private val localFoldersCreator: SpecialLocalFoldersCreator,
    private val clock: Clock,
    private val serverSettingsSerializer: ServerSettingsSerializer,
    private val context: Context,
) {
    private val identitySettingsWriter = IdentitySettingsWriter()
    private val folderSettingsWriter = FolderSettingsWriter()

    fun write(account: ValidatedSettings.Account): Pair<AccountDescription, AccountDescription> {
        val editor = preferences.createStorageEditor()

        val originalAccountName = account.name!!
        val originalAccountUuid = account.uuid
        val originalAccount = AccountDescription(originalAccountName, originalAccountUuid)

        val accountUuid = getUniqueAccountUuid(originalAccountUuid)
        val accountName = getUniqueAccountName(originalAccountName)
        val writtenAccount = AccountDescription(accountName, accountUuid)

        editor.putStringWithLogging("$accountUuid.$ACCOUNT_DESCRIPTION_KEY", accountName)

        // Convert account settings to the string representation used in preference storage
        val stringSettings = AccountSettingsDescriptions.convert(account.settings)

        for ((accountKey, value) in stringSettings) {
            editor.putStringWithLogging("$accountUuid.$accountKey", value)
        }

        val newAccountNumber = preferences.generateAccountNumber().toString()
        editor.putStringWithLogging("$accountUuid.accountNumber", newAccountNumber)

        // When deleting an account and then restoring it using settings import, the same account UUID will be used.
        // To avoid reusing a previously existing notification channel ID, we need to make sure to use a unique value
        // for `messagesNotificationChannelVersion`.
        val messageNotificationChannelVersion = clock.now().epochSeconds.toString()
        editor.putStringWithLogging(
            key = "$accountUuid.messagesNotificationChannelVersion",
            value = messageNotificationChannelVersion,
        )

        writeServerSettings(editor, key = "$accountUuid.$INCOMING_SERVER_SETTINGS_KEY", server = account.incoming)
        writeServerSettings(editor, key = "$accountUuid.$OUTGOING_SERVER_SETTINGS_KEY", server = account.outgoing)

        writeIdentities(editor, accountUuid, account.identities)
        writeFolders(editor, accountUuid, account.folders)

        updateAccountUuids(editor, accountUuid)

        if (!editor.commit()) {
            error("Failed to commit account settings")
        }

        // Reload accounts so the new account can be picked up by Preferences.getAccount()
        preferences.loadAccounts()

        val appAccount = preferences.getAccount(accountUuid) ?: error("Failed to load account: $accountUuid")
        localFoldersCreator.createSpecialLocalFolders(appAccount)

        Core.setServicesEnabled(context)

        return originalAccount to writtenAccount
    }

    private fun updateAccountUuids(editor: StorageEditor, accountUuid: String) {
        val oldAccountUuids = preferences.storage.getString("accountUuids", "")
            .split(',')
            .dropLastWhile { it.isEmpty() }
        val newAccountUuids = oldAccountUuids + accountUuid

        val newAccountUuidString = newAccountUuids.joinToString(separator = ",")
        editor.putStringWithLogging("accountUuids", newAccountUuidString)
    }

    private fun writeIdentities(
        editor: StorageEditor,
        accountUuid: String,
        identities: List<ValidatedSettings.Identity>,
    ) {
        for ((index, identity) in identities.withIndex()) {
            identitySettingsWriter.write(editor, accountUuid, index, identity)
        }
    }

    private fun writeFolders(editor: StorageEditor, accountUuid: String, folders: List<ValidatedSettings.Folder>) {
        for (folder in folders) {
            folderSettingsWriter.write(editor, accountUuid, folder)
        }
    }

    private fun getUniqueAccountUuid(accountUuid: String): String {
        val existingAccount = preferences.getAccount(accountUuid)
        return if (existingAccount != null) {
            // An account with this UUID already exists. So generate a new UUID.
            UUID.randomUUID().toString()
        } else {
            accountUuid
        }
    }

    private fun getUniqueAccountName(accountName: String): String {
        val accounts = preferences.getAccounts()
        if (!isAccountNameUsed(accountName, accounts)) {
            return accountName
        }

        // Account name is already in use. So generate a new one by appending " (x)", where x is the first
        // number >= 1 that results in an unused account name.
        for (i in 1..accounts.size) {
            val newAccountName = "$accountName ($i)"
            if (!isAccountNameUsed(newAccountName, accounts)) {
                return newAccountName
            }
        }

        error("Unexpected exit")
    }

    private fun isAccountNameUsed(name: String?, accounts: List<Account>): Boolean {
        return accounts.any { it.displayName == name }
    }

    private fun writeServerSettings(
        editor: StorageEditor,
        key: String,
        server: ValidatedSettings.Server,
    ) {
        val serverSettings = createServerSettings(server)
        val serverSettingsJson = serverSettingsSerializer.serialize(serverSettings)
        editor.putStringWithLogging(key, serverSettingsJson)
    }

    private fun createServerSettings(server: ValidatedSettings.Server): ServerSettings {
        val connectionSecurity = convertConnectionSecurity(server.connectionSecurity)
        val authenticationType = AuthType.valueOf(server.authenticationType)
        val password = if (authenticationType == AuthType.XOAUTH2) "" else server.password

        return ServerSettings(
            server.type,
            server.host,
            server.port,
            connectionSecurity,
            authenticationType,
            server.username,
            password,
            server.clientCertificateAlias,
            server.extras,
        )
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private fun convertConnectionSecurity(connectionSecurity: String): ConnectionSecurity {
        return try {
            // TODO: Add proper settings validation and upgrade capability for server settings. Once that exists, move
            //  this code into a SettingsUpgrader.
            when (connectionSecurity) {
                "SSL_TLS_OPTIONAL" -> ConnectionSecurity.SSL_TLS_REQUIRED
                "STARTTLS_OPTIONAL" -> ConnectionSecurity.STARTTLS_REQUIRED
                else -> ConnectionSecurity.valueOf(connectionSecurity)
            }
        } catch (e: Exception) {
            ConnectionSecurity.SSL_TLS_REQUIRED
        }
    }
}
