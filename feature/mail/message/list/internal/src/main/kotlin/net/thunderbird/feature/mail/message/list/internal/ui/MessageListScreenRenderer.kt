package net.thunderbird.feature.mail.message.list.internal.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.thunderbird.feature.mail.message.list.internal.ui.component.page.MessageListPage
import net.thunderbird.feature.mail.message.list.ui.MessageListContract
import net.thunderbird.feature.mail.message.list.ui.component.MessageListScope
import net.thunderbird.feature.mail.message.list.ui.event.MessageListEvent
import net.thunderbird.feature.mail.message.list.ui.state.MessageListState
import net.thunderbird.feature.notification.api.content.InAppNotification

internal class MessageListScreenRenderer : MessageListContract.MessageListScreenRenderer {
    @Composable
    override fun MessageListScope.Render(
        state: MessageListState,
        dispatchEvent: (MessageListEvent) -> Unit,
        modifier: Modifier,
        inAppNotificationEventFilter: (InAppNotification) -> Boolean,
    ) {
        MessageListPage(
            inAppNotificationEventFilter = inAppNotificationEventFilter,
            state = state,
            dispatchEvent = dispatchEvent,
            modifier = modifier,
        )
    }
}
