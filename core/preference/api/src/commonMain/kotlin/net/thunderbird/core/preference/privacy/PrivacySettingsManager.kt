package net.thunderbird.core.preference.privacy

interface PrivacySettingsManager {
    val privacySettings: PrivacySettings

    fun setIsHideTimeZone(isHideTimeZone: Boolean)
}
