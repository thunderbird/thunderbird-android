package com.fsck.k9.mailstore

import app.k9mail.legacy.mailstore.FolderSettings
import com.fsck.k9.Preferences
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.feature.account.AccountId

/**
 * Provides imported folder settings if available, otherwise default values.
 */
class FolderSettingsProvider(
    val preferences: Preferences,
    val accountManager: LegacyAccountManager,
    val accountId: AccountId,
) {
    fun getFolderSettings(folderServerId: String): FolderSettings {
        val storage = preferences.storage
        val prefix = "$accountId.$folderServerId"
        val account = getAccountById(accountId)

        return FolderSettings(
            visibleLimit = account.displayCount,
            isVisible = storage.getBoolean("$prefix.visible", true),
            isSyncEnabled = storage.getBoolean("$prefix.syncEnabled", false),
            isNotificationsEnabled = storage.getBoolean("$prefix.notificationsEnabled", false),
            isPushEnabled = storage.getBoolean("$prefix.pushEnabled", false),
            inTopGroup = storage.getBoolean("$prefix.inTopGroup", false),
            integrate = storage.getBoolean("$prefix.integrate", false),
        ).also {
            removeImportedFolderSettings(prefix)
        }
    }

    private fun getAccountById(accountId: AccountId) = runBlocking {
        accountManager.getById(accountId).firstOrNull()
            ?: error("Account not found: $accountId")
    }

    private fun removeImportedFolderSettings(prefix: String) {
        val editor = preferences.createStorageEditor()

        editor.remove("$prefix.visible")
        editor.remove("$prefix.syncEnabled")
        editor.remove("$prefix.notificationsEnabled")
        editor.remove("$prefix.pushEnabled")
        editor.remove("$prefix.inTopGroup")
        editor.remove("$prefix.integrate")

        editor.commit()
    }
}
