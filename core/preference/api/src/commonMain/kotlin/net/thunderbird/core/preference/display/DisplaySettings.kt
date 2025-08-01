package net.thunderbird.core.preference.display

import net.thunderbird.core.preference.AppTheme
import net.thunderbird.core.preference.SubTheme

const val DISPLAY_SETTINGS_DEFAULT_FIXED_MESSAGE_VIEW_THEME = true
const val DISPLAY_SETTINGS_DEFAULT_IS_SHOW_UNIFIED_INBOX = false
const val DISPLAY_SETTINGS_DEFAULT_IS_SHOW_STAR_COUNT = false
const val DISPLAY_SETTINGS_DEFAULT_IS_SHOW_MESSAGE_LIST_STAR = true
const val DISPLAY_SETTINGS_DEFAULT_IS_SHOW_ANIMATION = true
const val DISPLAY_SETTINGS_DEFAULT_IS_SHOW_CORRESPONDENT_NAMES = true
val DISPLAY_SETTINGS_DEFAULT_APP_THEME = AppTheme.FOLLOW_SYSTEM
const val DISPLAY_SETTINGS_DEFAULT_SHOW_RECENT_CHANGES = true
val DISPLAY_SETTINGS_DEFAULT_MESSAGE_VIEW_THEME = SubTheme.USE_GLOBAL
val DISPLAY_SETTINGS_DEFAULT_MESSAGE_COMPOSE_THEME = SubTheme.USE_GLOBAL
const val DISPLAY_SETTINGS_DEFAULT_SHOULD_SHOW_SETUP_ARCHIVE_FOLDER_DIALOG = true
const val DISPLAY_SETTINGS_DEFAULT_IS_MESSAGE_LIST_SENDER_ABOVE_SUBJECT = false
const val DISPLAY_SETTINGS_DEFAULT_IS_SHOW_CONTACT_NAME = false
const val DISPLAY_SETTINGS_DEFAULT_IS_SHOW_CONTACT_PICTURE = true
const val DISPLAY_SETTINGS_DEFAULT_IS_CHANGE_CONTACT_NAME_COLOR = true
const val DISPLAY_SETTINGS_DEFAULT_IS_COLORIZE_MISSING_CONTACT_PICTURE = false
const val DISPLAY_SETTINGS_DEFAULT_IS_USE_BACKGROUND_AS_INDICATOR = false
const val DISPLAY_SETTINGS_DEFAULT_IS_SHOW_COMPOSE_BUTTON_ON_MESSAGE_LIST = true
const val DISPLAY_SETTINGS_DEFAULT_IS_THREAD_VIEW_ENABLED = true
const val DISPLAY_SETTINGS_DEFAULT_IS_USE_MESSAGE_VIEW_FIXED_WIDTH_FONT = false
const val DISPLAY_SETTINGS_DEFAULT_IS_AUTO_FIT_WIDTH = true
const val DISPLAY_SETTINGS_DEFAULT_APP_LANGUAGE = ""

data class DisplaySettings(
    val fixedMessageViewTheme: Boolean = DISPLAY_SETTINGS_DEFAULT_FIXED_MESSAGE_VIEW_THEME,
    val isShowUnifiedInbox: Boolean = DISPLAY_SETTINGS_DEFAULT_IS_SHOW_UNIFIED_INBOX,
    val isShowStarredCount: Boolean = DISPLAY_SETTINGS_DEFAULT_IS_SHOW_STAR_COUNT,
    val isShowMessageListStars: Boolean = DISPLAY_SETTINGS_DEFAULT_IS_SHOW_MESSAGE_LIST_STAR,
    val isShowAnimations: Boolean = DISPLAY_SETTINGS_DEFAULT_IS_SHOW_ANIMATION,
    val isShowCorrespondentNames: Boolean = DISPLAY_SETTINGS_DEFAULT_IS_SHOW_CORRESPONDENT_NAMES,
    val appTheme: AppTheme = DISPLAY_SETTINGS_DEFAULT_APP_THEME,
    val showRecentChanges: Boolean = DISPLAY_SETTINGS_DEFAULT_SHOW_RECENT_CHANGES,
    val messageViewTheme: SubTheme = DISPLAY_SETTINGS_DEFAULT_MESSAGE_VIEW_THEME,
    val messageComposeTheme: SubTheme = DISPLAY_SETTINGS_DEFAULT_MESSAGE_COMPOSE_THEME,
    val shouldShowSetupArchiveFolderDialog: Boolean = DISPLAY_SETTINGS_DEFAULT_SHOULD_SHOW_SETUP_ARCHIVE_FOLDER_DIALOG,
    val isMessageListSenderAboveSubject: Boolean = DISPLAY_SETTINGS_DEFAULT_IS_MESSAGE_LIST_SENDER_ABOVE_SUBJECT,
    val isShowContactName: Boolean = DISPLAY_SETTINGS_DEFAULT_IS_SHOW_CONTACT_NAME,
    val isShowContactPicture: Boolean = DISPLAY_SETTINGS_DEFAULT_IS_SHOW_CONTACT_PICTURE,
    val isChangeContactNameColor: Boolean = DISPLAY_SETTINGS_DEFAULT_IS_CHANGE_CONTACT_NAME_COLOR,
    val isColorizeMissingContactPictures: Boolean = DISPLAY_SETTINGS_DEFAULT_IS_COLORIZE_MISSING_CONTACT_PICTURE,
    val isUseBackgroundAsUnreadIndicator: Boolean = DISPLAY_SETTINGS_DEFAULT_IS_USE_BACKGROUND_AS_INDICATOR,
    val isShowComposeButtonOnMessageList: Boolean = DISPLAY_SETTINGS_DEFAULT_IS_SHOW_COMPOSE_BUTTON_ON_MESSAGE_LIST,
    val isThreadedViewEnabled: Boolean = DISPLAY_SETTINGS_DEFAULT_IS_THREAD_VIEW_ENABLED,
    val isUseMessageViewFixedWidthFont: Boolean = DISPLAY_SETTINGS_DEFAULT_IS_USE_MESSAGE_VIEW_FIXED_WIDTH_FONT,
    val isAutoFitWidth: Boolean = DISPLAY_SETTINGS_DEFAULT_IS_AUTO_FIT_WIDTH,
    val appLanguage: String = DISPLAY_SETTINGS_DEFAULT_APP_LANGUAGE,
)
