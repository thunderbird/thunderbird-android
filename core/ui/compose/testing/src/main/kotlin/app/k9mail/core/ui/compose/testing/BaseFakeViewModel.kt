package app.k9mail.core.ui.compose.testing

import app.k9mail.core.ui.compose.common.mvi.BaseViewModel

/**
 * Base class for providing fake MVI ViewModels for testing.
 *
 * This class provides a way to capture events and emit effects on a fake ViewModel.
 * The state can be set directly using [applyState].
 *
 * Example usage:
 *
 * ```
 * class FakeViewModel(
 *    initialState: State = State(),
 * ) : BaseFakeViewModel<State, Event, Effect>(initialState), ViewModel
 * ```
 */
abstract class BaseFakeViewModel<STATE, EVENT, EFFECT>(
    initialState: STATE,
) : BaseViewModel<STATE, EVENT, EFFECT>(initialState = initialState) {

    val events = mutableListOf<EVENT>()

    override fun event(event: EVENT) {
        events.add(event)
    }

    fun effect(effect: EFFECT) {
        emitEffect(effect)
    }

    fun applyState(state: STATE) {
        updateState { state }
    }
}
