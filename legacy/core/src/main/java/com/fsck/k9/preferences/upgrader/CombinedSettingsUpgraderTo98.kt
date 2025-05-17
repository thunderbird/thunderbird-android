package com.fsck.k9.preferences.upgrader

import com.fsck.k9.preferences.CombinedSettingsUpgrader
import com.fsck.k9.preferences.InternalSettingsMap
import com.fsck.k9.preferences.ValidatedSettings
import com.fsck.k9.preferences.legacy.FolderClass
import net.thunderbird.core.android.account.FolderMode

class CombinedSettingsUpgraderTo98 : CombinedSettingsUpgrader {
    override fun upgrade(account: ValidatedSettings.Account): ValidatedSettings.Account {
        val folderPushMode = account.settings["folderPushMode"] as? FolderMode
            ?: FolderMode.NONE

        val newFolders = account.folders.map { folder ->
            val newFolderSettings = folder.settings.toMutableMap().apply {
                this["pushEnabled"] = folder.isPushEnabled(folderPushMode)
            }

            folder.copy(settings = newFolderSettings)
        }

        return account.copy(folders = newFolders)
    }

    private fun ValidatedSettings.Folder.isPushEnabled(
        folderPushMode: FolderMode,
    ): Boolean {
        val pushClass = getEffectivePushClass(settings)

        return when (folderPushMode) {
            FolderMode.NONE -> {
                false
            }

            FolderMode.ALL -> {
                true
            }

            FolderMode.FIRST_CLASS -> {
                pushClass == FolderClass.FIRST_CLASS
            }

            FolderMode.FIRST_AND_SECOND_CLASS -> {
                pushClass == FolderClass.FIRST_CLASS || pushClass == FolderClass.SECOND_CLASS
            }

            FolderMode.NOT_SECOND_CLASS -> {
                pushClass != FolderClass.SECOND_CLASS
            }
        }
    }

    private fun getEffectivePushClass(folderSettings: InternalSettingsMap): FolderClass {
        return folderSettings.getFolderClass("pushMode").takeIf { it != FolderClass.INHERITED }
            ?: folderSettings.getFolderClass("syncMode").takeIf { it != FolderClass.INHERITED }
            ?: folderSettings.getFolderClass("displayMode").takeIf { it != FolderClass.INHERITED }
            ?: FolderClass.NO_CLASS
    }

    private fun InternalSettingsMap.getFolderClass(key: String): FolderClass {
        return this[key] as? FolderClass ?: FolderClass.NO_CLASS
    }
}
