package net.thunderbird.feature.mail.message.list.internal.ui.state.sideeffect

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.mail.message.list.domain.DomainContract
import net.thunderbird.feature.mail.message.list.ui.event.MessageListEvent
import net.thunderbird.feature.mail.message.list.ui.state.MessageListState

private const val TAG = "LoadSwipeActionsSideEffectHandler"

class LoadSwipeActionsStateSideEffectHandler(
    private val scope: CoroutineScope,
    dispatch: suspend (MessageListEvent) -> Unit,
    private val logger: Logger,
    private val buildSwipeActions: DomainContract.UseCase.BuildSwipeActions,
) : StateSideEffectHandler(logger, dispatch) {
    var runningFlow: Job? = null
    override fun accept(event: MessageListEvent, newState: MessageListState): Boolean =
        event == MessageListEvent.LoadConfigurations

    override suspend fun handle(oldState: MessageListState, newState: MessageListState) {
        logger.verbose(TAG) { "$TAG.handle() called with: oldState = $oldState, newState = $newState" }
        runningFlow?.cancel()
        runningFlow = buildSwipeActions()
            .onEach { swipeActions ->
                dispatch(MessageListEvent.SwipeActionsLoaded(swipeActions))
            }
            .onCompletion { runningFlow = null }
            .launchIn(scope)
    }

    class Factory(
        private val logger: Logger,
        private val buildSwipeActions: DomainContract.UseCase.BuildSwipeActions,
    ) : StateSideEffectHandler.Factory {
        override fun create(
            scope: CoroutineScope,
            dispatch: suspend (MessageListEvent) -> Unit,
        ): StateSideEffectHandler = LoadSwipeActionsStateSideEffectHandler(
            dispatch = dispatch,
            scope = scope,
            logger = logger,
            buildSwipeActions = buildSwipeActions,
        )
    }
}
