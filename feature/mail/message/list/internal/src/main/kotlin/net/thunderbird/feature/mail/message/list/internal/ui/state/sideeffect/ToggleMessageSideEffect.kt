package net.thunderbird.feature.mail.message.list.internal.ui.state.sideeffect

import kotlinx.coroutines.CoroutineScope
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.mail.message.list.ui.effect.MessageListEffect
import net.thunderbird.feature.mail.message.list.ui.event.MessageItemEvent
import net.thunderbird.feature.mail.message.list.ui.event.MessageListEvent
import net.thunderbird.feature.mail.message.list.ui.state.MessageListState
import net.thunderbird.feature.mail.message.list.ui.state.sideeffect.MessageListStateSideEffectHandler
import net.thunderbird.feature.mail.message.list.ui.state.sideeffect.MessageListStateSideEffectHandlerFactory

internal class ToggleMessageSideEffect(
    logger: Logger,
    dispatch: suspend (MessageListEvent) -> Unit,
) : MessageListStateSideEffectHandler(logger, dispatch) {

    override fun accept(event: MessageListEvent, oldState: MessageListState, newState: MessageListState): Boolean =
        event is MessageItemEvent.OnMessageClick && newState is MessageListState.SelectingMessages

    override suspend fun consume(
        event: MessageListEvent,
        oldState: MessageListState,
        newState: MessageListState,
    ): ConsumeResult {
        val event = event as? MessageItemEvent.OnMessageClick ?: return ConsumeResult.Ignored
        dispatch(MessageItemEvent.ToggleSelectMessages(event.message))
        return ConsumeResult.Consumed
    }

    class Factory(
        private val logger: Logger,
    ) : MessageListStateSideEffectHandlerFactory {
        override fun create(
            scope: CoroutineScope,
            dispatch: suspend (MessageListEvent) -> Unit,
            dispatchUiEffect: suspend (MessageListEffect) -> Unit,
        ): MessageListStateSideEffectHandler = ToggleMessageSideEffect(
            logger = logger,
            dispatch = dispatch,
        )
    }
}
