package com.fsck.k9.preferences

import net.thunderbird.core.preference.GeneralSettingsManager

internal class ServerSettingsUpgrader(
    private val settingsDescriptions: SettingsDescriptions = ServerSettingsDescriptions.SETTINGS,
    private val upgraders: Map<Int, SettingsUpgrader> = ServerSettingsDescriptions.UPGRADERS,
    private val generalSettingsManager: GeneralSettingsManager,
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
            generalSettingsManager,
        )

        return server.copy(settings = upgradedSettings)
    }
}
