package net.thunderbird.feature.mail.message.list.internal.ui.component.page

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.k9mail.core.ui.compose.designsystem.molecule.PullToRefreshBox
import net.thunderbird.feature.mail.message.list.internal.ui.component.template.MessageList
import net.thunderbird.feature.mail.message.list.ui.event.MessageListEvent
import net.thunderbird.feature.mail.message.list.ui.state.MessageListState
import net.thunderbird.feature.notification.api.content.InAppNotification
import net.thunderbird.feature.notification.api.ui.InAppNotificationScaffold

@Composable
internal fun MessageListPage(
    inAppNotificationEventFilter: (InAppNotification) -> Boolean,
    state: MessageListState,
    dispatchEvent: (MessageListEvent) -> Unit,
    modifier: Modifier = Modifier,
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
