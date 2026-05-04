package net.thunderbird.feature.mail.message.list.internal.ui.state.sideeffect.ui

import kotlinx.coroutines.CoroutineScope
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.mail.message.list.ui.effect.MessageListEffect
import net.thunderbird.feature.mail.message.list.ui.event.MessageItemEvent
import net.thunderbird.feature.mail.message.list.ui.event.MessageListEvent
import net.thunderbird.feature.mail.message.list.ui.state.MessageListState
import net.thunderbird.feature.mail.message.list.ui.state.sideeffect.MessageListStateSideEffectHandler
import net.thunderbird.feature.mail.message.list.ui.state.sideeffect.MessageListStateSideEffectHandlerFactory

internal class SetMessageActiveSideEffect(
    private val logger: Logger,
    dispatch: suspend (MessageListEvent) -> Unit,
    dispatchUiEffect: suspend (MessageListEffect) -> Unit,
) : MessageListStateSideEffectHandler(logger, dispatch, dispatchUiEffect) {
    override fun accept(event: MessageListEvent, oldState: MessageListState, newState: MessageListState): Boolean =
        event is MessageItemEvent.SetMessageActive && newState is MessageListState.LoadedMessages

    override suspend fun consume(
        event: MessageListEvent,
        oldState: MessageListState,
        newState: MessageListState,
    ): ConsumeResult {
        val activeMessage = newState.metadata.activeMessage
        return if (activeMessage != null) {
            dispatchUiEffect(MessageListEffect.ScrollToMessage(message = activeMessage))
            ConsumeResult.Consumed
        } else {
            ConsumeResult.Ignored
        }
    }

    class Factory(
        private val logger: Logger,
    ) : MessageListStateSideEffectHandlerFactory {
        override fun create(
            scope: CoroutineScope,
            dispatch: suspend (MessageListEvent) -> Unit,
            dispatchUiEffect: suspend (MessageListEffect) -> Unit,
        ): MessageListStateSideEffectHandler = SetMessageActiveSideEffect(
            logger = logger,
            dispatch = dispatch,
            dispatchUiEffect = dispatchUiEffect,
        )
    }
}
