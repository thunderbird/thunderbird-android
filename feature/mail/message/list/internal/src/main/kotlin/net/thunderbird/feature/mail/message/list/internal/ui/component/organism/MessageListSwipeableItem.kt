package net.thunderbird.feature.mail.message.list.internal.ui.component.organism

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import net.thunderbird.core.common.action.SwipeAction
import net.thunderbird.core.common.action.SwipeActions
import net.thunderbird.core.ui.compose.designsystem.molecule.swipe.SwipeBehaviour
import net.thunderbird.core.ui.compose.designsystem.molecule.swipe.SwipeDirection
import net.thunderbird.core.ui.compose.designsystem.molecule.swipe.SwipeableRow
import net.thunderbird.core.ui.compose.designsystem.molecule.swipe.SwipeableRowState
import net.thunderbird.core.ui.compose.designsystem.molecule.swipe.rememberSwipeableRowState
import net.thunderbird.feature.mail.message.list.internal.ui.MessageListScreenAccessibilityState
import net.thunderbird.feature.mail.message.list.internal.ui.component.MessageItemSwipeBackground
import net.thunderbird.feature.mail.message.list.internal.ui.rememberMessageListScreenAccessibilityState
import net.thunderbird.feature.mail.message.list.ui.event.MessageItemEvent
import net.thunderbird.feature.mail.message.list.ui.state.MessageItemUi

@Composable
internal fun MessageListSwipeableItem(
    message: MessageItemUi,
    messageSwipeActions: SwipeActions?,
    dispatchEvent: (MessageItemEvent) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (MessageListScreenAccessibilityState) -> Unit,
) {
    val accessibilityState = rememberMessageListScreenAccessibilityState(messageSwipeActions)
    val state = rememberMessageListSwipeableRowState(
        accessibilityState = accessibilityState,
        startToEndAction = messageSwipeActions?.rightAction ?: SwipeAction.None,
        endToStartAction = messageSwipeActions?.leftAction ?: SwipeAction.None,
    )
    SwipeableRow(
        state = state,
        backgroundContent = {
            messageSwipeActions?.let { swipeActions ->
                val (swipeAction, arrangement) = when (state.swipeDirection) {
                    SwipeDirection.StartToEnd -> swipeActions.rightAction to Arrangement.Start
                    SwipeDirection.EndToStart -> swipeActions.leftAction to Arrangement.End
                    SwipeDirection.Settled -> return@let
                }
                MessageItemSwipeBackground(
                    action = swipeAction,
                    toggled = false,
                    arrangement = arrangement,
                )
            }
        },
        gesturesEnabled = messageSwipeActions != null,
        onSwipeEnd = { direction ->
            messageSwipeActions
                ?.let { swipeActions ->
                    when (direction) {
                        SwipeDirection.StartToEnd -> swipeActions.rightAction
                        SwipeDirection.EndToStart -> swipeActions.leftAction
                        else -> null
                    }
                }
                ?.let { dispatchEvent(MessageItemEvent.OnSwipeMessage(message, swipeAction = it)) }
        },
        modifier = modifier,
    ) {
        content(accessibilityState)
    }
}

@Composable
private fun rememberMessageListSwipeableRowState(
    accessibilityState: MessageListScreenAccessibilityState,
    startToEndAction: SwipeAction,
    endToStartAction: SwipeAction,
): SwipeableRowState = rememberSwipeableRowState(
    startToEndBehaviour = remember(startToEndAction) { startToEndAction.behaviour },
    endToStartBehaviour = remember(endToStartAction) { endToStartAction.behaviour },
    accessibilityActions = accessibilityState.swipeDirectionAccessibilityAction,
)

private val SwipeAction.behaviour: SwipeBehaviour
    get() = when (this) {
        SwipeAction.None, SwipeAction.ArchiveDisabled -> SwipeBehaviour.Disabled

        SwipeAction.ToggleSelection,
        SwipeAction.ToggleRead,
        SwipeAction.ToggleStar,
        SwipeAction.ArchiveSetupArchiveFolder,
        -> SwipeBehaviour.Action()

        SwipeAction.Archive,
        SwipeAction.Delete,
        SwipeAction.Spam,
        SwipeAction.Move,
        -> SwipeBehaviour.Dismiss()
    }
