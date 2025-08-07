package net.thunderbird.core.preference.debugging

val DEBUGGING_SETTINGS_DEFAULT_IS_DEBUGGING_LOGGING_ENABLED = isDebug

data class DebuggingSettings(
    val isDebugLoggingEnabled: Boolean = DEBUGGING_SETTINGS_DEFAULT_IS_DEBUGGING_LOGGING_ENABLED,
)
