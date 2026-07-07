package net.thunderbird.core.preference.privacy

import net.thunderbird.core.preference.PreferenceManager

enum class PrivacySettingKey(val value: String) {

    HideTimeZone("hideTimeZone"),
    HideUserAgent("hideUserAgent"),
}

interface PrivacySettingsPreferenceManager : PreferenceManager<PrivacySettings>
