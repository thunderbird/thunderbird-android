package net.thunderbird.feature.mail.message.list.internal.ui.state.sideeffect.legacy

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import dev.mokkery.spy
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.common.state.sideeffect.StateSideEffectHandler
import net.thunderbird.core.logging.LogLevel
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.feature.mail.message.list.internal.ui.state.sideeffect.BaseSideEffectHandlerTest
import net.thunderbird.feature.mail.message.list.preferences.MessageListPreferences
import net.thunderbird.feature.mail.message.list.ui.effect.MessageListEffect
import net.thunderbird.feature.mail.message.list.ui.event.MessageListEvent
import net.thunderbird.feature.mail.message.list.ui.legacy.LegacyMessageListBridge
import net.thunderbird.feature.mail.message.list.ui.state.MessageItemUi
import net.thunderbird.feature.mail.message.list.ui.state.MessageListMetadata
import net.thunderbird.feature.mail.message.list.ui.state.MessageListState
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoadMessagesLegacySideEffectTest : BaseSideEffectHandlerTest() {

    @Test
    fun `handle() should return Consumed when newState is LoadingMessages`() = runTest {
        // Arrange
        val testSubject = createTestSubject(scope = backgroundScope)

        // Act
        val actual = testSubject.handle(
            event = MessageListEvent.LoadConfigurations,
            oldState = MessageListState.WarmingUp(),
            newState = createLoadingMessagesState(),
        )

        // Assert
        assertThat(actual)
            .isEqualTo(StateSideEffectHandler.ConsumeResult.Consumed)
    }

    @Test
    fun `handle() should return ignored when newState is WarmingUp`() = runTest {
        // Arrange
        val testSubject = createTestSubject(scope = backgroundScope)

        // Act
        val actual = testSubject.handle(
            event = MessageListEvent.LoadConfigurations,
            oldState = MessageListState.WarmingUp(),
            newState = createReadyWarmingUpState(),
        )

        // Assert
        assertThat(actual)
            .isEqualTo(StateSideEffectHandler.ConsumeResult.Ignored)
    }

    @Test
    fun `handle() should return ignored when newState is LoadedMessages`() = runTest {
        // Arrange
        val testSubject = createTestSubject(scope = backgroundScope)

        // Act
        val actual = testSubject.handle(
            event = MessageListEvent.LoadConfigurations,
            oldState = MessageListState.WarmingUp(),
            newState = MessageListState.LoadedMessages(
                metadata = createMetadata(),
                preferences = createMessageListPreferences(),
                messages = persistentListOf(),
            ),
        )

        // Assert
        assertThat(actual)
            .isEqualTo(StateSideEffectHandler.ConsumeResult.Ignored)
    }

    @Test
    fun `handle() should dispatch UpdateLoadingProgress with progress 1f for non-empty messages`() =
        runTest(UnconfinedTestDispatcher()) {
            // Arrange
            val dispatch = spy<suspend (MessageListEvent) -> Unit>(obj = {})
            val fakeMessages = listOf(createMessageItemUi())
            val fakeBridge = FakeLegacyMessageListBridge(flow { emit(fakeMessages) })
            val testSubject = createTestSubject(
                scope = backgroundScope,
                dispatch = dispatch,
                legacyBridge = fakeBridge,
            )
            val oldState = MessageListState.WarmingUp()
            val newState = createLoadingMessagesState()

            // Act
            testSubject.handle(event = MessageListEvent.LoadNextPage, oldState, newState)

            // Assert
            verifySuspend(mode = VerifyMode.exactly(1)) {
                dispatch(MessageListEvent.UpdateLoadingProgress(progress = 1f, messages = fakeMessages))
            }
        }

    @Test
    fun `handle() should dispatch UpdateLoadingProgress with progress 0f for empty messages`() =
        runTest(UnconfinedTestDispatcher()) {
            // Arrange
            val dispatch = spy<suspend (MessageListEvent) -> Unit>(obj = {})
            val fakeBridge = FakeLegacyMessageListBridge(flow { emit(emptyList()) })
            val testSubject = createTestSubject(
                scope = backgroundScope,
                dispatch = dispatch,
                legacyBridge = fakeBridge,
            )
            val oldState = MessageListState.WarmingUp()
            val newState = createLoadingMessagesState()

            // Act
            testSubject.handle(event = MessageListEvent.LoadNextPage, oldState, newState)

            // Assert
            verifySuspend(mode = VerifyMode.exactly(1)) {
                dispatch(MessageListEvent.UpdateLoadingProgress(progress = 0f, messages = emptyList()))
            }
        }

    @Test
    fun `handle() should do nothing when newState is not LoadingMessages`() =
        runTest(UnconfinedTestDispatcher()) {
            // Arrange
            val dispatch = spy<suspend (MessageListEvent) -> Unit>(obj = {})
            val fakeBridge = FakeLegacyMessageListBridge()
            val testSubject = createTestSubject(
                scope = backgroundScope,
                dispatch = dispatch,
                legacyBridge = fakeBridge,
            )
            val oldState = MessageListState.WarmingUp()
            val newState = MessageListState.WarmingUp()

            // Act
            testSubject.handle(event = MessageListEvent.LoadNextPage, oldState, newState)

            // Assert
            verifySuspend(mode = VerifyMode.exactly(0)) {
                dispatch(MessageListEvent.UpdateLoadingProgress(progress = 0f))
            }
            assertThat(fakeBridge.loadMessagesCallCount).isEqualTo(0)
        }

    @Test
    fun `handle() should cancel previous running job when called again`() =
        runTest(UnconfinedTestDispatcher()) {
            // Arrange
            val dispatchedEvents = mutableListOf<MessageListEvent>()
            val firstFlow = MutableSharedFlow<List<MessageItemUi>>()
            val secondMessages = listOf(createMessageItemUi(id = "second"))
            val secondFlow = flow { emit(secondMessages) }
            var callCount = 0
            val fakeBridge = FakeLegacyMessageListBridge(
                flowProvider = {
                    callCount++
                    if (callCount == 1) firstFlow else secondFlow
                },
            )
            val testSubject = createTestSubject(
                scope = backgroundScope,
                dispatch = { dispatchedEvents.add(it) },
                legacyBridge = fakeBridge,
            )
            val oldState = MessageListState.WarmingUp()
            val newState = createLoadingMessagesState()

            // Act
            testSubject.handle(
                event = MessageListEvent.LoadNextPage,
                oldState,
                newState,
            ) // first call - flow stays open
            testSubject.handle(
                event = MessageListEvent.LoadNextPage,
                oldState,
                newState,
            ) // second call - cancels first, completes immediately

            // Try to emit on the first flow (should be cancelled)
            firstFlow.emit(listOf(createMessageItemUi(id = "first")))
            advanceUntilIdle()

            // Assert - only the second flow's emission should have been dispatched
            assertThat(dispatchedEvents).hasSize(1)
            assertThat(dispatchedEvents[0]).isEqualTo(
                MessageListEvent.UpdateLoadingProgress(progress = 1f, messages = secondMessages),
            )
        }

    @Test
    fun `handle() should log error when flow throws exception`() =
        runTest(UnconfinedTestDispatcher()) {
            // Arrange
            val testLogger = TestLogger()
            val exception = RuntimeException("test error")
            val fakeBridge = FakeLegacyMessageListBridge(
                flow { throw exception },
            )
            val testSubject = createTestSubject(
                scope = backgroundScope,
                logger = testLogger,
                legacyBridge = fakeBridge,
            )
            val oldState = MessageListState.WarmingUp()
            val newState = createLoadingMessagesState()

            // Act
            testSubject.handle(event = MessageListEvent.LoadNextPage, oldState, newState)

            // Assert
            val errorEvents = testLogger.events.filter { it.level == LogLevel.ERROR }
            assertThat(errorEvents).hasSize(1)
            assertThat(errorEvents[0]).prop("tag") { it.tag }
                .isEqualTo("LoadMessagesLegacySideEffect")
        }

    @Test
    fun `handle() should dispatch multiple updates when flow emits multiple times`() =
        runTest(UnconfinedTestDispatcher()) {
            // Arrange
            val dispatchedEvents = mutableListOf<MessageListEvent>()
            val sharedFlow = MutableSharedFlow<List<MessageItemUi>>()
            val fakeBridge = FakeLegacyMessageListBridge(sharedFlow)
            val testSubject = createTestSubject(
                scope = backgroundScope,
                dispatch = { dispatchedEvents.add(it) },
                legacyBridge = fakeBridge,
            )
            val oldState = MessageListState.WarmingUp()
            val newState = createLoadingMessagesState()

            // Act
            testSubject.handle(event = MessageListEvent.LoadNextPage, oldState, newState)
            sharedFlow.emit(emptyList())
            sharedFlow.emit(listOf(createMessageItemUi()))
            sharedFlow.emit(listOf(createMessageItemUi(), createMessageItemUi(id = "2")))

            // Assert
            assertThat(dispatchedEvents).hasSize(3)
            assertThat(dispatchedEvents[0])
                .isEqualTo(MessageListEvent.UpdateLoadingProgress(progress = 0f, messages = emptyList()))
            assertThat(dispatchedEvents[1].let { it as MessageListEvent.UpdateLoadingProgress }.progress)
                .isEqualTo(1f)
            assertThat(dispatchedEvents[2].let { it as MessageListEvent.UpdateLoadingProgress }.progress)
                .isEqualTo(1f)
        }

    @Test
    fun `handle() should work correctly after previous flow completes`() =
        runTest(UnconfinedTestDispatcher()) {
            // Arrange
            val dispatchedEvents = mutableListOf<MessageListEvent>()
            val firstMessages = listOf(createMessageItemUi(id = "first"))
            val secondMessages = listOf(createMessageItemUi(id = "second"))
            var callCount = 0
            val fakeBridge = FakeLegacyMessageListBridge(
                flowProvider = {
                    callCount++
                    if (callCount == 1) flow { emit(firstMessages) } else flow { emit(secondMessages) }
                },
            )
            val testSubject = createTestSubject(
                scope = backgroundScope,
                dispatch = { dispatchedEvents.add(it) },
                legacyBridge = fakeBridge,
            )
            val oldState = MessageListState.WarmingUp()
            val newState = createLoadingMessagesState()

            // Act
            testSubject.handle(event = MessageListEvent.LoadNextPage, oldState, newState) // first call - completes
            advanceUntilIdle()
            testSubject.handle(
                event = MessageListEvent.LoadNextPage,
                oldState,
                newState,
            ) // second call - should work fine
            advanceUntilIdle()

            // Assert
            assertThat(dispatchedEvents).hasSize(2)
            assertThat(dispatchedEvents[0]).isEqualTo(
                MessageListEvent.UpdateLoadingProgress(progress = 1f, messages = firstMessages),
            )
            assertThat(dispatchedEvents[1]).isEqualTo(
                MessageListEvent.UpdateLoadingProgress(progress = 1f, messages = secondMessages),
            )
        }

    @Test
    fun `Factory should create a LoadMessagesLegacySideEffect instance`() = runTest {
        // Arrange
        val factory = LoadMessagesLegacySideEffect.Factory(
            logger = TestLogger(),
            legacyBridge = FakeLegacyMessageListBridge(),
        )

        // Act
        val result = factory.create(scope = backgroundScope, dispatch = {}, dispatchUiEffect = {})

        // Assert
        assertThat(result)
            .isInstanceOf<StateSideEffectHandler<MessageListState, MessageListEvent, MessageListEffect>>()
    }

    private fun createTestSubject(
        scope: kotlinx.coroutines.CoroutineScope,
        logger: TestLogger = TestLogger(),
        legacyBridge: LegacyMessageListBridge = FakeLegacyMessageListBridge(),
        dispatch: suspend (MessageListEvent) -> Unit = {},
    ) = LoadMessagesLegacySideEffect(
        scope = scope,
        logger = logger,
        legacyBridge = legacyBridge,
        dispatch = dispatch,
    )
}

private class FakeLegacyMessageListBridge(
    private val flowToReturn: Flow<List<MessageItemUi>> = MutableSharedFlow(),
) : LegacyMessageListBridge {
    var loadMessagesCallCount = 0
        private set

    constructor(flowProvider: () -> Flow<List<MessageItemUi>>) : this() {
        this.flowProvider = flowProvider
    }

    private var flowProvider: (() -> Flow<List<MessageItemUi>>)? = null

    override fun loadMessages(
        preferences: MessageListPreferences,
        metadata: MessageListMetadata,
    ): Flow<List<MessageItemUi>> {
        loadMessagesCallCount++
        return flowProvider?.invoke() ?: flowToReturn
    }
}
