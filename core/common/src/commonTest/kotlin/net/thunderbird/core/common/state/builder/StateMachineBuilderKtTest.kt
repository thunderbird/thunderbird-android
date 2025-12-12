package net.thunderbird.core.common.state.builder

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.common.state.DefaultStateMachine
import org.junit.Test

class StateMachineBuilderKtTest {
    sealed interface State {
        data object Init : State
        data object Loading : State
        data object Success : State
        data object Error : State
    }

    sealed interface Event {
        data object LoadData : Event
        data object Retry : Event
        data object Cancel : Event
        data object Failure : Event
    }

    @Test
    fun `stateMachine should create state machine successfully`() = runTest {
        // Arrange & Act
        val machine = stateMachine(scope = this) {
            initialState(State.Init) {
                transition<Event.LoadData> { _, _ -> State.Loading }
                transition<Event.Failure> { _, _ -> State.Error }
            }
            state<State.Loading> {
                transition<Event.Cancel> { _, _ -> State.Init }
                transition<Event.Retry> { _, _ -> State.Init }
                transition<Event.LoadData> { _, _ -> State.Success }
                transition<Event.Failure> { _, _ -> State.Success }
            }
            state<State.Error> {
                transition<Event.LoadData> { _, _ -> State.Loading }
            }
            finalState<State.Success>()
        }

        // Assert
        assertThat(machine.currentStateSnapshot).isEqualTo(State.Init)
    }

    @Test
    fun `stateMachine should throw exception when missing initial state`() = runTest {
        // Arrange & Act
        val exception = assertFailure {
            stateMachine(scope = this) {
                state<State.Init> {
                    transition<Event.LoadData> { _, _ -> State.Loading }
                    transition<Event.Failure> { _, _ -> State.Error }
                }
                state<State.Loading> {
                    transition<Event.Cancel> { _, _ -> State.Init }
                    transition<Event.Retry> { _, _ -> State.Init }
                    transition<Event.LoadData> { _, _ -> State.Success }
                    transition<Event.Failure> { _, _ -> State.Success }
                }
                state<State.Error> {
                    transition<Event.LoadData> { _, _ -> State.Loading }
                }
                finalState<State.Success>()
            }
        }

        // Assert
        exception
            .isInstanceOf(IllegalStateException::class)
            .hasMessage("Initial state is required.")
    }

    @Test
    fun `stateMachine should throw exception when initial state is also registered as a state`() = runTest {
        // Arrange & Act
        val exception = assertFailure {
            stateMachine(scope = this) {
                initialState(State.Init) {
                    transition<Event.LoadData> { _, _ -> State.Loading }
                    transition<Event.Failure> { _, _ -> State.Error }
                }
                state<State.Init> {}
                state<State.Loading> {
                    transition<Event.Cancel> { _, _ -> State.Init }
                    transition<Event.Retry> { _, _ -> State.Init }
                    transition<Event.LoadData> { _, _ -> State.Success }
                    transition<Event.Failure> { _, _ -> State.Success }
                }
                state<State.Error> {
                    transition<Event.LoadData> { _, _ -> State.Loading }
                }
                finalState<State.Success>()
            }
        }

        // Assert
        exception
            .isInstanceOf(IllegalStateException::class)
            .hasMessage("${State.Init::class.simpleName} is already registered as a state.")
    }

    @Test
    fun `stateMachine should throw exception when state is already registered`() = runTest {
        // Arrange & Act
        val exception = assertFailure {
            stateMachine(scope = this) {
                initialState(State.Init) {
                    transition<Event.LoadData> { _, _ -> State.Loading }
                    transition<Event.Failure> { _, _ -> State.Error }
                }
                state<State.Loading> {
                    transition<Event.Cancel> { _, _ -> State.Init }
                    transition<Event.Retry> { _, _ -> State.Init }
                }
                state<State.Loading> {
                    transition<Event.LoadData> { _, _ -> State.Success }
                    transition<Event.Failure> { _, _ -> State.Success }
                }
                state<State.Error> {
                    transition<Event.LoadData> { _, _ -> State.Loading }
                }
                finalState<State.Success>()
            }
        }

        // Assert
        exception
            .isInstanceOf(IllegalStateException::class)
            .hasMessage("${State.Loading::class.simpleName} is already registered as a state.")
    }

