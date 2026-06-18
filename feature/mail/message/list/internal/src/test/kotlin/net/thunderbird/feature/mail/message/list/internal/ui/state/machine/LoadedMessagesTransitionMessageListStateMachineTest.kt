@file:OptIn(ExperimentalCoroutinesApi::class)

package net.thunderbird.feature.mail.message.list.internal.ui.state.machine

import app.cash.turbine.test
import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import net.thunderbird.feature.mail.message.list.ui.event.MessageItemEvent
import net.thunderbird.feature.mail.message.list.ui.state.MessageListState

class LoadedMessagesTransitionMessageListStateMachineTest : BaseMessageListStateMachineTest() {
    // region [LoadedMessages - active message tracking]
    @Test
    fun `process() should set activeMessage when event is OnMessageClick in LoadedMessages`() =
        runTest {
            // Arrange
            val messages = createMessageUiItemList(size = 5)
            val clickedMessage = messages.first()
            val stateMachine = createStateMachineOnLoadedState(messages = messages)
            advanceUntilIdle()

            stateMachine.currentState.test {
                // enforce correct state before acting.
                assertThat(expectMostRecentItem()).isInstanceOf<MessageListState.LoadedMessages>()

                // Act
                stateMachine.process(MessageItemEvent.OnMessageClick(clickedMessage))

                // Assert
                assertThat(awaitItem())
                    .isInstanceOf<MessageListState.LoadedMessages>()
                    .all {
                        transform { it.metadata.activeMessage }.isEqualTo(clickedMessage)
                        transform { state -> state.messages.first { it.id == clickedMessage.id }.active }.isTrue()
                    }

                expectNoEvents()
            }
        }

    @Test
    fun `process() should set activeMessage when event is SetMessageActive in LoadedMessages`() =
        runTest {
            // Arrange
            val messages = createMessageUiItemList(size = 5)
            val targetMessage = messages.first()
            val stateMachine = createStateMachineOnLoadedState(messages = messages)
            advanceUntilIdle()

            stateMachine.currentState.test {
                // enforce correct state before acting.
                assertThat(expectMostRecentItem()).isInstanceOf<MessageListState.LoadedMessages>()

                // Act
                stateMachine.process(MessageItemEvent.SetMessageActive(targetMessage))

                // Assert
                assertThat(awaitItem())
                    .isInstanceOf<MessageListState.LoadedMessages>()
                    .all {
                        transform { it.metadata.activeMessage }.isEqualTo(targetMessage)
                        transform { state -> state.messages.first { it.id == targetMessage.id }.active }.isTrue()
                    }

                expectNoEvents()
            }
        }

    @Test
    fun `process() should clear activeMessage when event is SetMessageActive with null`() =
        runTest {
            // Arrange
            val messages = createMessageUiItemList(size = 5)
            val stateMachine = createStateMachineOnLoadedState(messages = messages)
            advanceUntilIdle()

            stateMachine.currentState.test {
                // enforce correct state before acting.
                assertThat(expectMostRecentItem()).isInstanceOf<MessageListState.LoadedMessages>()

                // Set active first
                stateMachine.process(MessageItemEvent.SetMessageActive(messages.first()))
                assertThat(awaitItem())
                    .isInstanceOf<MessageListState.LoadedMessages>()
                    .transform { it.metadata.activeMessage }
                    .isNotNull()

                // Act
                stateMachine.process(MessageItemEvent.SetMessageActive(null))

                // Assert
                assertThat(awaitItem())
                    .isInstanceOf<MessageListState.LoadedMessages>()
                    .all {
                        transform { it.metadata.activeMessage }.isNull()
                        transform { state -> state.messages.none { it.active } }.isTrue()
                    }

                expectNoEvents()
            }
        }
    // endregion [LoadedMessages - active message tracking]
}
