package com.fsck.k9.preferences

internal class IdentitySettingsUpgrader {
    fun upgrade(contentVersion: Int, identity: ValidatedSettings.Identity): ValidatedSettings.Identity {
        if (contentVersion == Settings.VERSION) {
            return identity
        }

        val upgradedSettings = IdentitySettingsDescriptions.upgrade(contentVersion, identity.settings)

        return identity.copy(settings = upgradedSettings)
    }
}
