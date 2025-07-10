package net.thunderbird.core.preference.privacy

class DefaultPrivacySettingsManager(
    private val preferenceManager: PrivacySettingsPreferenceManager,
) : PrivacySettingsManager {
    override val privacySettings: PrivacySettings
        get() = preferenceManager.getConfig()

    override fun setIsHideTimeZone(isHideTimeZone: Boolean) {
        val privacySettings = preferenceManager.getConfig()
        preferenceManager.save(privacySettings.copy(isHideTimeZone = isHideTimeZone))
    }

    override fun setIsHideUserAgent(isHideUserAgent: Boolean) {
        val privacySettings = preferenceManager.getConfig()
        preferenceManager.save(privacySettings.copy(isHideUserAgent = isHideUserAgent))
    }
}
