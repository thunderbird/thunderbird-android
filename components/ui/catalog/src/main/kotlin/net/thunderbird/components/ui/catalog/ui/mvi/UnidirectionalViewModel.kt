package net.thunderbird.components.ui.catalog.ui.mvi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface UnidirectionalViewModel<STATE, EVENT, EFFECT> {
    val state: StateFlow<STATE>

    val effect: SharedFlow<EFFECT>?

    fun event(event: EVENT)
}

data class StateDispatch<STATE, EVENT>(
    val state: State<STATE>,
    val dispatch: (EVENT) -> Unit,
)

@Composable
inline fun <reified STATE, EVENT, EFFECT> UnidirectionalViewModel<STATE, EVENT, EFFECT>.observe(
    crossinline handleEffect: (EFFECT) -> Unit,
): StateDispatch<STATE, EVENT> {
    val collectedState = state.collectAsStateWithLifecycle()
    val dispatch: (EVENT) -> Unit = { event(it) }

    effect?.let { effect ->
        LaunchedEffect(key1 = effect) {
            effect.collect {
                handleEffect(it)
            }
        }
    }

    return StateDispatch(
        state = collectedState,
        dispatch = dispatch,
    )
}
