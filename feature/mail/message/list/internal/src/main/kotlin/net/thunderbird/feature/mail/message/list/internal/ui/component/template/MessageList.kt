package net.thunderbird.feature.mail.message.list.internal.ui.component.template

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.lifecycle.compose.LifecycleStartEffect
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import net.thunderbird.core.ui.compose.common.modifier.testTagAsResourceId
import net.thunderbird.feature.mail.message.list.internal.ui.component.MessageListItem
import net.thunderbird.feature.mail.message.list.internal.ui.component.organism.MessageListFooter
import net.thunderbird.feature.mail.message.list.internal.ui.component.organism.MessageListSwipeableItem
import net.thunderbird.feature.mail.message.list.ui.component.MessageListScope
import net.thunderbird.feature.mail.message.list.ui.component.ScrollEvent
import net.thunderbird.feature.mail.message.list.ui.event.MessageItemEvent
import net.thunderbird.feature.mail.message.list.ui.event.MessageListEvent
import net.thunderbird.feature.mail.message.list.ui.state.MessageItemUi
import net.thunderbird.feature.mail.message.list.ui.state.MessageListState
import net.thunderbird.feature.mail.message.list.ui.state.PaginationUi

const val TEST_TAG_MESSAGE_LIST_ROOT = "TestMessageList_Root"

@Composable
internal fun MessageListScope.MessageList(
    state: MessageListState,
    dispatchEvent: (MessageListEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberMessageListLazyState(state, dispatchEvent)

    val showAccountIndicator = state.metadata.showAccountIndicator
    val swipeActions = state.metadata.swipeActions

    ScrollEventEffect(state.messages, listState)

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
        item {
            MessageListFooter(state, dispatchEvent, Modifier.animateItem())
        }
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

private suspend fun LazyListState.scrollToMessage(
    messages: ImmutableList<MessageItemUi>,
    event: ScrollEvent.ScrollToMessage,
) {
    val (message, animated) = event
    val index = messages.indexOfFirst { message.id == it.id }.takeIf { it >= 0 } ?: return
    val firstVisible = layoutInfo.visibleItemsInfo.firstOrNull()?.index ?: 0
    val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
    if (index !in firstVisible..lastVisible) {
        if (animated) animateScrollToItem(index) else scrollToItem(index)
    }
}

@Composable
private fun MessageListScope.ScrollEventEffect(messages: ImmutableList<MessageItemUi>, listState: LazyListState) {
    val currentMessages by rememberUpdatedState(messages)

    val scope = rememberCoroutineScope()
    LifecycleStartEffect(scrollEvents, listState) {
        val job = scope.launch {
            scrollEvents.collect { event ->
                when (event) {
                    is ScrollEvent.ScrollToMessage -> listState.scrollToMessage(currentMessages, event)
                }
            }
        }
        onStopOrDispose {
            job.cancel()
        }
    }

    // For configuration restoration
    LaunchedEffect(listState) {
        val activeMessage = currentMessages.firstOrNull { it.active }
        if (activeMessage != null) {
            listState.scrollToMessage(
                currentMessages,
                ScrollEvent.ScrollToMessage(activeMessage, animated = false),
            )
        }
    }
}
