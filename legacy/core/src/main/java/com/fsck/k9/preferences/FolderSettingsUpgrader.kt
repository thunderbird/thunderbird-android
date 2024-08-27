package com.fsck.k9.preferences

internal class FolderSettingsUpgrader {
    fun upgrade(contentVersion: Int, folder: ValidatedSettings.Folder): ValidatedSettings.Folder {
        if (contentVersion == Settings.VERSION) {
            return folder
        }

        val upgradedSettings = FolderSettingsDescriptions.upgrade(contentVersion, folder.settings)

        return folder.copy(settings = upgradedSettings)
    }
}
