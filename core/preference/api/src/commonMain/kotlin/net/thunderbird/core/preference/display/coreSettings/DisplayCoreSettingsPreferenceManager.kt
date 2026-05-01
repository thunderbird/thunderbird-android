package net.thunderbird.core.preference.display.coreSettings

import net.thunderbird.core.preference.PreferenceManager

enum class DisplayCoreSettingKey(val value: String) {

    FixedMessageViewTheme("fixedMessageViewTheme"),
    MessageViewTheme("messageViewTheme"),
    MessageComposeTheme("messageComposeTheme"),
    AppLanguage("language"),
    SplitViewMode("splitViewMode"),
    Theme("theme"),
}

interface DisplayCoreSettingsPreferenceManager : PreferenceManager<DisplayCoreSettings>
