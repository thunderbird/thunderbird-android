package net.thunderbird.core.preference.interaction

import net.thunderbird.core.common.action.SwipeAction
import net.thunderbird.core.common.action.SwipeActions

const val INTERACTION_SETTINGS_DEFAULT_USE_VOLUME_KEYS_NAVIGATION = false
val INTERACTION_SETTINGS_DEFAULT_MESSAGE_VIEW_POST_REMOVE_NAVIGATION = PostRemoveNavigation.ReturnToMessageList.name
val INTERACTION_SETTINGS_DEFAULT_SWIPE_ACTION = SwipeActions(
    leftAction = SwipeAction.ToggleRead,
    rightAction = SwipeAction.ToggleSelection,
)
const val INTERACTION_SETTINGS_DEFAULT_CONFIRM_DELETE = false
const val INTERACTION_SETTINGS_DEFAULT_CONFIRM_DELETE_STARRED = false
const val INTERACTION_SETTINGS_DEFAULT_CONFIRM_DELETE_FROM_NOTIFICATION = true
const val INTERACTION_SETTINGS_DEFAULT_CONFIRM_SPAM = false
const val INTERACTION_SETTINGS_DEFAULT_CONFIRM_DISCARD_MESSAGE = true
const val INTERACTION_SETTINGS_DEFAULT_CONFIRM_MARK_ALL_READ = true

data class InteractionSettings(
    val useVolumeKeysForNavigation: Boolean = INTERACTION_SETTINGS_DEFAULT_USE_VOLUME_KEYS_NAVIGATION,
    val messageViewPostRemoveNavigation: String = INTERACTION_SETTINGS_DEFAULT_MESSAGE_VIEW_POST_REMOVE_NAVIGATION,
    val swipeActions: SwipeActions = INTERACTION_SETTINGS_DEFAULT_SWIPE_ACTION,
    val isConfirmDelete: Boolean = INTERACTION_SETTINGS_DEFAULT_CONFIRM_DELETE,
    val isConfirmDeleteStarred: Boolean = INTERACTION_SETTINGS_DEFAULT_CONFIRM_DELETE_STARRED,
    val isConfirmDeleteFromNotification: Boolean = INTERACTION_SETTINGS_DEFAULT_CONFIRM_DELETE_FROM_NOTIFICATION,
    val isConfirmSpam: Boolean = INTERACTION_SETTINGS_DEFAULT_CONFIRM_SPAM,
    val isConfirmDiscardMessage: Boolean = INTERACTION_SETTINGS_DEFAULT_CONFIRM_DISCARD_MESSAGE,
    val isConfirmMarkAllRead: Boolean = INTERACTION_SETTINGS_DEFAULT_CONFIRM_MARK_ALL_READ,
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
