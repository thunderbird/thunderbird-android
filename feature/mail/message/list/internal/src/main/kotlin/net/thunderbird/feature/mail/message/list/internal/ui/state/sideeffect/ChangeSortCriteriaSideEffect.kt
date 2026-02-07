package net.thunderbird.feature.mail.message.list.internal.ui.state.sideeffect

import kotlinx.coroutines.CoroutineScope
import net.thunderbird.core.common.state.sideeffect.StateSideEffectHandler
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.mail.message.list.domain.DomainContract
import net.thunderbird.feature.mail.message.list.ui.MessageListStateSideEffectHandlerFactory
import net.thunderbird.feature.mail.message.list.ui.event.MessageListEvent
import net.thunderbird.feature.mail.message.list.ui.state.MessageListState

private const val TAG = "ChangeSortCriteriaSideEffect"

class ChangeSortCriteriaSideEffect(
    dispatch: suspend (MessageListEvent) -> Unit,
    private val logger: Logger,
    private val updateSortCriteria: DomainContract.UseCase.UpdateSortCriteria,
) : StateSideEffectHandler<MessageListState, MessageListEvent>(logger, dispatch) {
    override fun accept(
        event: MessageListEvent,
        newState: MessageListState,
    ): Boolean = event is MessageListEvent.ChangeSortCriteria

    override suspend fun handle(oldState: MessageListState, newState: MessageListState) {
        logger.verbose(TAG) {
            "ChangeSortCriteriaSideEffect.handle() called with: oldState = $oldState, newState = $newState"
        }
        val folder = oldState.metadata.folder ?: newState.metadata.folder
        val currentAccountId = folder?.account?.id
        val oldSortCriteria = oldState.metadata.sortCriteriaPerAccount[currentAccountId]
        val newSortCriteria = requireNotNull(newState.metadata.sortCriteriaPerAccount[currentAccountId]) {
            "The new sort criteria for account $currentAccountId must not be null"
        }
        if (oldSortCriteria != newSortCriteria) {
            updateSortCriteria(currentAccountId, newSortCriteria)
        }
    }

    class Factory(
        private val logger: Logger,
        private val updateSortCriteria: DomainContract.UseCase.UpdateSortCriteria,
    ) : MessageListStateSideEffectHandlerFactory {
        override fun create(
            scope: CoroutineScope,
            dispatch: suspend (MessageListEvent) -> Unit,
        ): StateSideEffectHandler<MessageListState, MessageListEvent> = ChangeSortCriteriaSideEffect(
            dispatch = dispatch,
            logger = logger,
            updateSortCriteria = updateSortCriteria,
        )
    }
}
