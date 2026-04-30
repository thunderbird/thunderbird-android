package net.thunderbird.core.preference.privacy

import net.thunderbird.core.preference.PreferenceManager

enum class PrivacySettingKey(val value: String) {

    HideTimeZone("hideTimeZone"),
    HideUserAgent("hideUserAgent"),
    ;

    companion object {

        fun isValid(value: String): Boolean {
            return entries.any { it.value == value }
        }
    }
}

interface PrivacySettingsPreferenceManager : PreferenceManager<PrivacySettings>
