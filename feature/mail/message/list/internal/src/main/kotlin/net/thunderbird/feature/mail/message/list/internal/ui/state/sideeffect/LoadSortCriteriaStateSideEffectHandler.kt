package net.thunderbird.feature.mail.message.list.internal.ui.state.sideeffect

import kotlinx.coroutines.CoroutineScope
import net.thunderbird.core.common.state.sideeffect.StateSideEffectHandler
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.message.list.domain.DomainContract
import net.thunderbird.feature.mail.message.list.ui.MessageListStateSideEffectHandlerFactory
import net.thunderbird.feature.mail.message.list.ui.event.MessageListEvent
import net.thunderbird.feature.mail.message.list.ui.state.MessageListState

private const val TAG = "LoadSortCriteriaStateSideEffectHandler"

class LoadSortCriteriaStateSideEffectHandler(
    private val accounts: Set<AccountId>,
    dispatch: suspend (MessageListEvent) -> Unit,
    private val logger: Logger,
    private val getSortCriteriaPerAccount: DomainContract.UseCase.GetSortCriteriaPerAccount,
) : StateSideEffectHandler<MessageListState, MessageListEvent>(logger, dispatch) {
    override fun accept(event: MessageListEvent, newState: MessageListState): Boolean =
        event == MessageListEvent.LoadConfigurations

    override suspend fun handle(oldState: MessageListState, newState: MessageListState) {
        logger.verbose(TAG) { "$TAG.handle() called with: oldState = $oldState, newState = $newState" }
        val sortCriteriaPerAccount = getSortCriteriaPerAccount(accountIds = accounts)
        logger.verbose(TAG) { "saved sort criteria = $sortCriteriaPerAccount" }
        dispatch(MessageListEvent.SortCriteriaLoaded(sortCriteriaPerAccount))
    }

    class Factory(
        private val accounts: Set<AccountId>,
        private val logger: Logger,
        private val getSortCriteriaPerAccount: DomainContract.UseCase.GetSortCriteriaPerAccount,
    ) : MessageListStateSideEffectHandlerFactory {
        override fun create(
            scope: CoroutineScope,
            dispatch: suspend (MessageListEvent) -> Unit,
        ): StateSideEffectHandler<MessageListState, MessageListEvent> = LoadSortCriteriaStateSideEffectHandler(
            accounts = accounts,
            dispatch = dispatch,
            logger = logger,
            getSortCriteriaPerAccount = getSortCriteriaPerAccount,
        )
    }
}
