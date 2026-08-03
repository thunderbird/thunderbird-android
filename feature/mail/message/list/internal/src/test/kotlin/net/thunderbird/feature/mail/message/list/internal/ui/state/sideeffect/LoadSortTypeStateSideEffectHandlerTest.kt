package net.thunderbird.feature.mail.message.list.internal.ui.state.sideeffect

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.common.state.sideeffect.StateSideEffectHandler
import net.thunderbird.core.logging.Logger
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.mail.message.list.domain.DomainContract
import net.thunderbird.feature.mail.message.list.domain.model.SortCriteria
import net.thunderbird.feature.mail.message.list.domain.model.SortType
import net.thunderbird.feature.mail.message.list.internal.fakes.RecordingSuspendFunction
import net.thunderbird.feature.mail.message.list.ui.event.MessageListEvent
import net.thunderbird.feature.mail.message.list.ui.state.MessageListState
import org.junit.Test

class LoadSortTypeStateSideEffectHandlerTest : BaseSideEffectHandlerTest() {
    @Test
    fun `handle() should return Consumed when event is LoadConfigurations`() = runTest {
        // Arrange
        val handler = createTestSubject()
        val event = MessageListEvent.LoadConfigurations
        val state = MessageListState.WarmingUp()

        // Act
        val result = handler.handle(event, oldState = MessageListState.WarmingUp(), newState = state)

        // Assert
        assertThat(result).isEqualTo(StateSideEffectHandler.ConsumeResult.Consumed)
    }

    @Test
    fun `handle() should return Ignored when event is not LoadConfigurations`() = runTest {
        // Arrange
        val handler = createTestSubject()
        val event = MessageListEvent.AllConfigsReady
        val state = MessageListState.WarmingUp()

        // Act
        val result = handler.handle(event, oldState = MessageListState.WarmingUp(), newState = state)

        // Assert
        assertThat(result).isEqualTo(StateSideEffectHandler.ConsumeResult.Ignored)
    }

    @Test
    fun `handle() should call getSortTypes and dispatch SortTypesLoaded`() = runTest {
        // Arrange
        TestLogger()
        val accounts = setOf(AccountIdFactory.create())
        val dispatch = RecordingSuspendFunction<MessageListEvent>()
        val sortCriteriaPerAccount = mapOf(accounts.firstOrNull() to SortCriteria(primary = SortType.DateDesc))
        val fakeGetSortCriteriaPerAccount = FakeGetSortCriteriaPerAccount(sortCriteriaPerAccount)
        val handler = createTestSubject(
            accounts = accounts,
            getSortCriteriaPerAccount = fakeGetSortCriteriaPerAccount,
            dispatch = dispatch.function,
        )

        // Act
        handler.handle(
            event = MessageListEvent.LoadConfigurations,
            oldState = MessageListState.WarmingUp(),
            newState = MessageListState.WarmingUp(),
        )

        // Assert
        assertThat(fakeGetSortCriteriaPerAccount.calls).containsExactly(accounts)
        assertThat(dispatch.calls).containsExactly(MessageListEvent.SortCriteriaLoaded(sortCriteriaPerAccount))
    }

    private fun createTestSubject(
        getSortCriteriaPerAccount: DomainContract.UseCase.GetSortCriteriaPerAccount = FakeGetSortCriteriaPerAccount(),
        logger: Logger = TestLogger(),
        accounts: Set<AccountId> = setOf(AccountIdFactory.create()),
        dispatch: suspend (MessageListEvent) -> Unit = {},
    ) = LoadSortCriteriaStateSideEffectHandler(
        accounts = accounts,
        dispatch = dispatch,
        logger = logger,
        getSortCriteriaPerAccount = getSortCriteriaPerAccount,
    )

    private class FakeGetSortCriteriaPerAccount(
        private val sortCriteriaPerAccount: Map<AccountId?, SortCriteria> = emptyMap(),
    ) : DomainContract.UseCase.GetSortCriteriaPerAccount {
        val calls = mutableListOf<Set<AccountId>>()

        override suspend fun invoke(accountIds: Set<AccountId>): Map<AccountId?, SortCriteria> {
            calls += accountIds
            return sortCriteriaPerAccount
        }
    }
}
