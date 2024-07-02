package com.fsck.k9.preferences

internal class GeneralSettingsUpgrader {
    fun upgrade(contentVersion: Int, internalSettings: InternalSettingsMap): InternalSettingsMap {
        val settings = internalSettings.toMutableMap()
        if (contentVersion != Settings.VERSION) {
            GeneralSettingsDescriptions.upgrade(contentVersion, settings)
        }

        return settings.toMap()
    }
}
