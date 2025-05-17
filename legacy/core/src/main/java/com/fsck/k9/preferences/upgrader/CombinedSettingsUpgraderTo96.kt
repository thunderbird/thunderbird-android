package com.fsck.k9.preferences.upgrader

import com.fsck.k9.preferences.CombinedSettingsUpgrader
import com.fsck.k9.preferences.InternalSettingsMap
import com.fsck.k9.preferences.ValidatedSettings
import com.fsck.k9.preferences.legacy.FolderClass
import net.thunderbird.core.android.account.FolderMode

class CombinedSettingsUpgraderTo96 : CombinedSettingsUpgrader {
    override fun upgrade(account: ValidatedSettings.Account): ValidatedSettings.Account {
        val notifyFolderMode = account.settings["folderNotifyNewMailMode"] as? FolderMode
            ?: FolderMode.NONE
        val ignoredFolders = getNotificationIgnoredFolders(account)

        val newFolders = account.folders.map { folder ->
            val notificationsEnabled = if (folder.name in ignoredFolders) {
                false
            } else {
                folder.isNotificationEnabled(notifyFolderMode)
            }

            val newFolderSettings = folder.settings.toMutableMap().apply {
                this["notificationsEnabled"] = notificationsEnabled
            }

            folder.copy(settings = newFolderSettings)
        }

        return account.copy(folders = newFolders)
    }

    private fun getNotificationIgnoredFolders(account: ValidatedSettings.Account): Set<String> {
        return buildSet {
            account.settings.getFolderNameUnlessInbox("trashFolderName")?.let { add(it) }
            account.settings.getFolderNameUnlessInbox("draftsFolderName")?.let { add(it) }
            account.settings.getFolderNameUnlessInbox("spamFolderName")?.let { add(it) }
            account.settings.getFolderNameUnlessInbox("sentFolderName")?.let { add(it) }
        }
    }

    private fun InternalSettingsMap.getFolderNameUnlessInbox(key: String): String? {
        return (this[key] as? String)?.takeIf { !it.equals("INBOX", ignoreCase = true) }
    }

    private fun ValidatedSettings.Folder.isNotificationEnabled(
        notifyFolderMode: FolderMode,
    ): Boolean {
        val notifyClass = getEffectiveNotifyClass(settings)

        return when (notifyFolderMode) {
            FolderMode.NONE -> {
                false
            }

            FolderMode.ALL -> {
                true
            }

            FolderMode.FIRST_CLASS -> {
                notifyClass == FolderClass.FIRST_CLASS
            }

            FolderMode.FIRST_AND_SECOND_CLASS -> {
                notifyClass == FolderClass.FIRST_CLASS || notifyClass == FolderClass.SECOND_CLASS
            }

            FolderMode.NOT_SECOND_CLASS -> {
                notifyClass != FolderClass.SECOND_CLASS
            }
        }
    }

    private fun getEffectiveNotifyClass(folderSettings: InternalSettingsMap): FolderClass {
        return folderSettings.getFolderClass("notifyMode").takeIf { it != FolderClass.INHERITED }
            ?: folderSettings.getFolderClass("pushMode").takeIf { it != FolderClass.INHERITED }
            ?: folderSettings.getFolderClass("syncMode").takeIf { it != FolderClass.INHERITED }
            ?: folderSettings.getFolderClass("displayMode").takeIf { it != FolderClass.INHERITED }
            ?: FolderClass.NO_CLASS
    }

    private fun InternalSettingsMap.getFolderClass(key: String): FolderClass {
        return this[key] as? FolderClass ?: FolderClass.NO_CLASS
    }
}
