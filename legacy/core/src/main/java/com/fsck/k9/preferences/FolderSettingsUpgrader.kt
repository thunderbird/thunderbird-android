package com.fsck.k9.preferences

internal class FolderSettingsUpgrader(
    private val settingsDescriptions: SettingsDescriptions = FolderSettingsDescriptions.SETTINGS,
    private val upgraders: Map<Int, SettingsUpgrader> = FolderSettingsDescriptions.UPGRADERS,
) {
    fun upgrade(targetVersion: Int, contentVersion: Int, folder: ValidatedSettings.Folder): ValidatedSettings.Folder {
        if (contentVersion == targetVersion) {
            return folder
        }

        val upgradedSettings = SettingsUpgradeHelper.upgradeToVersion(
            targetVersion,
            contentVersion,
            upgraders,
            settingsDescriptions,
            folder.settings,
        )

        return folder.copy(settings = upgradedSettings)
    }
}
