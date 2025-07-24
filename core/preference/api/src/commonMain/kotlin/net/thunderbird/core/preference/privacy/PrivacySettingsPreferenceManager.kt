package net.thunderbird.core.preference.privacy

import net.thunderbird.core.preference.PreferenceManager

const val KEY_HIDE_TIME_ZONE = "hideTimeZone"
const val KEY_HIDE_USER_AGENT = "hideUserAgent"

interface PrivacySettingsPreferenceManager : PreferenceManager<PrivacySettings>
