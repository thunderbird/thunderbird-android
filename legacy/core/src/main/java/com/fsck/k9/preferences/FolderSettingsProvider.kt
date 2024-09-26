package com.fsck.k9.preferences

import app.k9mail.legacy.account.Account
import app.k9mail.legacy.mailstore.FolderRepository
import app.k9mail.legacy.mailstore.RemoteFolderDetails
import com.fsck.k9.mail.FolderClass

class FolderSettingsProvider(private val folderRepository: FolderRepository) {
    fun getFolderSettings(account: Account): List<FolderSettings> {
        return folderRepository.getRemoteFolderDetails(account)
            .filterNot { it.containsOnlyDefaultValues() }
            .map { it.toFolderSettings() }
    }

    private fun RemoteFolderDetails.containsOnlyDefaultValues(): Boolean {
        return isInTopGroup == getDefaultValue("inTopGroup") &&
            isIntegrate == getDefaultValue("integrate") &&
            isSyncEnabled == getDefaultValue("syncEnabled") &&
            displayClass == getDefaultValue("displayMode") &&
            isNotificationsEnabled == getDefaultValue("notificationsEnabled") &&
            isPushEnabled == getDefaultValue("pushEnabled")
    }

    private fun getDefaultValue(key: String): Any? {
        val versionedSetting = FolderSettingsDescriptions.SETTINGS[key] ?: error("Key not found: $key")
        val highestVersion = versionedSetting.lastKey()
        val setting = versionedSetting[highestVersion] ?: error("Setting description not found: $key")
        return setting.defaultValue
    }

    private fun RemoteFolderDetails.toFolderSettings(): FolderSettings {
        return FolderSettings(
            folder.serverId,
            isInTopGroup,
            isIntegrate,
            isSyncEnabled,
            displayClass,
            isNotificationsEnabled,
            isPushEnabled,
        )
    }
}

data class FolderSettings(
    val serverId: String,
    val isInTopGroup: Boolean,
    val isIntegrate: Boolean,
    val isSyncEnabled: Boolean,
    val displayClass: FolderClass,
    val isNotificationsEnabled: Boolean,
    val isPushEnabled: Boolean,
)
