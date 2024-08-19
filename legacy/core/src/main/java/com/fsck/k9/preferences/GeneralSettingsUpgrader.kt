package com.fsck.k9.preferences

internal class GeneralSettingsUpgrader {
    fun upgrade(contentVersion: Int, internalSettings: InternalSettingsMap): InternalSettingsMap {
        if (contentVersion == Settings.VERSION) {
            return internalSettings
        }

        return GeneralSettingsDescriptions.upgrade(contentVersion, internalSettings)
    }
}
