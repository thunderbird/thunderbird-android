package net.thunderbird.core.preference.debugging

const val DEBUGGING_SETTINGS_DEFAULT_IS_SYNC_LOGGING_ENABLED = false
const val DEBUGGING_SETTINGS_DEFAULT_SENSITIVE_LOGGING_ENABLED = false

data class DebuggingSettings(
    val isDebugLoggingEnabled: Boolean,
    val isSyncLoggingEnabled: Boolean = DEBUGGING_SETTINGS_DEFAULT_IS_SYNC_LOGGING_ENABLED,
    val isSensitiveLoggingEnabled: Boolean = DEBUGGING_SETTINGS_DEFAULT_SENSITIVE_LOGGING_ENABLED,
)
