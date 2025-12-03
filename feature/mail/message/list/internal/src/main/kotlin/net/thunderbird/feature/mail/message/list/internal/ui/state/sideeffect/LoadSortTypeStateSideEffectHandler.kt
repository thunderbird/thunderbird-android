package net.thunderbird.feature.mail.message.list.internal.ui.state.sideeffect

import kotlinx.coroutines.CoroutineScope
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.message.list.domain.DomainContract
import net.thunderbird.feature.mail.message.list.ui.event.MessageListEvent
import net.thunderbird.feature.mail.message.list.ui.state.MessageListState

private const val TAG = "LoadSortTypeSideEffectHandler"

class LoadSortTypeStateSideEffectHandler(
    private val accounts: Set<AccountId>,
    dispatch: suspend (MessageListEvent) -> Unit,
    private val logger: Logger,
    private val getSortTypes: DomainContract.UseCase.GetSortTypes,
) : StateSideEffectHandler(logger, dispatch) {
    override fun accept(event: MessageListEvent, newState: MessageListState): Boolean =
        event == MessageListEvent.LoadConfigurations

    override suspend fun handle(oldState: MessageListState, newState: MessageListState) {
        logger.verbose(TAG) { "$TAG.handle() called with: oldState = $oldState, newState = $newState" }
        val sortTypes = getSortTypes(accountIds = accounts)
        dispatch(MessageListEvent.SortTypesLoaded(sortTypes))
    }

    class Factory(
        private val accounts: Set<AccountId>,
        private val logger: Logger,
        private val getSortTypes: DomainContract.UseCase.GetSortTypes,
    ) : StateSideEffectHandler.Factory {
        override fun create(
            scope: CoroutineScope,
            dispatch: suspend (MessageListEvent) -> Unit,
        ): StateSideEffectHandler = LoadSortTypeStateSideEffectHandler(
            accounts = accounts,
            dispatch = dispatch,
            logger = logger,
            getSortTypes = getSortTypes,
        )
    }
}
