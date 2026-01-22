package net.thunderbird.feature.mail.message.list.internal.ui.state.sideeffect

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import dev.mokkery.spy
import dev.mokkery.verifySuspend
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.logging.Logger
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.mail.message.list.domain.DomainContract
import net.thunderbird.feature.mail.message.list.domain.model.SortCriteria
import net.thunderbird.feature.mail.message.list.domain.model.SortType
import net.thunderbird.feature.mail.message.list.ui.event.MessageListEvent
import net.thunderbird.feature.mail.message.list.ui.state.MessageListState
import org.junit.Test

class LoadSortTypeStateSideEffectHandlerTest {
    @Test
    fun `accept() should return true when event is LoadConfigurations`() {
        // Arrange
        val handler = createTestSubject()
        val event = MessageListEvent.LoadConfigurations
        val state = MessageListState.WarmingUp()

        // Act
        val result = handler.accept(event, state)

        // Assert
        assertThat(result).isTrue()
    }

    @Test
    fun `accept() should return false when event is not LoadConfigurations`() {
        // Arrange
        val handler = createTestSubject()
        val event = MessageListEvent.AllConfigsReady
        val state = MessageListState.WarmingUp()

        // Act
        val result = handler.accept(event, state)

        // Assert
        assertThat(result).isFalse()
    }

    @Test
    fun `handle() should call getSortTypes and dispatch SortTypesLoaded`() = runTest {
        // Arrange
        val logger = TestLogger()
        val accounts = setOf(AccountIdFactory.create())
        val dispatch = spy<suspend (MessageListEvent) -> Unit>(obj = {})
        val sortCriteriaPerAccount = mapOf(accounts.firstOrNull() to SortCriteria(primary = SortType.DateDesc))
        val fakeGetSortCriteriaPerAccount = spy<DomainContract.UseCase.GetSortCriteriaPerAccount>(FakeGetSortCriteriaPerAccount(sortCriteriaPerAccount))
        val handler = createTestSubject(
            accounts = accounts,
            getSortCriteriaPerAccount = fakeGetSortCriteriaPerAccount,
            dispatch = dispatch,
        )

        // Act
        handler.handle(oldState = MessageListState.WarmingUp(), newState = MessageListState.WarmingUp())

        // Assert
        verifySuspend {
            fakeGetSortCriteriaPerAccount.invoke(accounts)
            dispatch(MessageListEvent.SortCriteriaLoaded(sortCriteriaPerAccount))
        }
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
        override suspend fun invoke(accountIds: Set<AccountId>): Map<AccountId?, SortCriteria> = sortCriteriaPerAccount
    }
}
