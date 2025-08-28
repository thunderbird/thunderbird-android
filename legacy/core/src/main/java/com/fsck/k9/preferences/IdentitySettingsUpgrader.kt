package com.fsck.k9.preferences

import net.thunderbird.core.preference.GeneralSettingsManager

internal class IdentitySettingsUpgrader(
    private val settingsDescriptions: SettingsDescriptions = IdentitySettingsDescriptions.SETTINGS,
    private val upgraders: Map<Int, SettingsUpgrader> = IdentitySettingsDescriptions.UPGRADERS,
    private val generalSettingsManager: GeneralSettingsManager,
) {
    fun upgrade(
        targetVersion: Int,
        contentVersion: Int,
        identity: ValidatedSettings.Identity,
    ): ValidatedSettings.Identity {
        if (contentVersion == targetVersion) {
            return identity
        }

        val upgradedSettings = SettingsUpgradeHelper.upgradeToVersion(
            targetVersion,
            contentVersion,
            upgraders,
            settingsDescriptions,
            identity.settings,
            generalSettingsManager,
        )

        return identity.copy(settings = upgradedSettings)
    }
}
