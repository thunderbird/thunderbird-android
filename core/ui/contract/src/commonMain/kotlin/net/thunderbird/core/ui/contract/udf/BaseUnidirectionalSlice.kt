package net.thunderbird.core.ui.contract.udf

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * An abstract base Slice that implements [UnidirectionalSlice] and provides basic
 * functionality for managing state and effects.
 *
 * @param STATE The type that represents the state of the Slice. For example, the UI state of a screen can be
 * represented as a state.
 * @param EVENT The type that represents user actions that can occur and should be handled by the Slice. For
 * example, a button click can be represented as an event.
 * @param EFFECT The type that represents side-effects that can occur in response to the state changes. For example,
 * a navigation event can be represented as an effect.
 *
 * @param scope The [kotlinx.coroutines.CoroutineScope] in which to launch coroutines for emitting effects.
 * @param initialState The initial [STATE] of the Slice.
 */
public abstract class BaseUnidirectionalSlice<STATE, EVENT, EFFECT>(
    private val scope: CoroutineScope,
    initialState: STATE,
) : UnidirectionalSlice<STATE, EVENT, EFFECT> {

    private val _state = MutableStateFlow(initialState)
    override val state: StateFlow<STATE> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<EFFECT>()
    override val effect: SharedFlow<EFFECT> = _effect.asSharedFlow()

    /**
     * Updates the [STATE] of the Slice.
     *
     * @param update A function that takes the current [STATE] and produces a new [STATE].
     */
    protected fun updateState(update: (STATE) -> STATE) {
        _state.update(update)
    }

    protected fun updateStateAsync(update: (STATE) -> STATE) {
        scope.launch {
            _state.update(update)
        }
    }

    /**
     * Emits a side effect.
     *
     * @param effect The [EFFECT] to emit.
     */
    protected fun emitEffect(effect: EFFECT) {
        scope.launch {
            _effect.emit(effect)
        }
    }
}
