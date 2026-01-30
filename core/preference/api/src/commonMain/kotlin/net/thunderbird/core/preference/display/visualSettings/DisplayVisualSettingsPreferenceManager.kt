package net.thunderbird.core.preference.display.visualSettings

import net.thunderbird.core.preference.PreferenceManager

const val KEY_ANIMATION = "animations"
const val KEY_MESSAGE_VIEW_FIXED_WIDTH_FONT = "messageViewFixedWidthFont"
const val KEY_AUTO_FIT_WIDTH = "autofitWidth"
const val KEY_MESSAGE_VIEW_BODY_CONTENT_TYPE = "messageViewBodyContentType"
const val KEY_DRAWER_EXPAND_ALL_FOLDER = "drawerExpandAllFolder"

interface DisplayVisualSettingsPreferenceManager : PreferenceManager<DisplayVisualSettings>
