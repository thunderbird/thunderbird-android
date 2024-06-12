package com.fsck.k9.preferences

import com.fsck.k9.Account
import com.fsck.k9.AccountPreferenceSerializer.Companion.ACCOUNT_DESCRIPTION_KEY
import com.fsck.k9.Preferences
import java.util.UUID
import kotlinx.datetime.Clock

internal class AccountSettingsWriter(
    private val preferences: Preferences,
    private val clock: Clock,
) {
    fun write(editor: StorageEditor, account: ValidatedSettings.Account): Pair<AccountDescription, AccountDescription> {
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

        return originalAccount to writtenAccount
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
}