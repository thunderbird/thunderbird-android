package com.fsck.k9.preferences

internal class GeneralSettingsValidator {
    fun validate(contentVersion: Int, settings: SettingsMap): InternalSettingsMap {
        return GeneralSettingsDescriptions.validate(contentVersion, settings)
    }
}
