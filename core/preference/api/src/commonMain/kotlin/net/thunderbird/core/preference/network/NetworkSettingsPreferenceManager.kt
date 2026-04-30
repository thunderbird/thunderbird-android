package net.thunderbird.core.preference.network

import net.thunderbird.core.preference.PreferenceManager

enum class NetworkSettingKey(val value: String) {

    BackgroundOperations("backgroundOperations"),
    ;
}

    companion object {

        fun isValid(value: String): Boolean {
            return entries.any { it.value == value }
        }
    }
}
interface NetworkSettingsPreferenceManager : PreferenceManager<NetworkSettings>
