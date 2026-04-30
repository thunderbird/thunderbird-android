package net.thunderbird.core.preference.display.coreSettings

import net.thunderbird.core.preference.PreferenceManager

enum class DisplayCoreSettingKey(val value: String) {

    FixedMessageViewTheme("fixedMessageViewTheme"),
    MessageViewTheme("messageViewTheme"),
    MessageComposeTheme("messageComposeTheme"),
    AppLanguage("language"),
    SplitViewMode("splitViewMode"),
    Theme("theme"),
    ;

    companion object {

        fun isValid(value: String): Boolean {
            return entries.any { it.value == value }
        }
    }
}

interface DisplayCoreSettingsPreferenceManager : PreferenceManager<DisplayCoreSettings>
