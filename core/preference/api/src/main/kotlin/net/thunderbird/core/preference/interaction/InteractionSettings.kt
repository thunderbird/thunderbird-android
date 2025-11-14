package net.thunderbird.core.preference.interaction

const val INTERACTION_SETTINGS_DEFAULT_USE_VOLUME_KEYS_NAVIGATION = false
val INTERACTION_SETTINGS_DEFAULT_MESSAGE_VIEW_POST_REMOVE_NAVIGATION = PostRemoveNavigation.ReturnToMessageList.name

data class InteractionSettings(
    val useVolumeKeysForNavigation: Boolean = INTERACTION_SETTINGS_DEFAULT_USE_VOLUME_KEYS_NAVIGATION,
    val messageViewPostRemoveNavigation: String = INTERACTION_SETTINGS_DEFAULT_MESSAGE_VIEW_POST_REMOVE_NAVIGATION,
)

/**
 * The navigation actions that can be to performed after the user has deleted or moved a message from the message
 * view screen.
 */
enum class PostRemoveNavigation {
    ReturnToMessageList,
    ShowPreviousMessage,
    ShowNextMessage,
}
