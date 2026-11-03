package net.thunderbird.core.preference.interaction

import net.thunderbird.core.common.action.SwipeAction
import net.thunderbird.core.common.action.SwipeActions

const val INTERACTION_SETTINGS_DEFAULT_USE_VOLUME_KEYS_NAVIGATION = false
const val INTERACTION_SETTINGS_DEFAULT_KEYPAD_NAVIGATION_ENABLED = false
val INTERACTION_SETTINGS_DEFAULT_MESSAGE_VIEW_POST_REMOVE_NAVIGATION = PostRemoveNavigation.ReturnToMessageList.name
val INTERACTION_SETTINGS_DEFAULT_MESSAGE_VIEW_POST_MARK_AS_UNREAD_NAVIGATION =
    PostMarkAsUnreadNavigation.ReturnToMessageList
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
    val isKeypadNavigationEnabled: Boolean = INTERACTION_SETTINGS_DEFAULT_KEYPAD_NAVIGATION_ENABLED,
    val keypadMessageActions: Map<Int, KeypadMessageAction> = emptyMap(),
    val messageViewPostRemoveNavigation: String = INTERACTION_SETTINGS_DEFAULT_MESSAGE_VIEW_POST_REMOVE_NAVIGATION,
    val messageViewPostMarkAsUnreadNavigation: PostMarkAsUnreadNavigation =
        INTERACTION_SETTINGS_DEFAULT_MESSAGE_VIEW_POST_MARK_AS_UNREAD_NAVIGATION,
    val swipeActions: SwipeActions = INTERACTION_SETTINGS_DEFAULT_SWIPE_ACTION,
    val isConfirmDelete: Boolean = INTERACTION_SETTINGS_DEFAULT_CONFIRM_DELETE,
    val isConfirmDeleteStarred: Boolean = INTERACTION_SETTINGS_DEFAULT_CONFIRM_DELETE_STARRED,
    val isConfirmDeleteFromNotification: Boolean = INTERACTION_SETTINGS_DEFAULT_CONFIRM_DELETE_FROM_NOTIFICATION,
    val isConfirmSpam: Boolean = INTERACTION_SETTINGS_DEFAULT_CONFIRM_SPAM,
    val isConfirmDiscardMessage: Boolean = INTERACTION_SETTINGS_DEFAULT_CONFIRM_DISCARD_MESSAGE,
    val isConfirmMarkAllRead: Boolean = INTERACTION_SETTINGS_DEFAULT_CONFIRM_MARK_ALL_READ,
)

/** Optional message actions assigned to a hardware keypad's number keys. */
enum class KeypadMessageAction {
    NONE,
    DELETE,
    ARCHIVE,
    TOGGLE_FLAGGED,
}

/**
 * The navigation actions that can be to performed after the user has deleted or moved a message from the message
 * view screen.
 */
enum class PostRemoveNavigation {
    ReturnToMessageList,
    ShowPreviousMessage,
    ShowNextMessage,
}

/**
 * The navigation actions that can be to performed after the user has marked a
 * message as unread from the message view screen.
 */
enum class PostMarkAsUnreadNavigation {
    StayOnCurrentMessage,
    ReturnToMessageList,
}
