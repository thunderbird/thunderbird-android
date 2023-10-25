package app.k9mail.core.ui.compose.common.mvi

import app.cash.turbine.test
import app.k9mail.core.ui.compose.testing.MainDispatcherRule
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class BaseViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `should emit initial state`() = runTest {
        val viewModel = TestBaseViewModel()
        assertThat(viewModel.state.value).isEqualTo("Initial state")
    }

    @Test
    fun `should update state`() = runTest {
        val viewModel = TestBaseViewModel()

        viewModel.event("Test event")

        assertThat(viewModel.state.value).isEqualTo("Test event")

        viewModel.event("Another test event")

        assertThat(viewModel.state.value).isEqualTo("Another test event")
    }

    @Test
    fun `should emit effects`() = runTest {
        val viewModel = TestBaseViewModel()

        viewModel.effect.test {
            viewModel.event("Test effect")

            assertThat(awaitItem()).isEqualTo("Test effect")

            viewModel.event("Another test effect")

            assertThat(awaitItem()).isEqualTo("Another test effect")
        }
    }

    @Test
    fun `handleOneTimeEvent() should execute block`() = runTest {
        val viewModel = TestBaseViewModel()
        var eventHandled = false

        viewModel.callHandleOneTimeEvent(event = "event") {
            eventHandled = true
        }

        assertThat(eventHandled).isTrue()
    }

    @Test
    fun `handleOneTimeEvent() should execute block only once`() = runTest {
        val viewModel = TestBaseViewModel()
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
        val viewModel = TestBaseViewModel()
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

    private class TestBaseViewModel : BaseViewModel<String, String, String>("Initial state") {
        override fun event(event: String) {
            updateState { event }
            emitEffect(event)
        }

        fun callHandleOneTimeEvent(event: String, block: () -> Unit) {
            handleOneTimeEvent(event, block)
        }
    }
}
