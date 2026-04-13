package net.thunderbird.feature.mail.message.list.internal.ui.state.sideeffect

import kotlinx.coroutines.CoroutineScope
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.mail.message.list.ui.effect.MessageListEffect
import net.thunderbird.feature.mail.message.list.ui.event.MessageListEvent
import net.thunderbird.feature.mail.message.list.ui.state.MessageListState
import net.thunderbird.feature.mail.message.list.ui.state.sideeffect.MessageListStateSideEffectHandler
import net.thunderbird.feature.mail.message.list.ui.state.sideeffect.MessageListStateSideEffectHandlerFactory

/**
 * A side effect handler that monitors the message list state and dispatches an event when all
 * initial configurations have been loaded and the system is ready to proceed.
 *
 * @param logger A logger instance for tracking and debugging side effect execution.
 * @param dispatch A suspend function that dispatches [MessageListEvent] instances to the state machine.
 */
internal class AllConfigurationsReadySideEffect(
    logger: Logger,
    dispatch: suspend (MessageListEvent) -> Unit,
) : MessageListStateSideEffectHandler(logger, dispatch) {
    override fun accept(event: MessageListEvent, oldState: MessageListState, newState: MessageListState): Boolean =
        oldState != newState && newState is MessageListState.WarmingUp && newState.isReady

    override suspend fun consume(
        event: MessageListEvent,
        oldState: MessageListState,
        newState: MessageListState,
    ): ConsumeResult {
        dispatch(MessageListEvent.AllConfigsReady)
        return ConsumeResult.Consumed
    }

    class Factory(
        private val logger: Logger,
    ) : MessageListStateSideEffectHandlerFactory {
        override fun create(
            scope: CoroutineScope,
            dispatch: suspend (MessageListEvent) -> Unit,
            dispatchUiEffect: suspend (MessageListEffect) -> Unit,
        ): MessageListStateSideEffectHandler = AllConfigurationsReadySideEffect(
            dispatch = dispatch,
            logger = logger,
        )
    }
}
