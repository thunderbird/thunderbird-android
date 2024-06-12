package com.fsck.k9.preferences

internal typealias InternalSettingsMap = Map<String, Any>

internal class GeneralSettingsValidator {
    fun validate(contentVersion: Int, settings: SettingsMap): InternalSettingsMap {
        return GeneralSettingsDescriptions.validate(contentVersion, settings)
    }
}
