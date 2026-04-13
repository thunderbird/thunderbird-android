package net.thunderbird.feature.mail.message.list.internal.ui.state.sideeffect.ui

import kotlinx.coroutines.CoroutineScope
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.mail.message.list.ui.effect.MessageListEffect
import net.thunderbird.feature.mail.message.list.ui.event.MessageItemEvent
import net.thunderbird.feature.mail.message.list.ui.event.MessageListEvent
import net.thunderbird.feature.mail.message.list.ui.state.MessageListState
import net.thunderbird.feature.mail.message.list.ui.state.sideeffect.MessageListStateSideEffectHandler
import net.thunderbird.feature.mail.message.list.ui.state.sideeffect.MessageListStateSideEffectHandlerFactory

internal class OpenMessageSideEffect(
    private val logger: Logger,
    dispatch: suspend (MessageListEvent) -> Unit,
    dispatchUiEffect: suspend (MessageListEffect) -> Unit,
) : MessageListStateSideEffectHandler(logger, dispatch, dispatchUiEffect) {
    override fun accept(event: MessageListEvent, oldState: MessageListState, newState: MessageListState): Boolean =
        event is MessageItemEvent.OnMessageClick &&
            oldState is MessageListState.LoadedMessages &&
            newState is MessageListState.LoadedMessages

    override suspend fun consume(
        event: MessageListEvent,
        oldState: MessageListState,
        newState: MessageListState,
    ): ConsumeResult {
        val activeMessage = requireNotNull(newState.metadata.activeMessage) {
            "onClickMessageSideEffect: activeMessage must not be null"
        }
        dispatchUiEffect(MessageListEffect.OpenMessage(message = activeMessage))
        return ConsumeResult.Consumed
    }

    class Factory(
        private val logger: Logger,
    ) : MessageListStateSideEffectHandlerFactory {
        override fun create(
            scope: CoroutineScope,
            dispatch: suspend (MessageListEvent) -> Unit,
            dispatchUiEffect: suspend (MessageListEffect) -> Unit,
        ): MessageListStateSideEffectHandler = OpenMessageSideEffect(
            logger = logger,
            dispatch = dispatch,
            dispatchUiEffect = dispatchUiEffect,
        )
    }
}
