package net.thunderbird.feature.mail.message.list.ui

import app.k9mail.core.ui.compose.common.mvi.BaseViewModel
import net.thunderbird.feature.mail.message.list.ui.effect.MessageListEffect
import net.thunderbird.feature.mail.message.list.ui.event.MessageListEvent
import net.thunderbird.feature.mail.message.list.ui.state.MessageListState

interface MessageListContract {
    abstract class ViewModel : BaseViewModel<MessageListState, MessageListEvent, MessageListEffect>(
        initialState = MessageListState.WarmingUp(),
    )
}
