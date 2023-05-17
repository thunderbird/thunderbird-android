package app.k9mail.core.ui.compose.common.mvi

import app.cash.turbine.test
import app.k9mail.core.ui.compose.testing.MainDispatcherRule
import assertk.assertThat
import assertk.assertions.isEqualTo
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

    private class TestBaseViewModel : BaseViewModel<String, String, String>("Initial state") {
        override fun event(event: String) {
            updateState { event }
            emitEffect(event)
        }
    }
}
