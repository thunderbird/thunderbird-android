package net.thunderbird.core.preference.interaction

const val INTERACTION_SETTINGS_DEFAULT_USE_VOLUME_KEYS_NAVIGATION = false

data class InteractionSettings(
    val useVolumeKeysForNavigation: Boolean = INTERACTION_SETTINGS_DEFAULT_USE_VOLUME_KEYS_NAVIGATION,
)
