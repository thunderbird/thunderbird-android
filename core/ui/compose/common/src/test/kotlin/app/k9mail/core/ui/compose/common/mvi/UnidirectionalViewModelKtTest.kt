package app.k9mail.core.ui.compose.common.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.k9mail.core.ui.compose.testing.ComposeTest
import app.k9mail.core.ui.compose.testing.setContent
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.testing.coroutines.MainDispatcherRule
import org.junit.Rule
import org.junit.Test

class UnidirectionalViewModelKtTest : ComposeTest() {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `observe should emit state changes, allow event dispatch and expose effects`() = runTest {
        val viewModel = TestViewModel()
        val effects = mutableListOf<TestEffect>()
        lateinit var stateDispatch: StateDispatch<TestState, TestEvent>

        setContent {
            stateDispatch = viewModel.observe { effect ->
                effects.add(effect)
            }
        }

        val (state, dispatch) = stateDispatch

        // Initial state
        assertThat(state.value.data).isEqualTo("TestState: Initial")

        // Dispatch an event
        dispatch(TestEvent("Event 1"))

        assertThat(state.value.data).isEqualTo("TestState: Event 1")
        assertThat(effects.last().result).isEqualTo("TestEffect: Event 1")

        // Dispatch another event
        dispatch(TestEvent("Event 2"))

        assertThat(state.value.data).isEqualTo("TestState: Event 2")
        assertThat(effects.last().result).isEqualTo("TestEffect: Event 2")
    }

    private data class TestState(val data: String)
    private data class TestEvent(val action: String)
    private data class TestEffect(val result: String)

    private class TestViewModel(
        initialState: TestState = TestState("TestState: Initial"),
    ) : ViewModel(), UnidirectionalViewModel<TestState, TestEvent, TestEffect> {

        private val _state = MutableStateFlow(initialState)
        override val state: StateFlow<TestState> = _state.asStateFlow()

        private val _effect = MutableSharedFlow<TestEffect>()
        override val effect: SharedFlow<TestEffect> = _effect.asSharedFlow()

        override fun event(event: TestEvent) {
            _state.update { it.copy(data = "TestState: ${event.action}") }
            viewModelScope.launch {
                _effect.emit(TestEffect(result = "TestEffect: ${event.action}"))
            }
        }
    }
}
