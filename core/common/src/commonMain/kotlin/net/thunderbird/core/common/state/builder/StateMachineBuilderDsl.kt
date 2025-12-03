package net.thunderbird.core.common.state.builder

import kotlinx.coroutines.CoroutineScope
import net.thunderbird.core.common.state.StateMachine

@DslMarker
annotation class StateMachineBuilderDsl

/**
 * Creates a [StateMachine] using the type-safe builder DSL.
 *
 * This function serves as the entry point for defining a state machine. It initializes a
 * [StateMachineBuilder] and applies the provided configuration block [init] to it.
 *
 * Example usage:
 * ```kotlin
 * sealed class LightState {
 *     data class On(val brightness: Int) : LightState()
 *     data object Off : LightState()
 * }
 *
 * sealed class LightEvent {
 *     data object TurnOn : LightEvent()
 *     data object TurnOff : LightEvent()
 *     data class SetBrightness(val brightness: Int) : LightEvent()
 * }
 *
 * val lightStateMachine = stateMachine<LightState, LightEvent> {
 *     initialState(LightState.Off) {
 *         transition<LightEvent.TurnOn> { _, _ -> LightState.On }
 *     }
 *
 *     state<LightState.On> {
 *         transition<LightEvent.TurnOff> { _, _ -> LightState.Off }
 *         transition<LightEvent.SetBrightness> { currentState, event ->
 *             currentState.copy(brightness = brightness, event.brightness)
 *         }
 *     }
 * }
 * ```
 *
 * @param TState The base type for all states in the machine. Must be a non-nullable type.
 * @param TEvent The base type for all events that can be processed by the machine. Must be a non-nullable type.
 * @param init A lambda with a receiver of type [StateMachineBuilder] where the state machine's
 *             structure (states, transitions, etc.) is defined.
 * @return The configured and built [StateMachine] instance, ready to process events.
 */
@StateMachineBuilderDsl
fun <TState : Any, TEvent : Any> stateMachine(
    scope: CoroutineScope,
    init: StateMachineBuilder<TState, TEvent>.() -> Unit,
): StateMachine<TState, TEvent> = StateMachineBuilder<TState, TEvent>(scope).apply(init).build()
