package net.thunderbird.core.preference.interaction

import net.thunderbird.core.preference.PreferenceManager

const val KEY_USE_VOLUME_KEYS_FOR_NAVIGATION = "useVolumeKeysForNavigation"
const val KEY_MESSAGE_VIEW_POST_DELETE_ACTION = "messageViewPostDeleteAction"
const val KEY_SWIPE_ACTION_LEFT = "swipeLeftAction"
const val KEY_SWIPE_ACTION_RIGHT = "swipeRightAction"
const val KEY_CONFIRM_DELETE = "confirmDelete"
const val KEY_CONFIRM_DISCARD_MESSAGE = "confirmDiscardMessage"
const val KEY_CONFIRM_DELETE_STARRED = "confirmDeleteStarred"
const val KEY_CONFIRM_SPAM = "confirmSpam"
const val KEY_CONFIRM_DELETE_FROM_NOTIFICATION = "confirmDeleteFromNotification"
const val KEY_CONFIRM_MARK_ALL_READ = "confirmMarkAllRead"

interface InteractionSettingsPreferenceManager : PreferenceManager<InteractionSettings>
