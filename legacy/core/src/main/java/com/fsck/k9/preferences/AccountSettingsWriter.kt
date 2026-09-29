package com.fsck.k9.preferences

import android.R.attr.mimeType
import android.content.ClipData.newUri
import android.content.Context
import android.net.Uri
import android.util.Base64
import com.eygraber.uri.toKmpUri
import com.fsck.k9.Core
import com.fsck.k9.Preferences
import com.fsck.k9.mailstore.SpecialLocalFoldersCreator
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.preference.GeneralSettingsManager
import net.thunderbird.core.preference.storage.StorageEditor
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.account.avatar.AvatarImageRepository
import net.thunderbird.feature.account.storage.legacy.LegacyAccountStorageHandler.Companion.ACCOUNT_DESCRIPTION_KEY
import net.thunderbird.feature.account.storage.legacy.LegacyAccountStorageHandler.Companion.INCOMING_SERVER_SETTINGS_KEY
import net.thunderbird.feature.account.storage.legacy.LegacyAccountStorageHandler.Companion.OUTGOING_SERVER_SETTINGS_KEY
import net.thunderbird.feature.account.storage.legacy.serializer.ServerSettingsDtoSerializer
import net.thunderbird.legacy.logging.Log.logger

internal class AccountSettingsWriter
@OptIn(ExperimentalTime::class)
constructor(
    private val preferences: Preferences,
    private val localFoldersCreator: SpecialLocalFoldersCreator,
    private val clock: Clock,
    private val generalSettingsManager: GeneralSettingsManager,
    serverSettingsDtoSerializer: ServerSettingsDtoSerializer,
    private val avatarImageRepository: AvatarImageRepository,
    private val context: Context,
) {
    private val identitySettingsWriter = IdentitySettingsWriter(generalSettingsManager)
    private val folderSettingsWriter = FolderSettingsWriter(generalSettingsManager)
    private val serverSettingsWriter = ServerSettingsWriter(serverSettingsDtoSerializer, generalSettingsManager)

    @Suppress("LongMethod")
    suspend fun write(account: ValidatedSettings.Account): Pair<AccountDescription, AccountDescription> {
        val editor = preferences.createStorageEditor()

        val originalAccountName = account.name!!
        val originalAccountId = AccountIdFactory.of(account.uuid)
        val originalAccount = AccountDescription(originalAccountName, originalAccountId.toString())

        val accountId = getUniqueAccountId(originalAccountId)
        val accountName = getUniqueAccountName(originalAccountName)
        val writtenAccount = AccountDescription(accountName, accountId.toString())

        editor.putStringWithLogging(
            "$accountId.$ACCOUNT_DESCRIPTION_KEY",
            accountName,
            generalSettingsManager.getConfig().debugging.isDebugLoggingEnabled,
            generalSettingsManager.getConfig().debugging.isSensitiveLoggingEnabled,
        )

        // Convert account settings to the string representation used in preference storage
        val stringSettings = AccountSettingsDescriptions.convert(account.settings)
        account.decodeAvatarImage()?.let { stringSettings["avatarImageUri"] = it }

        for ((accountKey, value) in stringSettings) {
            editor.putStringWithLogging(
                "$accountId.$accountKey",
                value,
                generalSettingsManager.getConfig().debugging.isDebugLoggingEnabled,
                generalSettingsManager.getConfig().debugging.isSensitiveLoggingEnabled,
            )
        }

        val newAccountNumber = preferences.generateAccountNumber().toString()
        editor.putStringWithLogging(
            "$accountId.accountNumber",
            newAccountNumber,
            generalSettingsManager.getConfig().debugging.isDebugLoggingEnabled,
            generalSettingsManager.getConfig().debugging.isSensitiveLoggingEnabled,
        )

        // When deleting an account and then restoring it using settings import, the same account UUID will be used.
        // To avoid reusing a previously existing notification channel ID, we need to make sure to use a unique value
        // for `messagesNotificationChannelVersion`.
        @OptIn(ExperimentalTime::class)
        val messageNotificationChannelVersion = clock.now().epochSeconds.toString()
        editor.putStringWithLogging(
            key = "$accountId.messagesNotificationChannelVersion",
            value = messageNotificationChannelVersion,
            isDebugLoggingEnabled = generalSettingsManager.getConfig().debugging.isDebugLoggingEnabled,
            isSensitiveDebugLoggingEnabled = generalSettingsManager.getConfig().debugging.isSensitiveLoggingEnabled,
        )

        serverSettingsWriter.writeServerSettings(
            editor,
            key = "$accountId.$INCOMING_SERVER_SETTINGS_KEY",
            server = account.incoming,
        )
        serverSettingsWriter.writeServerSettings(
            editor,
            key = "$accountId.$OUTGOING_SERVER_SETTINGS_KEY",
            server = account.outgoing,
        )

        writeIdentities(editor, accountId, account.identities)
        writeFolders(editor, accountId, account.folders)

        updateAccountUuids(editor, accountId.toString())

        if (!editor.commit()) {
            error("Failed to commit account settings")
        }

        // Reload accounts so the new account can be picked up by Preferences.getAccount()
        preferences.loadAccounts()

        val appAccount = preferences.getById(accountId) ?: error("Failed to load account: $accountId")
        localFoldersCreator.createSpecialLocalFolders(appAccount)

        Core.setServicesEnabled(context)

        return originalAccount to writtenAccount
    }

    private fun updateAccountUuids(editor: StorageEditor, accountUuid: String) {
        val oldAccountUuids = preferences.storage.getStringOrDefault("accountUuids", "")
            .split(',')
            .dropLastWhile { it.isEmpty() }
        val newAccountUuids = oldAccountUuids + accountUuid

        val newAccountUuidString = newAccountUuids.joinToString(separator = ",")
        editor.putStringWithLogging(
            "accountUuids",
            newAccountUuidString,
            generalSettingsManager.getConfig().debugging.isDebugLoggingEnabled,
            generalSettingsManager.getConfig().debugging.isSensitiveLoggingEnabled,
        )
    }

    private fun writeIdentities(
        editor: StorageEditor,
        accountId: AccountId,
        identities: List<ValidatedSettings.Identity>,
    ) {
        for ((index, identity) in identities.withIndex()) {
            identitySettingsWriter.write(editor, accountId.toString(), index, identity)
        }
    }

    private fun writeFolders(editor: StorageEditor, accountId: AccountId, folders: List<ValidatedSettings.Folder>) {
        for (folder in folders) {
            folderSettingsWriter.write(editor, accountId.toString(), folder)
        }
    }

    private fun getUniqueAccountId(accountId: AccountId): AccountId {
        val existingAccount = preferences.getById(accountId)
        return if (existingAccount != null) {
            AccountIdFactory.create()
        } else {
            accountId
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

    private fun isAccountNameUsed(name: String?, accounts: List<LegacyAccountDto>): Boolean {
        return accounts.any { it.displayName == name }
    }

    private suspend fun ValidatedSettings.Account.decodeAvatarImage(): String? {
        val base64 = avatarImage
        return base64?.let { base64 ->
            val bytes = Base64.decode(base64, Base64.NO_WRAP)
            val tempFile = File(context.cacheDir, "avatar_import_$uuid")
            try {
                tempFile.writeBytes(bytes)
                val tempUri = Uri.fromFile(tempFile)
                val newUri = avatarImageRepository.update(AccountIdFactory.of(uuid), tempUri.toKmpUri())
                newUri.toString()
            } catch (e: IOException) {
                logger.error(throwable = e) { "Failed to decode avatar image." }
                null
            } finally {
                tempFile.delete()
            }
        }
    }
}
