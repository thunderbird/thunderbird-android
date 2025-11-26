package net.thunderbird.core.preference.interaction

import net.thunderbird.core.common.action.SwipeAction
import net.thunderbird.core.common.action.SwipeActions

const val INTERACTION_SETTINGS_DEFAULT_USE_VOLUME_KEYS_NAVIGATION = false
val INTERACTION_SETTINGS_DEFAULT_MESSAGE_VIEW_POST_REMOVE_NAVIGATION = PostRemoveNavigation.ReturnToMessageList.name
val INTERACTION_SETTINGS_DEFAULT_SWIPE_ACTION = SwipeActions(
    leftAction = SwipeAction.ToggleRead,
    rightAction = SwipeAction.ToggleSelection,
)

data class InteractionSettings(
    val useVolumeKeysForNavigation: Boolean = INTERACTION_SETTINGS_DEFAULT_USE_VOLUME_KEYS_NAVIGATION,
    val messageViewPostRemoveNavigation: String = INTERACTION_SETTINGS_DEFAULT_MESSAGE_VIEW_POST_REMOVE_NAVIGATION,
    val swipeActions: SwipeActions = INTERACTION_SETTINGS_DEFAULT_SWIPE_ACTION,
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
