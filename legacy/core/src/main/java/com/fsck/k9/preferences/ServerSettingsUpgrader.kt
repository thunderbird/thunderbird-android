package com.fsck.k9.preferences

internal class ServerSettingsUpgrader(
    private val settingsDescriptions: SettingsDescriptions = ServerSettingsDescriptions.SETTINGS,
    private val upgraders: Map<Int, SettingsUpgrader> = ServerSettingsDescriptions.UPGRADERS,
) {
    fun upgrade(targetVersion: Int, contentVersion: Int, server: ValidatedSettings.Server): ValidatedSettings.Server {
        if (contentVersion == targetVersion) {
            return server
        }

        val upgradedSettings = SettingsUpgradeHelper.upgradeToVersion(
            targetVersion,
            contentVersion,
            upgraders,
            settingsDescriptions,
            server.settings,
        )

        return server.copy(settings = upgradedSettings)
    }
}
