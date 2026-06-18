package net.thunderbird.feature.mail.message.list.internal.ui.state.sideeffect.ui

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import dev.mokkery.spy
import dev.mokkery.verifySuspend
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.common.state.sideeffect.StateSideEffectHandler
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.feature.mail.message.list.internal.ui.state.sideeffect.BaseSideEffectHandlerTest
import net.thunderbird.feature.mail.message.list.ui.effect.MessageListEffect
import net.thunderbird.feature.mail.message.list.ui.event.MessageItemEvent
import net.thunderbird.feature.mail.message.list.ui.event.MessageListEvent
import net.thunderbird.feature.mail.message.list.ui.state.MessageListState

class OpenMessageSideEffectTest : BaseSideEffectHandlerTest() {

    @Test
    fun `handle() should return Consumed when event is OnMessageClick and newState is LoadedMessages`() = runTest {
        // Arrange
        val message = createMessageItemUi()
        val testSubject = createTestSubject()

        // Act
        val result = testSubject.handle(
            event = MessageItemEvent.OnMessageClick(message),
            oldState = createLoadedMessagesState(),
            newState = createLoadedMessagesState(activeMessage = message),
        )

        // Assert
        assertThat(result).isEqualTo(StateSideEffectHandler.ConsumeResult.Consumed)
    }

    @Test
    fun `handle() should return Ignored when event is OnMessageClick but newState is SelectingMessages`() = runTest {
        // Arrange
        val message = createMessageItemUi()
        val testSubject = createTestSubject()

        // Act
        val result = testSubject.handle(
            event = MessageItemEvent.OnMessageClick(message),
            oldState = createLoadedMessagesState(),
            newState = MessageListState.SelectingMessages(
                metadata = createReadyMetadata(),
                preferences = createMessageListPreferences(),
                messages = persistentListOf(),
            ),
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
            oldState = createLoadedMessagesState(),
            newState = createLoadedMessagesState(),
        )

        // Assert
        assertThat(result).isEqualTo(StateSideEffectHandler.ConsumeResult.Ignored)
    }

    @Test
    fun `handle() should dispatch OpenMessage effect with active message`() = runTest {
        // Arrange
        val message = createMessageItemUi()
        val dispatchUiEffect = spy<suspend (MessageListEffect) -> Unit>(obj = {})
        val testSubject = createTestSubject(dispatchUiEffect = dispatchUiEffect)

        // Act
        testSubject.handle(
            event = MessageItemEvent.OnMessageClick(message),
            oldState = createLoadedMessagesState(),
            newState = createLoadedMessagesState(activeMessage = message),
        )

        // Assert
        verifySuspend { dispatchUiEffect(MessageListEffect.OpenMessage(message = message)) }
    }

    @Test
    fun `handle() should throw when activeMessage is null`() = runTest {
        // Arrange
        val message = createMessageItemUi()
        val testSubject = createTestSubject()

        // Act & Assert
        assertFailsWith<IllegalArgumentException> {
            testSubject.handle(
                event = MessageItemEvent.OnMessageClick(message),
                oldState = createLoadedMessagesState(),
                newState = createLoadedMessagesState(activeMessage = null),
            )
        }
    }

    @Test
    fun `Factory should create an OpenMessageSideEffect instance`() = runTest {
        // Arrange
        val factory = OpenMessageSideEffect.Factory(logger = TestLogger())

        // Act
        val result = factory.create(scope = this, dispatch = {}, dispatchUiEffect = {})

        // Assert
        assertThat(result).isInstanceOf(OpenMessageSideEffect::class)
    }

    private fun createTestSubject(
        dispatch: suspend (MessageListEvent) -> Unit = {},
        dispatchUiEffect: suspend (MessageListEffect) -> Unit = {},
    ) = OpenMessageSideEffect(
        logger = TestLogger(),
        dispatch = dispatch,
        dispatchUiEffect = dispatchUiEffect,
    )
}
