package net.thunderbird.core.preference.debugging

import net.thunderbird.core.preference.PreferenceManager

const val KEY_ENABLE_DEBUG_LOGGING = "enableDebugLogging"
const val KEY_ENABLE_SYNC_DEBUG_LOGGING = "enableSyncDebugLogging"
const val KEY_ENABLE_SENSITIVE_LOGGING = "enableSensitiveLogging"

interface DebuggingSettingsPreferenceManager : PreferenceManager<DebuggingSettings>
