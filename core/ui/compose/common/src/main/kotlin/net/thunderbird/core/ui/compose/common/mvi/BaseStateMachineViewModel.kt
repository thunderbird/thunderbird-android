package net.thunderbird.core.ui.compose.common.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.k9mail.core.ui.compose.common.mvi.UnidirectionalViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import net.thunderbird.core.common.state.StateMachine
import net.thunderbird.core.common.state.sideeffect.StateSideEffectHandler
import net.thunderbird.core.logging.Logger

/**
 * An abstract base ViewModel that implements [UnidirectionalViewModel] and provides a
 * MVI (Model-View-Intent) architecture based on a [StateMachine].
 *
 * This class serves as a bridge between the UI (View) and the business logic (Model),
 * which is encapsulated within a [StateMachine]. It receives user actions as [events][TEvent],
 * processes them through the state machine, and exposes the resulting [state][TState]
 * and one-time [UI side effects][TUiSideEffect] to the UI.
 *
 * @param TState The type that represents the state of the ViewModel. For example, the
 *  UI state of a screen.
 * @param TEvent The type that represents user actions that can be handled by the ViewModel.
 *  For example, a button click.
 * @param TUiSideEffect The type that represents one-time side effects that can occur
 *  in response to state changes. For example, a navigation event or showing a toast message.
 * @param logger The [Logger] instance for logging events and state changes.
 * @param sideEffectHandlersFactories A list of factories for creating [StateSideEffectHandler]s.
 *  These handlers can be used to trigger side effects in response to state transitions.
 */
abstract class BaseStateMachineViewModel<TState : Any, TEvent : Any, TUiSideEffect>(
    protected val logger: Logger,
    sideEffectHandlersFactories: List<StateSideEffectHandler.Factory<TState, TEvent>> = emptyList(),
) :
    ViewModel(),
    UnidirectionalViewModel<TState, TEvent, TUiSideEffect> {

    /**
     * The state machine responsible for managing the state transitions.
     *
     * It processes events and updates the [state] accordingly. Subclasses must provide
     * an implementation of this [net.thunderbird.core.common.state.StateMachine].
     */
    protected abstract val stateMachine: StateMachine<TState, TEvent>

    override val state: StateFlow<TState> get() = stateMachine.currentState

    private val _effect = MutableSharedFlow<TUiSideEffect>()
    override val effect: SharedFlow<TUiSideEffect> = _effect.asSharedFlow()
    private val sideEffectHandlers = sideEffectHandlersFactories.map { it.create(viewModelScope, ::event) }

    private val handledOneTimeEvents = mutableSetOf<TEvent>()

    /**
     * Emits a side effect to the UI.
     *
     * Side effects are events that should be consumed by the UI only once, such as showing
     * a toast message, navigating to another screen, or triggering a one-time animation.
     * This function should be called from within  the ViewModel to signal such an event to
     * the UI layer, which collects the `effect` flow.
     *
     * @param effect The [TUiSideEffect] to emit.
     */
    protected fun emitEffect(effect: TUiSideEffect) {
        viewModelScope.launch {
            _effect.emit(effect)
        }
    }

    /**
     * Ensures that one-time events are only handled once.
     *
     * When you can't ensure that an event is only sent once, but you want the event to only
     * be handled once, call this method. It will ensure [block] is only executed the first
     * time this function is called. Subsequent calls with an [event] argument equal to that
     * of a previous invocation will not execute [block].
     *
     * Multiple one-time events are supported.
     */
    protected fun handleOneTimeEvent(event: TEvent, block: () -> Unit) {
        if (event !in handledOneTimeEvents) {
            handledOneTimeEvents.add(event)
            block()
        }
    }

    /**
     * Processes an event by launching a coroutine in the [viewModelScope] to delegate it to
     * the [stateMachine].
     *
     * This function is the entry point for all user actions or other events that can modify
     * the ViewModel's state.
     *
     * It ensures that events are processed asynchronously off the main thread.
     *
     * @param event The [TEvent] to be processed.
     */
    final override fun event(event: TEvent) {
        viewModelScope.launch {
            val currentState = stateMachine.currentStateSnapshot
            val newState = stateMachine.process(event)
            if (newState != currentState) {
                logger.verbose { "event(${event::class.simpleName}): state update." }
                sideEffectHandlers
                    .filter { it.accept(event, newState) }
                    .forEach { it.handle(event, oldState = currentState, newState) }
            }
            stateMachine.process(event)
        }
    }
}
