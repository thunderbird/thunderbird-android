package net.thunderbird.feature.mail.message.list.internal.ui

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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonText
import app.k9mail.core.ui.compose.designsystem.molecule.PullToRefreshBox
import kotlinx.coroutines.flow.distinctUntilChanged
import net.thunderbird.feature.mail.message.list.internal.ui.component.MessageListItem
import net.thunderbird.feature.mail.message.list.preferences.MessageListPreferences
import net.thunderbird.feature.mail.message.list.ui.MessageListContract
import net.thunderbird.feature.mail.message.list.ui.effect.MessageListEffect
import net.thunderbird.feature.mail.message.list.ui.event.MessageItemEvent
import net.thunderbird.feature.mail.message.list.ui.event.MessageListEvent
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
        preferences: MessageListPreferences,
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
}

@Composable
private fun MessageList(
    state: MessageListState,
    dispatchEvent: (MessageListEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberMessageListLazyState(state, dispatchEvent)

    val showAccountIndicator = state.metadata.showAccountIndicator
    val preferences = requireNotNull(state.preferences)
    LazyColumn(modifier = modifier, state = listState) {
        items(
            items = state.messages,
            key = { message -> message.id },
        ) { message ->
            MessageListItem(
                message = message,
                showAccountIndicator = showAccountIndicator,
                preferences = preferences,
                modifier = Modifier.fillMaxWidth(),
                onClick = { dispatchEvent(MessageItemEvent.OnMessageClick(message)) },
                onLongClick = { dispatchEvent(MessageItemEvent.ToggleSelectMessages(message)) },
                onAvatarClick = { dispatchEvent(MessageItemEvent.ToggleSelectMessages(message)) },
                onFavouriteClick = { dispatchEvent(MessageItemEvent.ToggleFavourite(message)) },
            )
        }
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(FOOTER_HEIGHT.dp)
                    .animateItem(),
                contentAlignment = Alignment.Center,
            ) {
                ButtonText(
                    text = state.metadata.footer.text,
                    onClick = { dispatchEvent(MessageListEvent.OnFooterClick) },
                )
            }
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
                val prefetchTriggerIndex = total - 1 - latestPaging.prefetchDistance
                val nearEnd = total > 0 && lastVisible >= prefetchTriggerIndex

                if (nearEnd && latestPaging.phase != PaginationUi.Phase.Loading && !latestPaging.endReached) {
                    dispatchEvent(MessageListEvent.LoadNextPage)
                }
            }
    }
    return listState
}
