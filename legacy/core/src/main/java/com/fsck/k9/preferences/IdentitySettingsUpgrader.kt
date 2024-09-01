package com.fsck.k9.preferences

internal class IdentitySettingsUpgrader(
    private val latestVersion: Int = Settings.VERSION,
    private val settingsDescriptions: SettingsDescriptions = IdentitySettingsDescriptions.SETTINGS,
    private val upgraders: Map<Int, SettingsUpgrader> = IdentitySettingsDescriptions.UPGRADERS,
) {
    fun upgrade(contentVersion: Int, identity: ValidatedSettings.Identity): ValidatedSettings.Identity {
        if (contentVersion == latestVersion) {
            return identity
        }

        val upgradedSettings = SettingsUpgradeHelper.upgrade(
            contentVersion,
            upgraders,
            settingsDescriptions,
            identity.settings,
        )

        return identity.copy(settings = upgradedSettings)
    }
}
