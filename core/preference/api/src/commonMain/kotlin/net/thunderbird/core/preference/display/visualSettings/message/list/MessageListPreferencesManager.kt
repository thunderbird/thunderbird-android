package net.thunderbird.core.preference.display.visualSettings.message.list

import net.thunderbird.core.preference.PreferenceManager

const val KEY_COLORIZE_MISSING_CONTACT_PICTURE = "colorizeMissingContactPictures"
const val KEY_CHANGE_REGISTERED_NAME_COLOR = "changeRegisteredNameColor"
const val KEY_USE_BACKGROUND_AS_UNREAD_INDICATOR = "isUseBackgroundAsUnreadIndicator"
const val KEY_SHOW_CORRESPONDENT_NAMES = "showCorrespondentNames"
const val KEY_SHOW_CONTACT_NAME = "showContactName"
const val KEY_SHOW_CONTACT_PICTURE = "showContactPicture"
const val KEY_MESSAGE_LIST_VIEW_PREVIEW_LINES = "messageListPreviewLines"
const val KEY_MESSAGE_LIST_VIEW_DENSITY = "messageListDensity"

interface MessageListPreferencesManager : PreferenceManager<DisplayMessageListSettings>
