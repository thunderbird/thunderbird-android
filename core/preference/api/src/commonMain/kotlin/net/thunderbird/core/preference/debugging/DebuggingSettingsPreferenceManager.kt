package net.thunderbird.core.preference.debugging

import net.thunderbird.core.preference.PreferenceManager

enum class DebugSettingKey(val value: String) {

    EnableDebugLogging("enableDebugLogging"),
    EnableSyncDebugLogging("enableSyncDebugLogging"),
    EnableSensitiveLogging("enableSensitiveLogging"),
    ;

    companion object {

        fun isValid(value: String): Boolean {
            return entries.any { it.value == value }
        }
    }
}

interface DebuggingSettingsPreferenceManager : PreferenceManager<DebuggingSettings>
