package net.thunderbird.core.preference.interaction

import net.thunderbird.core.preference.PreferenceManager

const val KEY_USE_VOLUME_KEYS_FOR_NAVIGATION = "useVolumeKeysForNavigation"
const val KEY_MESSAGE_VIEW_POST_DELETE_ACTION = "messageViewPostDeleteAction"
const val KEY_SWIPE_ACTION_LEFT = "swipeLeftAction"
const val KEY_SWIPE_ACTION_RIGHT = "swipeRightAction"

interface InteractionSettingsPreferenceManager : PreferenceManager<InteractionSettings>
