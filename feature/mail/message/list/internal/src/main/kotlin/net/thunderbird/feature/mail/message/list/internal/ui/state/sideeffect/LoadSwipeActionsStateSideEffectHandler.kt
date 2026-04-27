package net.thunderbird.feature.mail.message.list.internal.ui.state.sideeffect

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.mail.message.list.domain.DomainContract
import net.thunderbird.feature.mail.message.list.ui.effect.MessageListEffect
import net.thunderbird.feature.mail.message.list.ui.event.MessageListEvent
import net.thunderbird.feature.mail.message.list.ui.state.MessageListState
import net.thunderbird.feature.mail.message.list.ui.state.sideeffect.MessageListStateSideEffectHandler
import net.thunderbird.feature.mail.message.list.ui.state.sideeffect.MessageListStateSideEffectHandlerFactory

private const val TAG = "LoadSwipeActionsSideEffectHandler"

internal class LoadSwipeActionsStateSideEffectHandler(
    private val scope: CoroutineScope,
    dispatch: suspend (MessageListEvent) -> Unit,
    private val logger: Logger,
    private val buildSwipeActions: DomainContract.UseCase.BuildSwipeActions,
) : MessageListStateSideEffectHandler(logger, dispatch) {
    private var runningFlow: Job? = null
    override fun accept(event: MessageListEvent, oldState: MessageListState, newState: MessageListState): Boolean =
        event == MessageListEvent.LoadConfigurations

    override suspend fun consume(
        event: MessageListEvent,
        oldState: MessageListState,
        newState: MessageListState,
    ): ConsumeResult {
        logger.verbose(TAG) { "$TAG.handle() called with: oldState = $oldState, newState = $newState" }
        runningFlow?.cancel()
        runningFlow = buildSwipeActions()
            .onEach { swipeActions ->
                dispatch(MessageListEvent.SwipeActionsLoaded(swipeActions))
            }
            .onCompletion { runningFlow = null }
            .launchIn(scope)

        return ConsumeResult.Consumed
    }

    class Factory(
        private val logger: Logger,
        private val buildSwipeActions: DomainContract.UseCase.BuildSwipeActions,
    ) : MessageListStateSideEffectHandlerFactory {
        override fun create(
            scope: CoroutineScope,
            dispatch: suspend (MessageListEvent) -> Unit,
            dispatchUiEffect: suspend (MessageListEffect) -> Unit,
        ): MessageListStateSideEffectHandler = LoadSwipeActionsStateSideEffectHandler(
            dispatch = dispatch,
            scope = scope,
            logger = logger,
            buildSwipeActions = buildSwipeActions,
        )
    }
}
