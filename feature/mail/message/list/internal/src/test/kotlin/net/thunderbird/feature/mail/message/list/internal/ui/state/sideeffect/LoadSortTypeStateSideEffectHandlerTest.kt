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
import net.thunderbird.feature.mail.message.list.ui.event.MessageListEvent
import net.thunderbird.feature.mail.message.list.ui.state.MessageListState
import net.thunderbird.feature.mail.message.list.ui.state.SortType
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
        val sortTypes: Map<AccountId?, SortType> = mapOf(accounts.first() to SortType.DateDesc)
        val fakeGetSortTypes = spy<DomainContract.UseCase.GetSortTypes>(FakeGetSortTypes(sortTypes))
        val handler = createTestSubject(
            accounts = accounts,
            getSortTypes = fakeGetSortTypes,
            dispatch = dispatch,
        )

        // Act
        handler.handle(oldState = MessageListState.WarmingUp(), newState = MessageListState.WarmingUp())

        // Assert
        verifySuspend {
            fakeGetSortTypes.invoke(accounts)
            dispatch(MessageListEvent.SortTypesLoaded(sortTypes))
        }
    }

    private fun createTestSubject(
        getSortTypes: DomainContract.UseCase.GetSortTypes = FakeGetSortTypes(),
        logger: Logger = TestLogger(),
        accounts: Set<AccountId> = setOf(AccountIdFactory.create()),
        dispatch: suspend (MessageListEvent) -> Unit = {},
    ) = LoadSortTypeStateSideEffectHandler(
        accounts = accounts,
        dispatch = dispatch,
        logger = logger,
        getSortTypes = getSortTypes,
    )

    private class FakeGetSortTypes(
        private val sortTypes: Map<AccountId?, SortType> = emptyMap(),
    ) : DomainContract.UseCase.GetSortTypes {
        override suspend fun invoke(accountIds: Set<AccountId>): Map<AccountId?, SortType> = sortTypes
    }
}
