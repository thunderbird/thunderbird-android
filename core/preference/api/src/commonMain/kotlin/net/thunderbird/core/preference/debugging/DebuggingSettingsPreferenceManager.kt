package net.thunderbird.core.preference.debugging

import net.thunderbird.core.preference.PreferenceManager

const val KEY_ENABLE_DEBUG_LOGGING = "enableDebugLogging"

interface DebuggingSettingsPreferenceManager : PreferenceManager<DebuggingSettings>
