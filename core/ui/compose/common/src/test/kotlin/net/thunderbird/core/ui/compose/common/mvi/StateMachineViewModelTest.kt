package net.thunderbird.core.ui.compose.common.mvi

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.common.state.StateMachine
import net.thunderbird.core.common.state.sideeffect.StateSideEffectHandler
import net.thunderbird.core.logging.Logger
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.core.testing.coroutines.MainDispatcherRule
import org.junit.Rule
import org.junit.Test

class StateMachineViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `should emit initial state`() = runTest {
        val initialState = "Initial state"
        val viewModel = TestStateMachineViewModel(
            initialState = initialState,
        )

        viewModel.state.test {
            assertThat(awaitItem()).isEqualTo(initialState)
        }
    }

    @Test
    fun `should update state`() = runTest {
        val viewModel = TestStateMachineViewModel(
            initialState = "Initial state",
        )

        viewModel.state.test {
            assertThat(awaitItem()).isEqualTo("Initial state")

            viewModel.event("Test event")

            assertThat(awaitItem()).isEqualTo("Test event")

            viewModel.event("Another test event")

            assertThat(awaitItem()).isEqualTo("Another test event")
        }
    }

    @Test
    fun `should emit effects`() = runTest {
        val viewModel = TestStateMachineViewModel(
            initialState = "Initial state",
        )

        viewModel.effect.test {
            viewModel.callEmitEffect("Test effect")

            assertThat(awaitItem()).isEqualTo("Test effect")

            viewModel.callEmitEffect("Another test effect")

            assertThat(awaitItem()).isEqualTo("Another test effect")
        }
    }

    @Test
    fun `handleOneTimeEvent() should execute block`() = runTest {
        val viewModel = TestStateMachineViewModel(
            initialState = "Initial state",
        )
        var eventHandled = false

        viewModel.callHandleOneTimeEvent(event = "event") {
            eventHandled = true
        }

        assertThat(eventHandled).isTrue()
    }

    @Test
    fun `handleOneTimeEvent() should execute block only once`() = runTest {
        val viewModel = TestStateMachineViewModel(
            initialState = "Initial state",
        )
        var eventHandledCount = 0

        repeat(2) {
            viewModel.callHandleOneTimeEvent(event = "event") {
                eventHandledCount++
            }
        }

        assertThat(eventHandledCount).isEqualTo(1)
    }

    @Test
    fun `handleOneTimeEvent() should support multiple one-time events`() = runTest {
        val viewModel = TestStateMachineViewModel(
            initialState = "Initial state",
        )
        var eventOneHandled = false
        var eventTwoHandled = false

        viewModel.callHandleOneTimeEvent(event = "eventOne") {
            eventOneHandled = true
        }

        assertThat(eventOneHandled).isTrue()
        assertThat(eventTwoHandled).isFalse()

        viewModel.callHandleOneTimeEvent(event = "eventTwo") {
            eventTwoHandled = true
        }

        assertThat(eventOneHandled).isTrue()
        assertThat(eventTwoHandled).isTrue()
    }

    @Test
    fun `should trigger side effect handlers on state change`() = runTest {
        var sideEffectTriggered = false
        val sideEffectHandler = object : StateSideEffectHandler<String, String>(TestLogger(), {}) {
            override fun accept(event: String, newState: String): Boolean = true
            override suspend fun handle(oldState: String, newState: String) {
                sideEffectTriggered = true
            }
        }
        val factory = StateSideEffectHandler.Factory { _, _ -> sideEffectHandler }

        val viewModel = TestStateMachineViewModel(
            initialState = "Initial state",
            sideEffectHandlersFactories = listOf(factory),
        )

        viewModel.event("Test event")
        viewModel.state.test {
            val state = awaitItem()
            assertThat(state).isEqualTo("Test event")
            assertThat(sideEffectTriggered).isTrue()
        }
    }

    private class TestStateMachineViewModel(
        initialState: String,
        logger: Logger = TestLogger(),
        override val stateMachine: StateMachine<String, String> = FakeStateMachine(initialState),
        sideEffectHandlersFactories: List<StateSideEffectHandler.Factory<String, String>> = emptyList(),
    ) : BaseStateMachineViewModel<String, String, String>(logger, sideEffectHandlersFactories) {

        fun callEmitEffect(effect: String) {
            emitEffect(effect)
        }

        fun callHandleOneTimeEvent(event: String, block: () -> Unit) {
            handleOneTimeEvent(event, block)
        }
    }

    private class FakeStateMachine(initialState: String) : StateMachine<String, String> {
        private val _currentState = MutableStateFlow(initialState)
        override val currentState: StateFlow<String> = _currentState.asStateFlow()

        override suspend fun process(event: String): String {
            _currentState.value = event
            return _currentState.value
        }
    }
}
