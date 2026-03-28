package net.thunderbird.feature.mail.message.list.internal.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import net.thunderbird.core.common.action.SwipeAction
import net.thunderbird.core.common.action.SwipeActions
import net.thunderbird.core.ui.compose.designsystem.molecule.swipe.SwipeDirectionAccessibilityAction
import net.thunderbird.core.ui.compose.designsystem.molecule.swipe.SwipeDirectionAccessibilityAction.StartToEndAccessibilityAction
import net.thunderbird.feature.mail.message.list.internal.R
import net.thunderbird.feature.mail.message.list.ui.state.MessageItemUi
import net.thunderbird.feature.mail.message.list.R as ApiR

/**
 * Manages accessibility state and descriptions for messages in a message list screen.
 *
 * This class provides accessibility support for message list items by managing state descriptions
 * and swipe gesture accessibility actions. It maps different message states (combining read status,
 * selection state, and active state) to human-readable descriptions that can be announced by
 * accessibility services.
 *
 * @param stateDescription A map that associates each [MessageListStateDescription] enum value
 *  with its corresponding localized accessibility description string.
 * @param swipeDirectionAccessibilityAction An immutable list of accessibility actions for
 *  swipe gestures that can be performed on message items. Defaults to an empty list if no
 *  swipe actions are available.
 */
@Stable
class MessageListScreenAccessibilityState(
    private val stateDescription: Map<MessageListStateDescription, String>,
    val swipeDirectionAccessibilityAction: ImmutableList<SwipeDirectionAccessibilityAction> = persistentListOf(),
) {
    /**
     * Retrieves the state description for accessibility purposes for the given message.
     *
     * @param message The message item for which to retrieve the state description.
     * @return A string containing the accessibility state description for the message.
     * @throws NoSuchElementException if the message does not have a corresponding state description.
     */
    @Throws(NoSuchElementException::class)
    fun stateDescription(message: MessageItemUi): String =
        stateDescription.getValue(message.toMessageStateDescription())

    private fun MessageItemUi.toMessageStateDescription(): MessageListStateDescription = when (state) {
        MessageItemUi.State.New if active -> MessageListStateDescription.ActiveNewMessage
        MessageItemUi.State.Read if active -> MessageListStateDescription.ActiveReadMessage
        MessageItemUi.State.Unread if active -> MessageListStateDescription.ActiveUnreadMessage
        MessageItemUi.State.New if selected -> MessageListStateDescription.SelectedNewMessage
        MessageItemUi.State.Read if selected -> MessageListStateDescription.SelectedReadMessage
        MessageItemUi.State.Unread if selected -> MessageListStateDescription.SelectedUnreadMessage
        MessageItemUi.State.New -> MessageListStateDescription.NewMessage
        MessageItemUi.State.Read -> MessageListStateDescription.ReadMessage
        MessageItemUi.State.Unread -> MessageListStateDescription.UnreadMessage
    }
}

/**
 * Describes the various states a message can be in within a message list, combining read status,
 * selection state, and active state.
 *
 * This enumeration represents all possible combinations of three orthogonal message properties:
 * - Read status: New (never opened), Read (opened), or Unread (marked as unread)
 * - Selection state: Not selected, Selected (marked for batch operations)
 * - Active state: Not active or Active (currently focused/displayed)
 *
 * The state descriptions are used to determine the appropriate visual presentation and
 * interaction behavior for messages in the message list UI.
 */
enum class MessageListStateDescription {
    NewMessage,
    ReadMessage,
    UnreadMessage,
    ActiveNewMessage,
    ActiveReadMessage,
    ActiveUnreadMessage,
    SelectedNewMessage,
    SelectedReadMessage,
    SelectedUnreadMessage,
}

/**
 * Creates and remembers a [MessageListScreenAccessibilityState] configured for the message list screen.
 *
 * When swipe actions are provided, they are converted into accessibility actions that can be
 * triggered programmatically, allowing users with accessibility needs to perform swipe-based
 * operations without physical gestures.
 *
 * @param swipeActions The swipe action configuration containing left and right swipe actions
 *  that should be made available as accessibility actions. If null or if individual actions are
 *  [SwipeAction.None], no corresponding accessibility actions will be created for those directions.
 * @return A remembered [MessageListScreenAccessibilityState] instance containing state descriptions
 *  and accessibility actions for swipe gestures. The instance is only recreated when the state
 *  description strings or swipe actions change.
 */
@Composable
fun rememberMessageListScreenAccessibilityState(swipeActions: SwipeActions?): MessageListScreenAccessibilityState {
    val stateDescription = mapOf(
        MessageListStateDescription.NewMessage to stringResource(
            id = R.string.message_list_state_new_message_description,
        ),
        MessageListStateDescription.ReadMessage to stringResource(
            id = R.string.message_list_state_read_message_description,
        ),
        MessageListStateDescription.UnreadMessage to stringResource(
            id = R.string.message_list_state_unread_message_description,
        ),
        MessageListStateDescription.ActiveNewMessage to stringResource(
            id = R.string.message_list_state_active_new_message_description,
        ),
        MessageListStateDescription.ActiveReadMessage to stringResource(
            id = R.string.message_list_state_active_read_message_description,
        ),
        MessageListStateDescription.ActiveUnreadMessage to stringResource(
            id = R.string.message_list_state_active_unread_message_description,
        ),
        MessageListStateDescription.SelectedNewMessage to stringResource(
            id = R.string.message_list_state_selected_new_message_description,
        ),
        MessageListStateDescription.SelectedReadMessage to stringResource(
            id = R.string.message_list_state_selected_read_message_description,
        ),
        MessageListStateDescription.SelectedUnreadMessage to stringResource(
            id = R.string.message_list_state_selected_unread_message_description,
        ),
    )
    val swipeDirectionAccessibilityAction = remember(swipeActions) {
        buildList {
            val rightAction = swipeActions?.rightAction?.toAccessibilityStringRes()
            if (rightAction != null) {
                add(StartToEndAccessibilityAction(actionStringRes = rightAction))
            }
            val leftAction = swipeActions?.leftAction?.toAccessibilityStringRes()
            if (leftAction != null) {
                add(StartToEndAccessibilityAction(actionStringRes = leftAction))
            }
        }
    }

    return remember(stateDescription, swipeDirectionAccessibilityAction) {
        MessageListScreenAccessibilityState(stateDescription, swipeDirectionAccessibilityAction.toPersistentList())
    }
}

private fun SwipeAction.toAccessibilityStringRes(): Int? = when (this) {
    SwipeAction.None -> null
    SwipeAction.ToggleSelection -> ApiR.string.swipe_action_select
    SwipeAction.ToggleRead -> ApiR.string.swipe_action_mark_as_read
    SwipeAction.ToggleStar -> ApiR.string.swipe_action_add_star
    SwipeAction.Archive -> ApiR.string.swipe_action_archive
    SwipeAction.ArchiveSetupArchiveFolder -> ApiR.string.swipe_action_archive_folder_not_set
    SwipeAction.ArchiveDisabled -> ApiR.string.swipe_action_change_swipe_gestures
    SwipeAction.Delete -> ApiR.string.swipe_action_delete
    SwipeAction.Spam -> ApiR.string.swipe_action_spam
    SwipeAction.Move -> ApiR.string.swipe_action_move
}
