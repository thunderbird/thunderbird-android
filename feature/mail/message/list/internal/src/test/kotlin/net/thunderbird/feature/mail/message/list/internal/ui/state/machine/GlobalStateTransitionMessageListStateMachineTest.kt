@file:OptIn(ExperimentalCoroutinesApi::class)

package net.thunderbird.feature.mail.message.list.internal.ui.state.machine

import app.cash.turbine.test
import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import assertk.assertions.isTrue
import assertk.assertions.prop
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.mail.message.list.domain.model.SortCriteria
import net.thunderbird.feature.mail.message.list.domain.model.SortType
import net.thunderbird.feature.mail.message.list.ui.event.MessageItemEvent
import net.thunderbird.feature.mail.message.list.ui.event.MessageListEvent
import net.thunderbird.feature.mail.message.list.ui.state.MessageListState

class GlobalStateTransitionMessageListStateMachineTest : BaseMessageListStateMachineTest() {
    // region [Global state transitions]
    @Test
    fun `late focus exit from a previous message should retain the current focused message`() = runTest {
        // Arrange
        val messages = createMessageUiItemList(size = 2)
        val stateMachine = createStateMachineOnLoadedState(messages = messages)
        stateMachine.process(MessageItemEvent.OnFocusEnter(messages[0]))
        stateMachine.process(MessageItemEvent.OnFocusEnter(messages[1]))

        // Act
        stateMachine.process(MessageItemEvent.OnFocusExit(messages[0]))

        // Assert
        assertThat(stateMachine.currentStateSnapshot.metadata.focusedMessage).isEqualTo(messages[1])
    }

    @Test
    fun `focus exit should match message identity after its flags have changed`() = runTest {
        // Arrange
        val message = createMessageUiItemList(size = 1).single()
        val updatedMessage = message.copy(starred = !message.starred)
        val stateMachine = createStateMachineOnLoadedState(messages = listOf(updatedMessage))
        stateMachine.process(MessageItemEvent.OnFocusEnter(updatedMessage))

        // Act
        stateMachine.process(MessageItemEvent.OnFocusExit(message))

        // Assert
        assertThat(stateMachine.currentStateSnapshot.metadata.focusedMessage).isNull()
    }

    @Test
    fun `focus enter from a removed message should not replace the current focus`() = runTest {
        // Arrange
        val messages = createMessageUiItemList(size = 2)
        val stateMachine = createStateMachineOnLoadedState(messages = listOf(messages[0]))
        stateMachine.process(MessageItemEvent.OnFocusEnter(messages[0]))

        // Act
        stateMachine.process(MessageItemEvent.OnFocusEnter(messages[1]))

        // Assert
        assertThat(stateMachine.currentStateSnapshot.metadata.focusedMessage).isEqualTo(messages[0])
    }

    @Test
    fun `focus enter should use current message flags instead of a stale rendered snapshot`() = runTest {
        // Arrange
        val message = createMessageUiItemList(size = 1).single()
        val updatedMessage = message.copy(starred = !message.starred)
        val stateMachine = createStateMachineOnLoadedState(messages = listOf(updatedMessage))

        // Act
        stateMachine.process(MessageItemEvent.OnFocusEnter(message))

        // Assert
        assertThat(stateMachine.currentStateSnapshot.metadata.focusedMessage).isEqualTo(updatedMessage)
    }

    @Test
    fun `process() should update focusedMessage when event is OnFocusEnter from LoadedMessages`() =
        runTest {
            // Arrange
            val messages = createMessageUiItemList(size = 5)
            val focusMessage = messages.random()
            val stateMachine = createStateMachineOnLoadedState(messages = messages)
            advanceUntilIdle()

            stateMachine.currentState.test {
                // enforce correct state before acting.
                assertThat(expectMostRecentItem()).isInstanceOf<MessageListState.LoadedMessages>()

                // Act
                stateMachine.process(MessageItemEvent.OnFocusEnter(focusMessage))

                // Assert
                assertThat(awaitItem())
                    .isInstanceOf<MessageListState.LoadedMessages>()
                    .transform { it.metadata.focusedMessage }
                    .isEqualTo(focusMessage)

                expectNoEvents()
            }
        }

