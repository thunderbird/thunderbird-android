@file:OptIn(ExperimentalCoroutinesApi::class)

package net.thunderbird.feature.mail.message.list.internal.ui.state.machine

import app.cash.turbine.test
import assertk.all
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlin.test.Test
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import net.thunderbird.feature.mail.message.list.ui.event.MessageItemEvent
import net.thunderbird.feature.mail.message.list.ui.state.ComposedAddressStyle
import net.thunderbird.feature.mail.message.list.ui.state.ComposedAddressUi
import net.thunderbird.feature.mail.message.list.ui.state.MessageItemUi
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

    @Test
    fun `process() should deactivate the previously active message when another becomes active`() =
        runTest {
            // Arrange
            val messages = createMessageUiItemList(size = 5) { index ->
                createMessageUiItem(state = MessageItemUi.State.Read, id = "id$index")
            }
            val stateMachine = createStateMachineOnLoadedState(messages = messages)
            advanceUntilIdle()

            stateMachine.currentState.test {
                // enforce correct state before acting.
                assertThat(expectMostRecentItem()).isInstanceOf<MessageListState.LoadedMessages>()
                stateMachine.process(MessageItemEvent.SetMessageActive(messages[1]))
                awaitItem()

                // Act
                stateMachine.process(MessageItemEvent.SetMessageActive(messages[3]))

                // Assert
                assertThat(awaitItem())
                    .isInstanceOf<MessageListState.LoadedMessages>()
                    .transform { state -> state.messages.filter { it.active }.map { it.id } }
                    .containsExactly(messages[3].id)

                expectNoEvents()
            }
        }

    @Test
    fun `process() should mark the clicked message as read when event is OnMessageClick`() =
        runTest {
            // Arrange
            val messages = createMessageUiItemList(size = 5) { index ->
                createMessageUiItem(state = MessageItemUi.State.Unread, id = "id$index")
            }
            val clickedMessage = messages[2]
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
                    .transform { state -> state.messages.map { it.id to it.state } }
                    .containsExactly(
                        "id0" to MessageItemUi.State.Unread,
                        "id1" to MessageItemUi.State.Unread,
                        "id2" to MessageItemUi.State.Read,
                        "id3" to MessageItemUi.State.Unread,
                        "id4" to MessageItemUi.State.Unread,
                    )

                expectNoEvents()
            }
        }

    @Test
    fun `process() should deactivate the previously active message when event is OnMessageClick`() =
        runTest {
            // Arrange
            val messages = createMessageUiItemList(size = 5) { index ->
                createMessageUiItem(state = MessageItemUi.State.Read, id = "id$index")
            }
            val stateMachine = createStateMachineOnLoadedState(messages = messages)
            advanceUntilIdle()

            stateMachine.currentState.test {
                // enforce correct state before acting.
                assertThat(expectMostRecentItem()).isInstanceOf<MessageListState.LoadedMessages>()
                stateMachine.process(MessageItemEvent.SetMessageActive(messages[1]))
                awaitItem()

                // Act
                stateMachine.process(MessageItemEvent.OnMessageClick(messages[3]))

                // Assert
                assertThat(awaitItem())
                    .isInstanceOf<MessageListState.LoadedMessages>()
                    .transform { state -> state.messages.filter { it.active }.map { it.id } }
                    .containsExactly(messages[3].id)

                expectNoEvents()
            }
        }

    @Test
    fun `process() should update the sender styles of the clicked message when event is OnMessageClick`() =
        runTest {
            // Arrange
            val messages = createMessageUiItemList(size = 3) { index ->
                createMessageUiItem(
                    state = MessageItemUi.State.Unread,
                    id = "id$index",
                    senders = ComposedAddressUi(
                        displayName = "sender",
                        displayNameStyles = persistentListOf(ComposedAddressStyle.Bold(start = 0)),
                    ),
                )
            }
            val clickedMessage = messages[1]
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
                    .transform { state ->
                        state.messages.first { it.id == clickedMessage.id }.senders.displayNameStyles
                    }
                    .containsExactly(ComposedAddressStyle.Regular(start = 0))

                expectNoEvents()
            }
        }

    @Test
    fun `process() should not change the read state when event is SetMessageActive`() =
        runTest {
            // Arrange
            val messages = createMessageUiItemList(size = 3) { index ->
                createMessageUiItem(state = MessageItemUi.State.Unread, id = "id$index")
            }
            val stateMachine = createStateMachineOnLoadedState(messages = messages)
            advanceUntilIdle()

            stateMachine.currentState.test {
                // enforce correct state before acting.
                assertThat(expectMostRecentItem()).isInstanceOf<MessageListState.LoadedMessages>()

                // Act
                stateMachine.process(MessageItemEvent.SetMessageActive(messages[1]))

                // Assert
                assertThat(awaitItem())
                    .isInstanceOf<MessageListState.LoadedMessages>()
                    .transform { state -> state.messages.map { it.state } }
                    .containsExactly(MessageItemUi.State.Unread, MessageItemUi.State.Unread, MessageItemUi.State.Unread)

                expectNoEvents()
            }
        }

    @Test
    fun `process() should avoid unnecessary copies when event is OnMessageClick`() =
        runTest {
            // Arrange
            val messages = createMessageUiItemList(size = 5) { index ->
                createMessageUiItem(state = MessageItemUi.State.Unread, id = "id$index")
            }
            val stateMachine = createStateMachineOnLoadedState(messages = messages)
            advanceUntilIdle()

            stateMachine.currentState.test {
                // enforce correct state before acting.
                assertThat(expectMostRecentItem()).isInstanceOf<MessageListState.LoadedMessages>()
                stateMachine.process(MessageItemEvent.SetMessageActive(messages[1]))
                val before = awaitItem().messages

                // Act
                stateMachine.process(MessageItemEvent.OnMessageClick(messages[3]))

                // Assert
                val after = awaitItem().messages
                assertThat(listOf(0, 2, 4).map { index -> after[index] === before[index] })
                    .containsExactly(true, true, true)

                expectNoEvents()
            }
        }
    // endregion [LoadedMessages - active message tracking]
}
