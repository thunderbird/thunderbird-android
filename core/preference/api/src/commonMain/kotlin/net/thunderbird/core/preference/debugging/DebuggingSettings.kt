package net.thunderbird.core.preference.debugging

const val DEBUGGING_SETTINGS_DEFAULT_IS_SYNC_LOGGING_ENABLED = false
val DEBUGGING_SETTINGS_DEFAULT_IS_DEBUGGING_LOGGING_ENABLED = isDebug

data class DebuggingSettings(
    val isDebugLoggingEnabled: Boolean = DEBUGGING_SETTINGS_DEFAULT_IS_DEBUGGING_LOGGING_ENABLED,
    val isSyncLoggingEnabled: Boolean = DEBUGGING_SETTINGS_DEFAULT_IS_SYNC_LOGGING_ENABLED,
)