    @Test
    fun `stateMachine should throw exception when no transitions are defined`() = runTest {
        // Arrange & Act
        val exception = assertFailure {
            stateMachine<State, Event>(scope = this) {
                initialState(State.Init) {}
                state<State.Loading> {}
                state<State.Success> {}
                state<State.Error> {}
            }
        }

        // Assert
        exception
            .isInstanceOf(IllegalStateException::class)
            .hasMessage("At least one transition must be defined.")
    }

    @Test
    fun `stateMachine should throw exception when only the initial state is defined`() = runTest {
        // Arrange & Act
        val exception = assertFailure {
            stateMachine<State, Event>(scope = this) {
                initialState(State.Init) {}
            }
        }

        // Assert
        exception
            .isInstanceOf(IllegalStateException::class)
            .hasMessage("At least two states must be defined.")
    }

    @Test
    fun `stateMachine should throw exception when a state has no transitions and it isn't the final state`() = runTest {
        // Arrange & Act
        val exception = assertFailure {
            stateMachine(scope = this) {
                initialState(State.Init) {
                    transition<Event.LoadData> { _, _ -> State.Loading }
                    transition<Event.Failure> { _, _ -> State.Error }
                }
                state<State.Loading> {
                    transition<Event.Cancel> { _, _ -> State.Init }
                    transition<Event.Retry> { _, _ -> State.Init }
                    transition<Event.LoadData> { _, _ -> State.Success }
                    transition<Event.Failure> { _, _ -> State.Success }
                }
                state<State.Error> { }
                state<State.Success> {}
            }
        }

        // Assert
        exception
            .isInstanceOf(IllegalStateException::class)
            .hasMessage(
                "Only the final states can have no transitions. States without transaction: [ Error, Success ]",
            )
    }

    @Test
    fun `stateMachine - initialState should allow setting onEntry listener`() = runTest {
        // Arrange & Act
        val machine = stateMachine<State, Event>(scope = this) {
            initialState(State.Init) {
                onEnter { _, _ -> println("No op.") }
                transition<Event.LoadData> { _, _ -> State.Loading }
                transition<Event.Failure> { _, _ -> State.Error }
            }
            state<State.Loading> {
                transition<Event.Cancel> { _, _ -> State.Init }
                transition<Event.Retry> { _, _ -> State.Init }
                transition<Event.LoadData> { _, _ -> State.Success }
                transition<Event.Failure> { _, _ -> State.Success }
            }
            state<State.Error> {
                transition<Event.LoadData> { _, _ -> State.Loading }
            }
            finalState<State.Success>()
        } as DefaultStateMachine

        // Assert
        assertThat(machine.currentStateSnapshot).isEqualTo(State.Init)
        assertThat(machine.stateRegistrar[State.Init::class])
            .isNotNull()
            .transform { it.listeners.onEnter }
            .isNotNull()
    }

    @Test
    fun `stateMachine - initialState should allow setting onExit listener`() = runTest {
        // Arrange & Act
        val machine = stateMachine(scope = this) {
            initialState(State.Init) {
                onExit { println("No op.") }
                transition<Event.LoadData> { _, _ -> State.Loading }
                transition<Event.Failure> { _, _ -> State.Error }
            }
            state<State.Loading> {
                transition<Event.Cancel> { _, _ -> State.Init }
                transition<Event.Retry> { _, _ -> State.Init }
                transition<Event.LoadData> { _, _ -> State.Success }
                transition<Event.Failure> { _, _ -> State.Success }
            }
            state<State.Error> {
                transition<Event.LoadData> { _, _ -> State.Loading }
            }
            finalState<State.Success>()
        } as DefaultStateMachine

        // Assert
        assertThat(machine.currentStateSnapshot).isEqualTo(State.Init)
        assertThat(machine.stateRegistrar[State.Init::class])
            .isNotNull()
            .transform { it.listeners.onExit }
            .isNotNull()
    }

