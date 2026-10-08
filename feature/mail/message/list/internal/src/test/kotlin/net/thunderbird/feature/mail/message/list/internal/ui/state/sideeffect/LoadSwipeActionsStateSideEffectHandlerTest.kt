package net.thunderbird.feature.mail.message.list.internal.ui.state.sideeffect

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.common.action.SwipeAction
import net.thunderbird.core.common.action.SwipeActions
import net.thunderbird.core.common.state.sideeffect.StateSideEffectHandler
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.mail.message.list.domain.DomainContract
import net.thunderbird.feature.mail.message.list.internal.fakes.RecordingFunction
import net.thunderbird.feature.mail.message.list.ui.event.MessageListEvent
import net.thunderbird.feature.mail.message.list.ui.state.MessageListState
import org.junit.Test

class LoadSwipeActionsStateSideEffectHandlerTest : BaseSideEffectHandlerTest() {
    @Test
    fun `handle() should return Consumed if event is LoadConfigurations`() = runTest {
        // Arrange
        val testSubject = LoadSwipeActionsStateSideEffectHandler(
            dispatch = {},
            scope = backgroundScope,
            logger = TestLogger(),
            buildSwipeActions = FakeBuildSwipeActionsUseCase(),
        )

        // Act
        val actual = testSubject.handle(
            event = MessageListEvent.LoadConfigurations,
            oldState = MessageListState.WarmingUp(),
            newState = createReadyWarmingUpState(),
        )

        // Assert
        assertThat(actual).isEqualTo(StateSideEffectHandler.ConsumeResult.Consumed)
    }

    @Test
    fun `handle() should return Ignored if event is not LoadConfigurations`() = runTest {
        // Arrange
        val testSubject = LoadSwipeActionsStateSideEffectHandler(
            dispatch = {},
            scope = backgroundScope,
            logger = TestLogger(),
            buildSwipeActions = FakeBuildSwipeActionsUseCase(),
        )

        // Act
        val actual = testSubject.handle(
            event = MessageListEvent.ExitSelectionMode,
            oldState = MessageListState.WarmingUp(),
            newState = createReadyWarmingUpState(),
        )

        // Assert
        assertThat(actual).isEqualTo(StateSideEffectHandler.ConsumeResult.Ignored)
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun `handle() should start buildSwipeActions flow and dispatch SwipeActionsLoaded event`() =
        runTest(UnconfinedTestDispatcher()) {
            // Arrange
            val dispatch = RecordingFunction<MessageListEvent>()
            val initialSwipeActions = mapOf<AccountId, SwipeActions>(
                AccountIdFactory.create() to SwipeActions(SwipeAction.None, SwipeAction.None),
            )
            val fakeBuildSwipeActionsUseCase = FakeBuildSwipeActionsUseCase(initialSwipeActions)
            val testSubject = LoadSwipeActionsStateSideEffectHandler(
                dispatch = dispatch.function,
                scope = backgroundScope,
                logger = TestLogger(),
                buildSwipeActions = fakeBuildSwipeActionsUseCase,
            )
            val oldState = MessageListState.WarmingUp()
            val newState = oldState.withMetadata { copy(isActive = true) }

            // Act
            testSubject.handle(event = MessageListEvent.LoadConfigurations, oldState = oldState, newState = newState)

            // Assert
            assertThat(dispatch.calls).containsExactly(MessageListEvent.SwipeActionsLoaded(initialSwipeActions))
        }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun `buildSwipeActions() should dispatch SwipeActionsLoaded event whenever a new swipe action is emitted`() =
        runTest(UnconfinedTestDispatcher()) {
            // Arrange
            val dispatch = RecordingFunction<MessageListEvent>()
            val initialSwipeActions = mapOf(
                AccountIdFactory.create() to SwipeActions(SwipeAction.None, SwipeAction.None),
            )
            val swipeActions = initialSwipeActions + List(size = 5) {
                AccountIdFactory.create() to SwipeActions(SwipeAction.entries.random(), SwipeAction.entries.random())
            }
            val fakeBuildSwipeActionsUseCase = FakeBuildSwipeActionsUseCase(initialSwipeActions)
            val testSubject = LoadSwipeActionsStateSideEffectHandler(
                dispatch = dispatch.function,
                scope = backgroundScope,
                logger = TestLogger(),
                buildSwipeActions = fakeBuildSwipeActionsUseCase,
            )
            val oldState = MessageListState.WarmingUp()
            val newState = oldState.withMetadata { copy(isActive = true) }

            // Act
            testSubject.handle(event = MessageListEvent.LoadConfigurations, oldState = oldState, newState = newState)

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
            assertThat(dispatch.calls).hasSize(5)
        }
}

private class FakeBuildSwipeActionsUseCase(
    initialValue: Map<AccountId, SwipeActions> = mapOf(),
) : DomainContract.UseCase.BuildSwipeActions {
    val swipeActions = MutableStateFlow(initialValue)

    override fun invoke(): StateFlow<Map<AccountId, SwipeActions>> = swipeActions
}
