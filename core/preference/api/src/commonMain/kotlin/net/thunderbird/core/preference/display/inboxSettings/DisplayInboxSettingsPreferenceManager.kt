package net.thunderbird.core.preference.display.inboxSettings

import net.thunderbird.core.preference.PreferenceManager

const val KEY_MESSAGE_LIST_SENDER_ABOVE_SUBJECT = "messageListSenderAboveSubject"
const val KEY_SHOW_COMPOSE_BUTTON_ON_MESSAGE_LIST = "showComposeButtonOnMessageList"
const val KEY_SHOW_MESSAGE_LIST_STARS = "messageListStars"
const val KEY_SHOW_STAR_COUNT = "showStarredCount"
const val KEY_SHOW_UNIFIED_INBOX = "showUnifiedInbox"
const val KEY_THREAD_VIEW_ENABLED = "isThreadedViewEnabled"

interface DisplayInboxSettingsPreferenceManager : PreferenceManager<DisplayInboxSettings>
