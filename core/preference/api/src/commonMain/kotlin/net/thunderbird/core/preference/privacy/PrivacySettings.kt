package net.thunderbird.core.preference.privacy

const val PRIVACY_SETTINGS_DEFAULT_HIDE_TIME_ZONE = false
const val PRIVACY_SETTINGS_DEFAULT_HIDE_USER_AGENT = false

data class PrivacySettings(
    val isHideTimeZone: Boolean = PRIVACY_SETTINGS_DEFAULT_HIDE_TIME_ZONE,
    val isHideUserAgent: Boolean = PRIVACY_SETTINGS_DEFAULT_HIDE_USER_AGENT,
)
