package net.thunderbird.ui.catalog.ui.mvi

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

abstract class BaseViewModel<STATE, EVENT, EFFECT>(
    initialState: STATE,
) : ViewModel(),
    UnidirectionalViewModel<STATE, EVENT, EFFECT> {

    private val _state = MutableStateFlow(initialState)
    override val state: StateFlow<STATE> = _state.asStateFlow()

    override val effect: SharedFlow<EFFECT>? = null

    protected fun updateState(update: (STATE) -> STATE) {
        _state.update(update)
    }
}
