package net.thunderbird.feature.mail.message.list.internal.ui

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import net.thunderbird.core.common.state.StateMachine
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.mail.message.list.internal.ui.state.machine.MessageListStateMachine
import net.thunderbird.feature.mail.message.list.ui.MessageListContract
import net.thunderbird.feature.mail.message.list.ui.MessageListStateSideEffectHandlerFactory
import net.thunderbird.feature.mail.message.list.ui.event.MessageListEvent
import net.thunderbird.feature.mail.message.list.ui.state.MessageListState

private const val TAG = "MessageListViewModel"

class MessageListViewModel(
    logger: Logger,
    messageListStateMachineFactory: MessageListStateMachine.Factory,
    stateSideEffectHandlersFactories: List<MessageListStateSideEffectHandlerFactory>,
) : MessageListContract.ViewModel(logger, stateSideEffectHandlersFactories) {
    override val stateMachine: StateMachine<MessageListState, MessageListEvent> = messageListStateMachineFactory
        .create(viewModelScope, ::event)

    init {
        logger.verbose(TAG) { "init() called" }
        state
            .onEach { state -> logger.verbose(TAG) { "state.onEach called with: state = $state" } }
            .launchIn(viewModelScope)
    }
}
