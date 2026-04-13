package net.thunderbird.feature.mail.message.list.internal.ui.state.sideeffect.ui

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import dev.mokkery.spy
import dev.mokkery.verifySuspend
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.common.state.sideeffect.StateSideEffectHandler
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.feature.mail.message.list.internal.ui.state.sideeffect.BaseSideEffectHandlerTest
import net.thunderbird.feature.mail.message.list.ui.effect.MessageListEffect
import net.thunderbird.feature.mail.message.list.ui.event.MessageItemEvent
import net.thunderbird.feature.mail.message.list.ui.event.MessageListEvent
import net.thunderbird.feature.mail.message.list.ui.state.MessageListState

@Suppress("MaxLineLength")
class SetMessageActiveSideEffectTest : BaseSideEffectHandlerTest() {

    @Test
    fun `handle() should return Consumed when event is SetMessageActive, newState is LoadedMessages, and activeMessage is not null`() =
        runTest {
            // Arrange
            val message = createMessageItemUi()
            val testSubject = createTestSubject()

            // Act
            val result = testSubject.handle(
                event = MessageItemEvent.SetMessageActive(message),
                oldState = createLoadedMessagesState(),
                newState = createLoadedMessagesState(activeMessage = message),
            )

            // Assert
            assertThat(result).isEqualTo(StateSideEffectHandler.ConsumeResult.Consumed)
        }

    @Test
    fun `handle() should return Ignored when event is SetMessageActive, newState is LoadedMessages, but activeMessage is null`() =
        runTest {
            // Arrange
            val testSubject = createTestSubject()

            // Act
            val result = testSubject.handle(
                event = MessageItemEvent.SetMessageActive(null),
                oldState = createLoadedMessagesState(),
                newState = createLoadedMessagesState(activeMessage = null),
            )

            // Assert
            assertThat(result).isEqualTo(StateSideEffectHandler.ConsumeResult.Ignored)
        }

    @Test
    fun `handle() should return Ignored when event is SetMessageActive but newState is not LoadedMessages`() = runTest {
        // Arrange
        val message = createMessageItemUi()
        val testSubject = createTestSubject()

        // Act
        val result = testSubject.handle(
            event = MessageItemEvent.SetMessageActive(message),
            oldState = MessageListState.WarmingUp(),
            newState = MessageListState.WarmingUp(),
        )

        // Assert
        assertThat(result).isEqualTo(StateSideEffectHandler.ConsumeResult.Ignored)
    }

    @Test
    fun `handle() should return Ignored when event is not SetMessageActive`() = runTest {
        // Arrange
        val testSubject = createTestSubject()

        // Act
        val result = testSubject.handle(
            event = MessageListEvent.LoadConfigurations,
            oldState = createLoadedMessagesState(),
            newState = createLoadedMessagesState(),
        )

        // Assert
        assertThat(result).isEqualTo(StateSideEffectHandler.ConsumeResult.Ignored)
    }

    @Test
    fun `handle() should dispatch ScrollToMessage effect when activeMessage is not null`() = runTest {
        // Arrange
        val message = createMessageItemUi()
        val dispatchUiEffect = spy<suspend (MessageListEffect) -> Unit>(obj = {})
        val testSubject = createTestSubject(dispatchUiEffect = dispatchUiEffect)

        // Act
        testSubject.handle(
            event = MessageItemEvent.SetMessageActive(message),
            oldState = createLoadedMessagesState(),
            newState = createLoadedMessagesState(activeMessage = message),
        )

        // Assert
        verifySuspend { dispatchUiEffect(MessageListEffect.ScrollToMessage(message = message)) }
    }

    @Test
    fun `Factory should create a SetMessageActiveSideEffect instance`() = runTest {
        // Arrange
        val factory = SetMessageActiveSideEffect.Factory(logger = TestLogger())

        // Act
        val result = factory.create(scope = this, dispatch = {}, dispatchUiEffect = {})

        // Assert
        assertThat(result).isInstanceOf(SetMessageActiveSideEffect::class)
    }

    private fun createTestSubject(
        dispatch: suspend (MessageListEvent) -> Unit = {},
        dispatchUiEffect: suspend (MessageListEffect) -> Unit = {},
    ) = SetMessageActiveSideEffect(
        logger = TestLogger(),
        dispatch = dispatch,
        dispatchUiEffect = dispatchUiEffect,
    )
}
