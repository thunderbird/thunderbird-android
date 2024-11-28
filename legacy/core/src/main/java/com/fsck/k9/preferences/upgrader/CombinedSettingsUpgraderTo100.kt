package com.fsck.k9.preferences.upgrader

import app.k9mail.legacy.account.Account.FolderMode
import com.fsck.k9.preferences.CombinedSettingsUpgrader
import com.fsck.k9.preferences.InternalSettingsMap
import com.fsck.k9.preferences.ValidatedSettings
import com.fsck.k9.preferences.legacy.FolderClass

class CombinedSettingsUpgraderTo100 : CombinedSettingsUpgrader {
    override fun upgrade(account: ValidatedSettings.Account): ValidatedSettings.Account {
        val folderDisplayMode = account.settings["folderDisplayMode"] as? FolderMode ?: FolderMode.NONE

        val newFolders = account.folders.map { folder ->
            val newFolderSettings = folder.settings.toMutableMap().apply {
                this["visible"] = folder.isVisible(folderDisplayMode)
            }

            folder.copy(settings = newFolderSettings)
        }

        return account.copy(folders = newFolders)
    }

    private fun ValidatedSettings.Folder.isVisible(folderDisplayMode: FolderMode): Boolean {
        val displayClass = getEffectiveDisplayClass(settings)

        return when (folderDisplayMode) {
            FolderMode.NONE -> {
                false
            }

            FolderMode.ALL -> {
                true
            }

            FolderMode.FIRST_CLASS -> {
                displayClass == FolderClass.FIRST_CLASS
            }

            FolderMode.FIRST_AND_SECOND_CLASS -> {
                displayClass == FolderClass.FIRST_CLASS || displayClass == FolderClass.SECOND_CLASS
            }

            FolderMode.NOT_SECOND_CLASS -> {
                displayClass != FolderClass.SECOND_CLASS
            }
        }
    }

    private fun getEffectiveDisplayClass(folderSettings: InternalSettingsMap): FolderClass {
        return folderSettings.getFolderClass("displayMode").takeIf { it != FolderClass.INHERITED }
            ?: FolderClass.NO_CLASS
    }

    private fun InternalSettingsMap.getFolderClass(key: String): FolderClass {
        return this[key] as? FolderClass ?: FolderClass.NO_CLASS
    }
}
