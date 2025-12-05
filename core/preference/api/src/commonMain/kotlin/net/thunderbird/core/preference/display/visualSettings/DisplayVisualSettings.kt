package net.thunderbird.core.preference.display.visualSettings

import net.thunderbird.core.preference.BodyContentType
import net.thunderbird.core.preference.display.visualSettings.message.list.DisplayMessageListSettings

const val DISPLAY_SETTINGS_DEFAULT_IS_USE_MESSAGE_VIEW_FIXED_WIDTH_FONT = false
const val DISPLAY_SETTINGS_DEFAULT_IS_AUTO_FIT_WIDTH = true
const val DISPLAY_SETTINGS_DEFAULT_IS_SHOW_ANIMATION = true
val DISPLAY_SETTINGS_DEFAULT_BODY_CONTENT_TYPE = BodyContentType.TEXT_HTML
const val DISPLAY_SETTINGS_DEFAULT_DRAWER_EXPAND_ALL_FOLDER = false

data class DisplayVisualSettings(
    val isShowAnimations: Boolean = DISPLAY_SETTINGS_DEFAULT_IS_SHOW_ANIMATION,
    val isUseMessageViewFixedWidthFont: Boolean = DISPLAY_SETTINGS_DEFAULT_IS_USE_MESSAGE_VIEW_FIXED_WIDTH_FONT,
    val isAutoFitWidth: Boolean = DISPLAY_SETTINGS_DEFAULT_IS_AUTO_FIT_WIDTH,
    val bodyContentType: BodyContentType = DISPLAY_SETTINGS_DEFAULT_BODY_CONTENT_TYPE,
    val drawerExpandAllFolder: Boolean = DISPLAY_SETTINGS_DEFAULT_DRAWER_EXPAND_ALL_FOLDER,
    val messageListSettings: DisplayMessageListSettings = DisplayMessageListSettings(),
)
