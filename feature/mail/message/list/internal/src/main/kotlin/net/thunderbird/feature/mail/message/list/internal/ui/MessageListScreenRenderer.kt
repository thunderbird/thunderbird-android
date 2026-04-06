package net.thunderbird.feature.mail.message.list.internal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonText
import app.k9mail.core.ui.compose.designsystem.molecule.PullToRefreshBox
import kotlinx.coroutines.flow.distinctUntilChanged
import net.thunderbird.core.common.action.SwipeAction
import net.thunderbird.core.common.action.SwipeActions
import net.thunderbird.core.ui.compose.common.modifier.testTagAsResourceId
import net.thunderbird.core.ui.compose.designsystem.molecule.swipe.SwipeBehaviour
import net.thunderbird.core.ui.compose.designsystem.molecule.swipe.SwipeDirection
import net.thunderbird.core.ui.compose.designsystem.molecule.swipe.SwipeableRow
import net.thunderbird.core.ui.compose.designsystem.molecule.swipe.SwipeableRowState
import net.thunderbird.core.ui.compose.designsystem.molecule.swipe.rememberSwipeableRowState
import net.thunderbird.feature.mail.message.list.internal.ui.MessageListScreenRenderer.Companion.TEST_TAG_MESSAGE_LIST_ROOT
import net.thunderbird.feature.mail.message.list.internal.ui.component.MessageItemSwipeBackground
import net.thunderbird.feature.mail.message.list.internal.ui.component.MessageListItem
import net.thunderbird.feature.mail.message.list.ui.MessageListContract
import net.thunderbird.feature.mail.message.list.ui.effect.MessageListEffect
import net.thunderbird.feature.mail.message.list.ui.event.MessageItemEvent
import net.thunderbird.feature.mail.message.list.ui.event.MessageListEvent
import net.thunderbird.feature.mail.message.list.ui.state.MessageItemUi
import net.thunderbird.feature.mail.message.list.ui.state.MessageListState
import net.thunderbird.feature.mail.message.list.ui.state.PaginationUi
import net.thunderbird.feature.notification.api.content.InAppNotification
import net.thunderbird.feature.notification.api.ui.InAppNotificationScaffold

private const val FOOTER_HEIGHT = 64

internal class MessageListScreenRenderer : MessageListContract.MessageListScreenRenderer {
    @Composable
    override fun Render(
        state: MessageListState,
        dispatchEvent: (MessageListEvent) -> Unit,
        onEffect: (MessageListEffect) -> Unit,
        modifier: Modifier,
        inAppNotificationEventFilter: (InAppNotification) -> Boolean,
    ) {
        InAppNotificationScaffold(
            eventFilter = inAppNotificationEventFilter,
            modifier = modifier,
        ) { paddingValues ->
            PullToRefreshBox(
                isRefreshing = (state as? MessageListState.LoadingMessages)?.isPullToRefresh == true,
                onRefresh = { dispatchEvent(MessageListEvent.Refresh) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            ) {
                MessageList(state = state, dispatchEvent = dispatchEvent, modifier = Modifier.fillMaxSize())
            }
        }
    }

    internal companion object {
        const val TEST_TAG_MESSAGE_LIST_ROOT = "TestMessageList_Root"
    }
}

@Composable
private fun MessageList(
    state: MessageListState,
    dispatchEvent: (MessageListEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberMessageListLazyState(state, dispatchEvent)

    val showAccountIndicator = state.metadata.showAccountIndicator
    val swipeActions = state.metadata.swipeActions
    LazyColumn(
        modifier = modifier.testTagAsResourceId(TEST_TAG_MESSAGE_LIST_ROOT),
        state = listState,
    ) {
        items(
            items = state.messages,
            key = { message -> message.id },
        ) { message ->
            val messageSwipeActions = swipeActions[message.account.id]
            val preferences = state.preferences ?: return@items
            MessageListSwipeableItem(message, messageSwipeActions, dispatchEvent) { accessibilityState ->
                MessageListItem(
                    message = message,
                    showAccountIndicator = showAccountIndicator,
                    preferences = preferences,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics(mergeDescendants = true) {
                            stateDescription = accessibilityState.stateDescription(message)
                        },
                    onClick = { dispatchEvent(MessageItemEvent.OnMessageClick(message)) },
                    onLongClick = { dispatchEvent(MessageItemEvent.ToggleSelectMessages(message)) },
                    onAvatarClick = { dispatchEvent(MessageItemEvent.ToggleSelectMessages(message)) },
                    onFavouriteClick = { dispatchEvent(MessageItemEvent.ToggleFavourite(message)) },
                )
            }
        }
        item { MessageListFooter(state, dispatchEvent, Modifier.animateItem()) }
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

@Composable
private fun MessageListSwipeableItem(
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
private fun MessageListFooter(
    state: MessageListState,
    dispatchEvent: (MessageListEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(FOOTER_HEIGHT.dp),
        contentAlignment = Alignment.Center,
    ) {
        ButtonText(
            text = state.metadata.footer.text,
            onClick = { dispatchEvent(MessageListEvent.OnFooterClick) },
        )
    }
}

@Composable
private fun rememberMessageListLazyState(
    state: MessageListState,
    dispatchEvent: (MessageListEvent) -> Unit,
): LazyListState {
    val listState = rememberLazyListState()
    val latestPaging by rememberUpdatedState(state.metadata.paging)

    LaunchedEffect(listState, state.metadata.folder?.id) {
        snapshotFlow {
            val total = listState.layoutInfo.totalItemsCount
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible to total
        }
            .distinctUntilChanged()
            .collect { (lastVisible, total) ->
                val hasNextPage = total > 0 && lastVisible >= total - 1 && total > latestPaging.prefetchDistance
                if (!hasNextPage) return@collect
                val prefetchTriggerIndex = (total - 1 - latestPaging.prefetchDistance).coerceAtLeast(0)
                val nearEnd = lastVisible >= prefetchTriggerIndex

                if (nearEnd &&
                    latestPaging.phase != PaginationUi.Phase.Loading &&
                    !latestPaging.endReached
                ) {
                    dispatchEvent(MessageListEvent.LoadNextPage)
                }
            }
    }
    return listState
}
