package com.fsck.k9.preferences.upgrader

import app.k9mail.legacy.account.Account.FolderMode
import com.fsck.k9.mail.FolderClass
import com.fsck.k9.preferences.CombinedSettingsUpgrader
import com.fsck.k9.preferences.InternalSettingsMap
import com.fsck.k9.preferences.ValidatedSettings

class CombinedSettingsUpgraderTo99 : CombinedSettingsUpgrader {
    override fun upgrade(account: ValidatedSettings.Account): ValidatedSettings.Account {
        val folderSyncMode = account.settings["folderSyncMode"] as? FolderMode ?: FolderMode.NONE

        val newFolders = account.folders.map { folder ->
            val newFolderSettings = folder.settings.toMutableMap().apply {
                this["syncEnabled"] = folder.isSyncEnabled(folderSyncMode)
            }

            folder.copy(settings = newFolderSettings)
        }

        return account.copy(folders = newFolders)
    }

    private fun ValidatedSettings.Folder.isSyncEnabled(
        folderSyncMode: FolderMode,
    ): Boolean {
        val syncClass = getEffectiveSyncClass(settings)

        return when (folderSyncMode) {
            FolderMode.NONE -> {
                false
            }

            FolderMode.ALL -> {
                true
            }

            FolderMode.FIRST_CLASS -> {
                syncClass == FolderClass.FIRST_CLASS
            }

            FolderMode.FIRST_AND_SECOND_CLASS -> {
                syncClass == FolderClass.FIRST_CLASS || syncClass == FolderClass.SECOND_CLASS
            }

            FolderMode.NOT_SECOND_CLASS -> {
                syncClass != FolderClass.SECOND_CLASS
            }
        }
    }

    private fun getEffectiveSyncClass(folderSettings: InternalSettingsMap): FolderClass {
        return folderSettings.getFolderClass("syncMode").takeIf { it != FolderClass.INHERITED }
            ?: folderSettings.getFolderClass("displayMode").takeIf { it != FolderClass.INHERITED }
            ?: FolderClass.NO_CLASS
    }

    private fun InternalSettingsMap.getFolderClass(key: String): FolderClass {
        return this[key] as? FolderClass ?: FolderClass.NO_CLASS
    }
}
