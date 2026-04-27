package net.thunderbird.feature.mail.message.list.internal.ui

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import net.thunderbird.core.common.resources.StringsResourceManager
import net.thunderbird.core.common.state.StateMachine
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.mail.message.list.internal.ui.state.machine.MessageListStateMachine
import net.thunderbird.feature.mail.message.list.ui.MessageListContract
import net.thunderbird.feature.mail.message.list.ui.effect.MessageListEffect
import net.thunderbird.feature.mail.message.list.ui.event.MessageListEvent
import net.thunderbird.feature.mail.message.list.ui.state.MessageListState
import net.thunderbird.feature.mail.message.list.ui.state.sideeffect.MessageListStateSideEffectHandlerFactory
import net.thunderbird.feature.mail.message.list.R as MessageListApiR

private const val TAG = "MessageListViewModel"

internal class MessageListViewModel(
    logger: Logger,
    messageListStateMachineFactory: MessageListStateMachine.Factory,
    stateSideEffectHandlersFactories: List<MessageListStateSideEffectHandlerFactory>,
    stringsResourceManager: StringsResourceManager,
) : MessageListContract.ViewModel(logger, stateSideEffectHandlersFactories) {
    override val stateMachine: StateMachine<MessageListState, MessageListEvent> = messageListStateMachineFactory
        .create(scope = viewModelScope, dispatch = ::event, dispatchUiEffect = ::emitEffect)

    init {
        logger.verbose(TAG) { "init() called" }
        state
            .onEach { state ->
                logger.verbose(TAG) { "state.onEach called with: state = $state" }
                when (state) {
                    is MessageListState.SelectingMessages -> {
                        val selectedCount = state.messages.count { it.selected }
                        emitEffect(
                            if (selectedCount > 0) {
                                MessageListEffect.UpdateToolbarActionMode(
                                    title = stringsResourceManager.stringResource(
                                        MessageListApiR.string.actionbar_selected,
                                        selectedCount,
                                    ),
                                    isAllSelected = selectedCount == state.messages.size,
                                )
                            } else {
                                MessageListEffect.ResetToolbarActionMode
                            },
                        )
                    }

                    else -> Unit
                }
            }
            .launchIn(viewModelScope)
    }

    override fun onEventWithoutStateModification(event: MessageListEvent, currentState: MessageListState) {
        when (event) {
            is MessageListEvent.OnFooterClick -> emitEffect(MessageListEffect.TriggerOnFooterClicked)
            else -> Unit
        }
    }
}
