package net.thunderbird.feature.mail.message.list.internal.ui.state.sideeffect

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.spy
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.common.action.SwipeAction
import net.thunderbird.core.common.action.SwipeActions
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.mail.message.list.domain.DomainContract
import net.thunderbird.feature.mail.message.list.ui.event.MessageListEvent
import net.thunderbird.feature.mail.message.list.ui.state.MessageListState
import org.junit.Test

class LoadSwipeActionsStateSideEffectHandlerTest {
    @Test
    fun `accept() should return true if event is LoadConfigurations`() = runTest {
        // Arrange
        val testSubject = LoadSwipeActionsStateSideEffectHandler(
            dispatch = {},
            scope = this,
            logger = TestLogger(),
            buildSwipeActions = mock(),
        )

        // Act
        val actual = testSubject.accept(
            event = MessageListEvent.LoadConfigurations,
            newState = MessageListState.WarmingUp(),
        )

        // Assert
        assertThat(actual).isTrue()
    }

    @Test
    fun `accept() should return false if event is not LoadConfigurations`() = runTest {
        // Arrange
        val testSubject = LoadSwipeActionsStateSideEffectHandler(
            dispatch = {},
            scope = this,
            logger = TestLogger(),
            buildSwipeActions = mock(),
        )

        // Act
        val actual = testSubject.accept(
            event = MessageListEvent.ExitSelectionMode,
            newState = MessageListState.WarmingUp(),
        )

        // Assert
        assertThat(actual).isFalse()
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun `handle() should start buildSwipeActions flow and dispatch SwipeActionsLoaded event`() =
        runTest(UnconfinedTestDispatcher()) {
            // Arrange
            val dispatch = spy<(MessageListEvent) -> Unit>(obj = {})
            val initialSwipeActions = mapOf<AccountId, SwipeActions>(
                AccountIdFactory.create() to SwipeActions(SwipeAction.None, SwipeAction.None),
            )
            val fakeBuildSwipeActionsUseCase = FakeBuildSwipeActionsUseCase(initialSwipeActions)
            val testSubject = LoadSwipeActionsStateSideEffectHandler(
                dispatch = dispatch,
                scope = backgroundScope,
                logger = TestLogger(),
                buildSwipeActions = fakeBuildSwipeActionsUseCase,
            )
            val oldState = MessageListState.WarmingUp()
            val newState = oldState.withMetadata { copy(isActive = true) }

            // Act
            testSubject.handle(oldState, newState)

            // Assert
            verify(mode = VerifyMode.exactly(1)) {
                dispatch(MessageListEvent.SwipeActionsLoaded(initialSwipeActions))
            }
        }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun `buildSwipeActions() should dispatch SwipeActionsLoaded event whenever a new swipe action is emitted`() =
        runTest(UnconfinedTestDispatcher()) {
            // Arrange
            val dispatch = spy<(MessageListEvent) -> Unit>(obj = {})
            val initialSwipeActions = mapOf(
                AccountIdFactory.create() to SwipeActions(SwipeAction.None, SwipeAction.None),
            )
            val swipeActions = initialSwipeActions + List(size = 5) {
                AccountIdFactory.create() to SwipeActions(SwipeAction.entries.random(), SwipeAction.entries.random())
            }
            val fakeBuildSwipeActionsUseCase = FakeBuildSwipeActionsUseCase(initialSwipeActions)
            val testSubject = LoadSwipeActionsStateSideEffectHandler(
                dispatch = dispatch,
                scope = backgroundScope,
                logger = TestLogger(),
                buildSwipeActions = fakeBuildSwipeActionsUseCase,
            )
            val oldState = MessageListState.WarmingUp()
            val newState = oldState.withMetadata { copy(isActive = true) }

            // Act
            testSubject.handle(oldState, newState)

            swipeActions.entries.foldIndexed(mapOf<AccountId, SwipeActions>()) {
                    index,
                    acc,
                    (accountId, swipeActions),
                ->
                if (index > 1) {
                    // emit new swipe actions
                    fakeBuildSwipeActionsUseCase.swipeActions.emit(acc)
                }
                acc + (accountId to swipeActions)
            }

            // Assert
            verify(mode = VerifyMode.exactly(5)) {
                dispatch(any())
            }
        }
}

private class FakeBuildSwipeActionsUseCase(
    initialValue: Map<AccountId, SwipeActions> = mapOf(),
) : DomainContract.UseCase.BuildSwipeActions {
    val swipeActions = MutableStateFlow(initialValue)

    override fun invoke(): StateFlow<Map<AccountId, SwipeActions>> = swipeActions
}
