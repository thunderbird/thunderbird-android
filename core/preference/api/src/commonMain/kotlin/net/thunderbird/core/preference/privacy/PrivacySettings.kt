package net.thunderbird.core.preference.privacy

const val PRIVACY_SETTINGS_DEFAULT_HIDE_TIME_ZONE = false
const val PRIVACY_SETTINGS_DEFAULT_HIDE_USER_AGENT = false
const val PRIVACY_SETTINGS_DEFAULT_HIDE_NOTIFICATION_CONTENT = false
const val PRIVACY_SETTINGS_DEFAULT_PRIVATE_KEYBOARD_ENABLED = true

data class PrivacySettings(
    val isHideTimeZone: Boolean = PRIVACY_SETTINGS_DEFAULT_HIDE_TIME_ZONE,
    val isHideUserAgent: Boolean = PRIVACY_SETTINGS_DEFAULT_HIDE_USER_AGENT,
    val isHideNotificationContent: Boolean = PRIVACY_SETTINGS_DEFAULT_HIDE_NOTIFICATION_CONTENT,
    val isPrivateKeyboardEnabled: Boolean = PRIVACY_SETTINGS_DEFAULT_PRIVATE_KEYBOARD_ENABLED,
)
