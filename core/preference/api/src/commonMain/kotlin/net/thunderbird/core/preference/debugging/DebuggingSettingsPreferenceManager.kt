package net.thunderbird.core.preference.debugging

import net.thunderbird.core.preference.PreferenceManager

enum class DebugSettingKey(val value: String) {

    EnableDebugLogging("enableDebugLogging"),
    EnableSyncDebugLogging("enableSyncDebugLogging"),
    EnableSensitiveLogging("enableSensitiveLogging"),
}

interface DebuggingSettingsPreferenceManager : PreferenceManager<DebuggingSettings>
