package net.thunderbird.feature.mail.message.list.internal.ui

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.mail.message.list.domain.DomainContract
import net.thunderbird.feature.mail.message.list.internal.ui.state.machine.MessageListStateMachine
import net.thunderbird.feature.mail.message.list.ui.MessageListContract
import net.thunderbird.feature.mail.message.list.ui.event.MessageListEvent
import net.thunderbird.feature.mail.message.list.ui.state.MessageListState

private const val TAG = "MessageListViewModel"

class MessageListViewModel(
    private val logger: Logger,
    messageListStateMachineFactory: MessageListStateMachine.Factory,
    getMessageListPreferences: DomainContract.UseCase.GetMessageListPreferences,
) : MessageListContract.ViewModel() {
    private val stateMachine = messageListStateMachineFactory.create(dispatch = ::event).also {
        it.currentState.onEach { state ->
            logger.debug(TAG) { "stateMachine.currentState() called with: state = $state" }
        }.launchIn(viewModelScope)
    }
    override val state: StateFlow<MessageListState> = stateMachine.currentState

    init {
        logger.debug(TAG) { "init() called" }
        getMessageListPreferences()
            .onEach { preferences ->
                logger.verbose(TAG) { "getMessageListPreferences() called with: preferences = $preferences" }
                event(MessageListEvent.UpdatePreferences(preferences))
            }
            .launchIn(viewModelScope)
    }

    override fun updateState(update: (MessageListState) -> MessageListState) {
        error("updateState() is not supported by this ViewModel. The state must be updated using the state machine.")
    }

    override fun event(event: MessageListEvent) {
        logger.verbose(TAG) { "event() called with: event = $event" }
        viewModelScope.launch {
            stateMachine.onEvent(event)
        }
    }
}
