package net.thunderbird.core.preference.display

import net.thunderbird.core.preference.PreferenceManager

const val KEY_SHOW_UNIFIED_INBOX = "showUnifiedInbox"
const val KEY_SHOW_STAR_COUNT = "showStarredCount"
const val KEY_SHOW_CONTACT_NAME = "showContactName"
const val KEY_SHOW_CORRESPONDENT_NAMES = "showCorrespondentNames"
const val KEY_SHOW_MESSAGE_LIST_STARS = "messageListStars"
const val KEY_SHOW_RECENT_CHANGES = "showRecentChanges"
const val KEY_THEME = "theme"
const val KEY_ANIMATION = "animations"
const val KEY_SHOULD_SHOW_SETUP_ARCHIVE_FOLDER_DIALOG = "shouldShowSetupArchiveFolderDialog"
const val KEY_CHANGE_REGISTERED_NAME_COLOR = "changeRegisteredNameColor"
const val KEY_COLORIZE_MISSING_CONTACT_PICTURE = "colorizeMissingContactPictures"
const val KEY_USE_BACKGROUND_AS_UNREAD_INDICATOR = "isUseBackgroundAsUnreadIndicator"
const val KEY_SHOW_COMPOSE_BUTTON_ON_MESSAGE_LIST = "showComposeButtonOnMessageList"
const val KEY_THREAD_VIEW_ENABLED = "isThreadedViewEnabled"
const val KEY_MESSAGE_VIEW_FIXED_WIDTH_FONT = "messageViewFixedWidthFont"
const val KEY_AUTO_FIT_WIDTH = "autofitWidth"
const val KEY_MESSAGE_LIST_SENDER_ABOVE_SUBJECT = "messageListSenderAboveSubject"
const val KEY_SHOW_CONTACT_PICTURE = "showContactPicture"

interface DisplaySettingsPreferenceManager : PreferenceManager<DisplaySettings>
