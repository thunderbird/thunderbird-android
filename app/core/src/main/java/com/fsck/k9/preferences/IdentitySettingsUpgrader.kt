package com.fsck.k9.preferences

internal class IdentitySettingsUpgrader {
    fun upgrade(contentVersion: Int, identity: ValidatedSettings.Identity): ValidatedSettings.Identity {
        val settings = identity.settings.toMutableMap()

        // Upgrade identity settings to current content version
        if (contentVersion != Settings.VERSION) {
            IdentitySettingsDescriptions.upgrade(contentVersion, settings)
        }

        return identity.copy(settings = settings.toMap())
    }
}
