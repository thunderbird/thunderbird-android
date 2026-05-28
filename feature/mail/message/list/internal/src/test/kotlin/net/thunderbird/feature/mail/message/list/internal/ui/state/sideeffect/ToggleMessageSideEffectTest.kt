package net.thunderbird.feature.mail.message.list.internal.ui.state.sideeffect

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import dev.mokkery.spy
import dev.mokkery.verifySuspend
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.common.state.sideeffect.StateSideEffectHandler
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.feature.mail.message.list.internal.ui.state.sideeffect.ui.ToggleMessageSideEffect
import net.thunderbird.feature.mail.message.list.ui.event.MessageItemEvent
import net.thunderbird.feature.mail.message.list.ui.event.MessageListEvent

class ToggleMessageSideEffectTest : BaseSideEffectHandlerTest() {

    @Test
    fun `handle() should return Consumed when event is OnMessageClick and both states are SelectingMessages`() =
        runTest {
            // Arrange
            val message = createMessageItemUi()
            val testSubject = createTestSubject()

            // Act
            val result = testSubject.handle(
                event = MessageItemEvent.OnMessageClick(message),
                oldState = createSelectingMessagesState(),
                newState = createSelectingMessagesState(),
            )

            // Assert
            assertThat(result).isEqualTo(StateSideEffectHandler.ConsumeResult.Consumed)
        }

    @Test
    fun `handle() should return Ignored when event is OnMessageClick but oldState is LoadedMessages`() = runTest {
        // Arrange
        val message = createMessageItemUi()
        val testSubject = createTestSubject()

        // Act
        val result = testSubject.handle(
            event = MessageItemEvent.OnMessageClick(message),
            oldState = createLoadedMessagesState(),
            newState = createSelectingMessagesState(),
        )

        // Assert
        assertThat(result).isEqualTo(StateSideEffectHandler.ConsumeResult.Ignored)
    }

    @Test
    fun `handle() should return Ignored when event is OnMessageClick but newState is LoadedMessages`() = runTest {
        // Arrange
        val message = createMessageItemUi()
        val testSubject = createTestSubject()

        // Act
        val result = testSubject.handle(
            event = MessageItemEvent.OnMessageClick(message),
            oldState = createSelectingMessagesState(),
            newState = createLoadedMessagesState(),
        )

        // Assert
        assertThat(result).isEqualTo(StateSideEffectHandler.ConsumeResult.Ignored)
    }

    @Test
    fun `handle() should return Ignored when event is not OnMessageClick`() = runTest {
        // Arrange
        val testSubject = createTestSubject()

        // Act
        val result = testSubject.handle(
            event = MessageListEvent.EnterSelectionMode,
            oldState = createSelectingMessagesState(),
            newState = createSelectingMessagesState(),
        )

        // Assert
        assertThat(result).isEqualTo(StateSideEffectHandler.ConsumeResult.Ignored)
    }

    @Test
    fun `handle() should dispatch ToggleSelectMessages with clicked message`() = runTest {
        // Arrange
        val message = createMessageItemUi()
        val dispatch = spy<suspend (MessageListEvent) -> Unit>(obj = {})
        val testSubject = createTestSubject(dispatch = dispatch)

        // Act
        testSubject.handle(
            event = MessageItemEvent.OnMessageClick(message),
            oldState = createSelectingMessagesState(),
            newState = createSelectingMessagesState(),
        )

        // Assert
        verifySuspend { dispatch(MessageItemEvent.ToggleSelectMessages(message)) }
    }

    @Test
    fun `Factory should create a ToggleMessageSideEffect instance`() = runTest {
        // Arrange
        val factory = ToggleMessageSideEffect.Factory(logger = TestLogger())

        // Act
        val result = factory.create(scope = this, dispatch = {}, dispatchUiEffect = {})

        // Assert
        assertThat(result).isInstanceOf(ToggleMessageSideEffect::class)
    }

    private fun createTestSubject(
        dispatch: suspend (MessageListEvent) -> Unit = {},
    ) = ToggleMessageSideEffect(
        logger = TestLogger(),
        dispatch = dispatch,
    )
}
