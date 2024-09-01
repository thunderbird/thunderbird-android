package com.fsck.k9.preferences

internal class ServerSettingsUpgrader(
    private val latestVersion: Int = Settings.VERSION,
    private val settingsDescriptions: SettingsDescriptions = ServerSettingsDescriptions.SETTINGS,
    private val upgraders: Map<Int, SettingsUpgrader> = ServerSettingsDescriptions.UPGRADERS,
) {
    fun upgrade(contentVersion: Int, server: ValidatedSettings.Server): ValidatedSettings.Server {
        if (contentVersion == latestVersion) {
            return server
        }

        val upgradedSettings = SettingsUpgradeHelper.upgrade(
            contentVersion,
            upgraders,
            settingsDescriptions,
            server.settings,
        )

        return server.copy(settings = upgradedSettings)
    }
}
