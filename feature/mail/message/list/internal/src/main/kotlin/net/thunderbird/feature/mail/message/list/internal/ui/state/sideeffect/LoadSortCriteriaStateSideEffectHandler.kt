package net.thunderbird.feature.mail.message.list.internal.ui.state.sideeffect

import kotlinx.coroutines.CoroutineScope
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.message.list.domain.DomainContract
import net.thunderbird.feature.mail.message.list.ui.effect.MessageListEffect
import net.thunderbird.feature.mail.message.list.ui.event.MessageListEvent
import net.thunderbird.feature.mail.message.list.ui.state.MessageListState
import net.thunderbird.feature.mail.message.list.ui.state.sideeffect.MessageListStateSideEffectHandler
import net.thunderbird.feature.mail.message.list.ui.state.sideeffect.MessageListStateSideEffectHandlerFactory

private const val TAG = "LoadSortCriteriaStateSideEffectHandler"

internal class LoadSortCriteriaStateSideEffectHandler(
    private val accounts: Set<AccountId>,
    dispatch: suspend (MessageListEvent) -> Unit,
    private val logger: Logger,
    private val getSortCriteriaPerAccount: DomainContract.UseCase.GetSortCriteriaPerAccount,
) : MessageListStateSideEffectHandler(logger, dispatch) {
    override fun accept(event: MessageListEvent, oldState: MessageListState, newState: MessageListState): Boolean =
        event == MessageListEvent.LoadConfigurations

    override suspend fun consume(
        event: MessageListEvent,
        oldState: MessageListState,
        newState: MessageListState,
    ): ConsumeResult {
        logger.verbose(TAG) { "$TAG.handle() called with: oldState = $oldState, newState = $newState" }
        val sortCriteriaPerAccount = getSortCriteriaPerAccount(accountIds = accounts)
        logger.verbose(TAG) { "saved sort criteria = $sortCriteriaPerAccount" }
        dispatch(MessageListEvent.SortCriteriaLoaded(sortCriteriaPerAccount))
        return ConsumeResult.Consumed
    }

    class Factory(
        private val accounts: Set<AccountId>,
        private val logger: Logger,
        private val getSortCriteriaPerAccount: DomainContract.UseCase.GetSortCriteriaPerAccount,
    ) : MessageListStateSideEffectHandlerFactory {
        override fun create(
            scope: CoroutineScope,
            dispatch: suspend (MessageListEvent) -> Unit,
            dispatchUiEffect: suspend (MessageListEffect) -> Unit,
        ): MessageListStateSideEffectHandler = LoadSortCriteriaStateSideEffectHandler(
            accounts = accounts,
            dispatch = dispatch,
            logger = logger,
            getSortCriteriaPerAccount = getSortCriteriaPerAccount,
        )
    }
}