    @Test
    fun `stateMachine - state should allow setting onEntry listener`() = runTest {
        // Arrange & Act
        val machine = stateMachine(scope = this) {
            initialState(State.Init) {
                transition<Event.LoadData> { _, _ -> State.Loading }
                transition<Event.Failure> { _, _ -> State.Error }
            }
            state<State.Loading> {
                onEnter { _, _ -> println("No op.") }
                transition<Event.Cancel> { _, _ -> State.Init }
                transition<Event.Retry> { _, _ -> State.Init }
                transition<Event.LoadData> { _, _ -> State.Success }
                transition<Event.Failure> { _, _ -> State.Success }
            }
            state<State.Error> {
                transition<Event.LoadData> { _, _ -> State.Loading }
            }
            finalState<State.Success>()
        } as DefaultStateMachine

        // Assert
        assertThat(machine.currentStateSnapshot).isEqualTo(State.Init)
        assertThat(machine.stateRegistrar[State.Loading::class])
            .isNotNull()
            .transform { it.listeners.onEnter }
            .isNotNull()
    }

    @Test
    fun `stateMachine - state should allow setting onExit listener`() = runTest {
        // Arrange & Act
        val machine = stateMachine(scope = this) {
            initialState(State.Init) {
                transition<Event.LoadData> { _, _ -> State.Loading }
                transition<Event.Failure> { _, _ -> State.Error }
            }
            state<State.Loading> {
                onExit { println("No op.") }
                transition<Event.Cancel> { _, _ -> State.Init }
                transition<Event.Retry> { _, _ -> State.Init }
                transition<Event.LoadData> { _, _ -> State.Success }
                transition<Event.Failure> { _, _ -> State.Success }
            }
            state<State.Error> {
                transition<Event.LoadData> { _, _ -> State.Loading }
            }
            finalState<State.Success>()
        } as DefaultStateMachine

        // Assert
        assertThat(machine.currentStateSnapshot).isEqualTo(State.Init)
        assertThat(machine.stateRegistrar[State.Loading::class])
            .isNotNull()
            .transform { it.listeners.onExit }
            .isNotNull()
    }

    @Test
    fun `stateMachine - finalState should allow setting onEntry listener`() = runTest {
        // Arrange & Act
        val machine = stateMachine(scope = this) {
            initialState(State.Init) {
                transition<Event.LoadData> { _, _ -> State.Loading }
                transition<Event.Failure> { _, _ -> State.Error }
            }
            state<State.Loading> {
                transition<Event.Cancel> { _, _ -> State.Init }
                transition<Event.Retry> { _, _ -> State.Init }
                transition<Event.LoadData> { _, _ -> State.Success }
                transition<Event.Failure> { _, _ -> State.Success }
            }
            state<State.Error> {
                transition<Event.LoadData> { _, _ -> State.Loading }
            }
            finalState<State.Success> {
                onEnter { _, _ -> println("No op.") }
            }
        } as DefaultStateMachine

        // Assert
        assertThat(machine.currentStateSnapshot).isEqualTo(State.Init)
        assertThat(machine.stateRegistrar[State.Success::class])
            .isNotNull()
            .transform { it.listeners.onEnter }
            .isNotNull()
    }

    @Test
    fun `stateMachine - finalState should allow setting onExit listener`() = runTest {
        // Arrange & Act
        val machine = stateMachine(scope = this) {
            initialState(State.Init) {
                transition<Event.LoadData> { _, _ -> State.Loading }
                transition<Event.Failure> { _, _ -> State.Error }
            }
            state<State.Loading> {
                transition<Event.Cancel> { _, _ -> State.Init }
                transition<Event.Retry> { _, _ -> State.Init }
                transition<Event.LoadData> { _, _ -> State.Success }
                transition<Event.Failure> { _, _ -> State.Success }
            }
            state<State.Error> {
                transition<Event.LoadData> { _, _ -> State.Loading }
            }
            finalState<State.Success> {
                onExit { println("No op.") }
            }
        } as DefaultStateMachine

        // Assert
        assertThat(machine.currentStateSnapshot).isEqualTo(State.Init)
        assertThat(machine.stateRegistrar[State.Success::class])
            .isNotNull()
            .transform { it.listeners.onExit }
            .isNotNull()
    }
}
