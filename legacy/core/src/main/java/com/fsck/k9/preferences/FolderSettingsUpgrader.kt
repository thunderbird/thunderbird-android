package com.fsck.k9.preferences

internal class FolderSettingsUpgrader(
    private val latestVersion: Int = Settings.VERSION,
    private val settingsDescriptions: SettingsDescriptions = FolderSettingsDescriptions.SETTINGS,
    private val upgraders: Map<Int, SettingsUpgrader> = FolderSettingsDescriptions.UPGRADERS,
) {
    fun upgrade(contentVersion: Int, folder: ValidatedSettings.Folder): ValidatedSettings.Folder {
        if (contentVersion == latestVersion) {
            return folder
        }

        val upgradedSettings = SettingsUpgradeHelper.upgrade(
            contentVersion,
            upgraders,
            settingsDescriptions,
            folder.settings,
        )

        return folder.copy(settings = upgradedSettings)
    }
}
