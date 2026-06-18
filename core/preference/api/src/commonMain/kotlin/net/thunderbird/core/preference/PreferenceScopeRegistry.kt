package net.thunderbird.core.preference

import net.thunderbird.core.preference.debugging.DebugSettingKey
import net.thunderbird.core.preference.display.coreSettings.DisplayCoreSettingKey
import net.thunderbird.core.preference.display.inboxSettings.DisplayInboxSettingKey
import net.thunderbird.core.preference.display.miscSettings.DisplayMiscSettingKey
import net.thunderbird.core.preference.display.visualSettings.DisplayVisualSettingKey
import net.thunderbird.core.preference.display.visualSettings.message.list.DisplayMessageListSettingKey
import net.thunderbird.core.preference.interaction.InteractionSettingKey
import net.thunderbird.core.preference.network.NetworkSettingKey
import net.thunderbird.core.preference.notification.NotificationSettingKey
import net.thunderbird.core.preference.privacy.PrivacySettingKey

object PreferenceScopeRegistry {

    private val map: Map<String, PreferenceScope> = buildMap {

        DebugSettingKey.entries.forEach { put(it.value, PreferenceScope.DEBUGGING) }
        InteractionSettingKey.entries.forEach { put(it.value, PreferenceScope.INTERACTION) }
        NetworkSettingKey.entries.forEach { put(it.value, PreferenceScope.NETWORK) }
        NotificationSettingKey.entries.forEach { put(it.value, PreferenceScope.NOTIFICATION) }
        PrivacySettingKey.entries.forEach { put(it.value, PreferenceScope.PRIVACY) }

        DisplayCoreSettingKey.entries.forEach {
            put(it.value, PreferenceScope.DISPLAY_CORE)
        }

        DisplayInboxSettingKey.entries.forEach {
            put(it.value, PreferenceScope.DISPLAY_INBOX)
        }

        DisplayMiscSettingKey.entries.forEach {
            put(it.value, PreferenceScope.DISPLAY_MISC)
        }

        DisplayVisualSettingKey.entries.forEach {
            put(it.value, PreferenceScope.DISPLAY_VISUAL)
        }

        DisplayMessageListSettingKey.entries.forEach {
            put(it.value, PreferenceScope.DISPLAY_VISUAL_MESSAGE_LIST)
        }
    }

    fun getScope(key: String): PreferenceScope =
        map[key] ?: PreferenceScope.ALL
}

fun getPreferenceScope(changedKey: String): PreferenceScope {
    return PreferenceScopeRegistry.getScope(changedKey)
}