    @Test
    fun `process() should clear focusedMessage when event is OnFocusExit from LoadedMessages`() =
        runTest {
            // Arrange
            val messages = createMessageUiItemList(size = 5)
            val focusMessage = messages.random()
            val stateMachine = createStateMachineOnLoadedState(messages = messages)
            advanceUntilIdle()

            stateMachine.currentState.test {
                // enforce correct state before acting.
                assertThat(expectMostRecentItem()).isInstanceOf<MessageListState.LoadedMessages>()

                // Set focus to the random message
                stateMachine.process(MessageItemEvent.OnFocusEnter(focusMessage))
                assertThat(awaitItem())
                    .isInstanceOf<MessageListState.LoadedMessages>()
                    .transform { it.metadata.focusedMessage }
                    .isEqualTo(focusMessage)

                // Act
                stateMachine.process(MessageItemEvent.OnFocusExit(focusMessage))

                // Assert
                assertThat(awaitItem())
                    .isInstanceOf<MessageListState.LoadedMessages>()
                    .transform { it.metadata.focusedMessage }
                    .isNull()

                expectNoEvents()
            }
        }

    @Test
    fun `process() should move to SelectingMessages with all messages selected when event is SelectAll`() =
        runTest {
            // Arrange
            val messages = createMessageUiItemList(size = 10)
            val stateMachine = createStateMachineOnLoadedState(messages = messages)
            advanceUntilIdle()

            stateMachine.currentState.test {
                // enforce correct state before acting.
                assertThat(expectMostRecentItem()).isInstanceOf<MessageListState.LoadedMessages>()

                // Act
                stateMachine.process(MessageItemEvent.SelectAll)

                // Assert
                assertThat(awaitItem())
                    .isInstanceOf<MessageListState.SelectingMessages>()
                    .all {
                        transform { state -> state.messages.all { it.selected } }.isTrue()
                        prop(MessageListState.SelectingMessages::selectedCount).isEqualTo(messages.size)
                    }

                expectNoEvents()
            }
        }

    @Test
    fun `process() should move to LoadedMessages with all messages deselected when event is DeselectAll`() =
        runTest {
            // Arrange
            val messages = createMessageUiItemList(size = 10)
            val stateMachine = createStateMachineOnSelectingMessages(messages = messages)
            advanceUntilIdle()

            stateMachine.currentState.test {
                // enforce correct state before acting.
                assertThat(expectMostRecentItem()).isInstanceOf<MessageListState.SelectingMessages>()

                // Act
                stateMachine.process(MessageItemEvent.DeselectAll)

                // Assert
                assertThat(awaitItem())
                    .isInstanceOf<MessageListState.LoadedMessages>()
                    .transform { state -> state.messages.none { it.selected } }
                    .isTrue()

                expectNoEvents()
            }
        }

    @Test
    fun `process() should update sortCriteriaPerAccount when event is ChangeSortCriteria`() =
        runTest {
            // Arrange
            val accountId = AccountIdFactory.create()
            val newSortCriteria = SortCriteria(SortType.SubjectDesc, SortType.DateDesc)
            val messages = createMessageUiItemList(size = 5)
            val stateMachine = createStateMachineOnLoadedState(messages = messages)
            advanceUntilIdle()

            stateMachine.currentState.test {
                // enforce correct state before acting.
                assertThat(expectMostRecentItem()).isInstanceOf<MessageListState.LoadedMessages>()

                // Act
                stateMachine.process(MessageListEvent.ChangeSortCriteria(accountId, newSortCriteria))

                // Assert
                assertThat(awaitItem())
                    .isInstanceOf<MessageListState.LoadedMessages>()
                    .transform { it.metadata.sortCriteriaPerAccount[accountId] }
                    .isEqualTo(newSortCriteria)

                expectNoEvents()
            }
        }
    // endregion [Global state transitions]
}
