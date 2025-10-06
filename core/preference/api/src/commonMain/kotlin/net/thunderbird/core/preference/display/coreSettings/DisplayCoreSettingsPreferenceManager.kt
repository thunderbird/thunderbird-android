package net.thunderbird.core.preference.display.coreSettings

import net.thunderbird.core.preference.PreferenceManager

const val KEY_FIXED_MESSAGE_VIEW_THEME = "fixedMessageViewTheme"
const val KEY_MESSAGE_VIEW_THEME = "messageViewTheme"
const val KEY_MESSAGE_COMPOSE_THEME = "messageComposeTheme"
const val KEY_APP_LANGUAGE = "language"
const val KEY_SPLIT_VIEW_MODE = "splitViewMode"

interface DisplayCoreSettingsPreferenceManager : PreferenceManager<DisplayCoreSettings>
