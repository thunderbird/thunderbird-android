package net.thunderbird.core.common.state

import app.cash.turbine.test
import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.common.state.builder.stateMachine

@Suppress("MaxLineLength")
class StateMachineTest {
    private sealed interface State {
        data object Init : State
        data object Loading : State
        data object Success : State
        data object Error : State
    }

    private sealed interface Event {
        data object LoadData : Event
        data object LoadedData : Event
        data class Retry(val forceRetry: Boolean) : Event
        data object Cancel : Event
        data object Failure : Event
    }

    @Test
    fun `onEntry from initialState should be called when state machine is created`() {
        // Arrange && Act
        var onEntryCalled = false
        stateMachine {
            initialState(State.Init) {
                onEnter { onEntryCalled = true }
                transition<Event.LoadData> { _, _ -> State.Loading }
            }
            state<State.Loading> {
                transition<Event.LoadedData> { _, _ -> State.Success }
            }
            finalState<State.Success>()
        }

        // Assert
        assertThat(onEntryCalled).isTrue()
    }

    @Test
    fun `process should return the new state when transition is defined and guard passes`() = runTest {
        // Arrange
        val stateMachine = stateMachine {
            initialState(State.Init) {
                transition<Event.LoadData> { _, _ -> State.Loading }
            }
            state<State.Loading> {
                transition<Event.LoadedData> { _, _ -> State.Success }
            }
            finalState<State.Success>()
        }

        stateMachine.currentState.test {
            // Skip initial state.
            skipItems(count = 1)
            // Act (Phase 1)
            stateMachine.process(Event.LoadData)

            // Assert (Phase 1)
            assertThat(awaitItem()).isEqualTo(State.Loading)

            // Act (Phase 2)
            stateMachine.process(Event.LoadedData)

            // Assert (Phase 2)
            assertThat(awaitItem()).isEqualTo(State.Success)
        }
    }

    @Test
    fun `process should return the current state when no transition between current state and even is defined`() =
        runTest {
            // Arrange
            val stateMachine = stateMachine {
                initialState(State.Init) {
                    transition<Event.LoadData> { _, _ -> State.Loading }
                }
                state<State.Loading> {
                    transition<Event.LoadedData> { _, _ -> State.Success }
                }
                finalState<State.Success>()
            }

            stateMachine.currentState.test {
                // Act
                stateMachine.process(Event.Cancel)

                // Assert
                assertThat(awaitItem()).isEqualTo(State.Init)
                expectNoEvents()
            }
        }

    @Test
    fun `process should return the current state when guard fails`() = runTest {
        // Arrange
        val stateMachine = stateMachine {
            initialState(State.Init) {
                transition<Event.LoadData> { _, _ -> State.Loading }
            }
            state<State.Loading> {
                transition<Event.LoadedData> { _, _ -> State.Success }
                transition<Event.Failure> { _, _ -> State.Error }
            }
            state<State.Error> {
                transition<Event.Retry>(
                    guard = { _, event -> event.forceRetry },
                ) { _, _ -> State.Loading }
            }
            finalState<State.Success>()
        }

        stateMachine.currentState.test {
            // Act (Phase 1)
            stateMachine.process(Event.LoadData)
            stateMachine.process(Event.Failure)
            skipItems(count = 2)

            // Assert (Phase 1)
            assertThat(awaitItem()).isEqualTo(State.Error)

            // Act (Phase 2)
            val state = stateMachine.process(Event.Retry(forceRetry = false))

            // Assert (Phase 2)
            assertThat(state).isEqualTo(State.Error)
            expectNoEvents()
        }
    }

    @Test
    fun `process should trigger onEnter(newState) and onExit(oldState) when state changes`() = runTest {
        // Arrange
        fun MutableMap<KClass<out State>, Int>.increment(stateClass: KClass<out State>) {
            this[stateClass] = getOrElse(stateClass) { 0 } + 1
        }

        val onEnterCalled = mutableMapOf<KClass<out State>, Int>()
        val onExitCalled = mutableMapOf<KClass<out State>, Int>()
        val stateMachine = stateMachine {
            initialState(State.Init) {
                onEnter { _ -> onEnterCalled.increment(stateClass) }
                onExit { _ -> onExitCalled.increment(stateClass) }
                transition<Event.LoadData> { _, _ -> State.Loading }
            }
            state<State.Loading> {
                onEnter { _ -> onEnterCalled.increment(stateClass) }
                onExit { _ -> onExitCalled.increment(stateClass) }
                transition<Event.LoadedData> { _, _ -> State.Success }
                transition<Event.Failure> { _, _ -> State.Error }
            }
            state<State.Error> {
                onEnter { _ -> onEnterCalled.increment(stateClass) }
                onExit { _ -> onExitCalled.increment(stateClass) }
                transition<Event.Retry>(
                    guard = { _, event -> event.forceRetry },
                ) { _, _ -> State.Loading }
            }
            finalState<State.Success> {
                onEnter { _ -> onEnterCalled.increment(stateClass) }
                onExit { _ -> onExitCalled.increment(stateClass) }
            }
        }

        // Act
        stateMachine.process(Event.LoadData)
        stateMachine.process(Event.Failure)
        stateMachine.process(Event.Retry(forceRetry = true))
        stateMachine.process(Event.LoadedData)

        assertThat(onEnterCalled).all {
            transform { it[State.Init::class] }.isEqualTo(1)
            transform { it[State.Loading::class] }.isEqualTo(2)
            transform { it[State.Error::class] }.isEqualTo(1)
            transform { it[State.Success::class] }.isEqualTo(1)
        }

        assertThat(onExitCalled).all {
            transform { it[State.Init::class] }.isEqualTo(1)
            transform { it[State.Loading::class] }.isEqualTo(2)
            transform { it[State.Error::class] }.isEqualTo(1)
            transform { it[State.Success::class] }.isEqualTo(1)
        }
    }

    @Test
    fun `process should not process events and return final state when state machine is in final state`() = runTest {
        // Arrange
        var successStateExited = 0
        val stateMachine = stateMachine {
            initialState(State.Init) {
                transition<Event.LoadData> { _, _ -> State.Loading }
            }
            state<State.Loading> {
                transition<Event.LoadedData> { _, _ -> State.Success }
            }
            finalState<State.Success> {
                onExit { successStateExited++ }
            }
        }

        // Act
        stateMachine.currentState.test {
            // Act (Phase 1)
            stateMachine.process(Event.LoadData)
            stateMachine.process(Event.LoadedData)

            // Assert (Phase 1)
            // Skip to the last state.
            skipItems(count = 2)
            assertThat(awaitItem()).isEqualTo(State.Success)

            // Act (Phase 2)
            stateMachine.process(Event.Cancel)

            // Assert (Phase 2)
            expectNoEvents()
            assertThat(successStateExited).isEqualTo(1)

            // Act (Phase 3)
            stateMachine.process(Event.Retry(forceRetry = false))

            // Assert (Phase 3)
            expectNoEvents()
            assertThat(successStateExited).isEqualTo(1)
        }
    }
}
