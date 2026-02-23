package net.thunderbird.feature.mail.message.list.internal.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.k9mail.core.ui.compose.designsystem.atom.button.ButtonText
import app.k9mail.core.ui.compose.designsystem.molecule.PullToRefreshBox
import net.thunderbird.feature.mail.message.list.internal.ui.component.MessageListItem
import net.thunderbird.feature.mail.message.list.preferences.MessageListPreferences
import net.thunderbird.feature.mail.message.list.ui.MessageListContract
import net.thunderbird.feature.mail.message.list.ui.effect.MessageListEffect
import net.thunderbird.feature.mail.message.list.ui.event.MessageItemEvent
import net.thunderbird.feature.mail.message.list.ui.event.MessageListEvent
import net.thunderbird.feature.mail.message.list.ui.state.MessageListState
import net.thunderbird.feature.notification.api.content.InAppNotification
import net.thunderbird.feature.notification.api.ui.InAppNotificationScaffold

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
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(
                        items = state.messages,
                        key = { message -> message.id },
                    ) { message ->
                        MessageListItem(
                            message = message,
                            showAccountIndicator = state.metadata.showAccountIndicator,
                            preferences = requireNotNull(state.preferences),
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
                                .height(64.dp)
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
        }
    }
}
