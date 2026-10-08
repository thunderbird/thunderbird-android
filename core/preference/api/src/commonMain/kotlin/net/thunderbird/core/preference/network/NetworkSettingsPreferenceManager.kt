package net.thunderbird.core.preference.network

import net.thunderbird.core.preference.PreferenceManager

enum class NetworkSettingKey(val value: String) {
    BackgroundOperations("backgroundOperations"),
}

interface NetworkSettingsPreferenceManager : PreferenceManager<NetworkSettings>
