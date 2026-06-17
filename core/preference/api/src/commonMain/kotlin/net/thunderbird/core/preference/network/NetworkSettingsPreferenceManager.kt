package net.thunderbird.core.preference.network

import net.thunderbird.core.preference.PreferenceManager

enum class NetworkSettingKey(val value: String) {
    BackgroundOperations("backgroundOperations"),
    IsProxyEnabled("isProxyEnabled"),
    DefaultProxyType("defaultProxyType"),
    DefaultProxyHost("defaultProxyHost"),
    DefaultProxyPort("defaultProxyPort"),
    DefaultProxyDns("defaultProxyDns"),
    DefaultProxyUsername("defaultProxyUsername"),
    DefaultProxyPassword("defaultProxyPassword"),
}

interface NetworkSettingsPreferenceManager : PreferenceManager<NetworkSettings>
