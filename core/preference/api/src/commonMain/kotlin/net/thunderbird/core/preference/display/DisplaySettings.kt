package net.thunderbird.core.preference.display

import net.thunderbird.core.preference.display.coreSettings.DisplayCoreSettings
import net.thunderbird.core.preference.display.inboxSettings.DisplayInboxSettings

const val DISPLAY_SETTINGS_DEFAULT_IS_SHOW_ANIMATION = true
const val DISPLAY_SETTINGS_DEFAULT_IS_SHOW_CORRESPONDENT_NAMES = true
const val DISPLAY_SETTINGS_DEFAULT_SHOW_RECENT_CHANGES = true
const val DISPLAY_SETTINGS_DEFAULT_SHOULD_SHOW_SETUP_ARCHIVE_FOLDER_DIALOG = true
const val DISPLAY_SETTINGS_DEFAULT_IS_SHOW_CONTACT_NAME = false
const val DISPLAY_SETTINGS_DEFAULT_IS_SHOW_CONTACT_PICTURE = true
const val DISPLAY_SETTINGS_DEFAULT_IS_CHANGE_CONTACT_NAME_COLOR = true
const val DISPLAY_SETTINGS_DEFAULT_IS_COLORIZE_MISSING_CONTACT_PICTURE = false
const val DISPLAY_SETTINGS_DEFAULT_IS_USE_BACKGROUND_AS_INDICATOR = false
const val DISPLAY_SETTINGS_DEFAULT_IS_USE_MESSAGE_VIEW_FIXED_WIDTH_FONT = false
const val DISPLAY_SETTINGS_DEFAULT_IS_AUTO_FIT_WIDTH = true

data class DisplaySettings(
    val coreSettings: DisplayCoreSettings = DisplayCoreSettings(),
    val inboxSettings: DisplayInboxSettings = DisplayInboxSettings(),
    val isShowAnimations: Boolean = DISPLAY_SETTINGS_DEFAULT_IS_SHOW_ANIMATION,
    val isShowCorrespondentNames: Boolean = DISPLAY_SETTINGS_DEFAULT_IS_SHOW_CORRESPONDENT_NAMES,
    val showRecentChanges: Boolean = DISPLAY_SETTINGS_DEFAULT_SHOW_RECENT_CHANGES,
    val shouldShowSetupArchiveFolderDialog: Boolean = DISPLAY_SETTINGS_DEFAULT_SHOULD_SHOW_SETUP_ARCHIVE_FOLDER_DIALOG,
    val isShowContactName: Boolean = DISPLAY_SETTINGS_DEFAULT_IS_SHOW_CONTACT_NAME,
    val isShowContactPicture: Boolean = DISPLAY_SETTINGS_DEFAULT_IS_SHOW_CONTACT_PICTURE,
    val isChangeContactNameColor: Boolean = DISPLAY_SETTINGS_DEFAULT_IS_CHANGE_CONTACT_NAME_COLOR,
    val isColorizeMissingContactPictures: Boolean = DISPLAY_SETTINGS_DEFAULT_IS_COLORIZE_MISSING_CONTACT_PICTURE,
    val isUseBackgroundAsUnreadIndicator: Boolean = DISPLAY_SETTINGS_DEFAULT_IS_USE_BACKGROUND_AS_INDICATOR,
    val isUseMessageViewFixedWidthFont: Boolean = DISPLAY_SETTINGS_DEFAULT_IS_USE_MESSAGE_VIEW_FIXED_WIDTH_FONT,
    val isAutoFitWidth: Boolean = DISPLAY_SETTINGS_DEFAULT_IS_AUTO_FIT_WIDTH,
)
