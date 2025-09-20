package net.thunderbird.core.preference.interaction

import net.thunderbird.core.preference.PreferenceManager

const val KEY_USE_VOLUME_KEYS_FOR_NAVIGATION = "useVolumeKeysForNavigation"
const val KEY_MESSAGE_VIEW_POST_DELETE_ACTION = "messageViewPostDeleteAction"
interface InteractionSettingsPreferenceManager : PreferenceManager<InteractionSettings>
