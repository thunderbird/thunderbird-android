package app.k9mail.core.ui.compose.common.mvi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface for a unidirectional view model with side-effects ([EFFECT]). It has a [STATE] and can handle [EVENT]'s.
 *
 * @param STATE The type that represents the state of the ViewModel. For example, the UI state of a screen can be
 * represented as a state.
 * @param EVENT The type that represents user actions that can occur and should be handled by the ViewModel. For
 * example, a button click can be represented as an event.
 * @param EFFECT The  type that represents side-effects that can occur in response to the state changes. For example,
 * a navigation event can be represented as an effect.
 */
interface UnidirectionalViewModel<STATE, EVENT, EFFECT> {
    /**
     * The current [STATE] of the view model.
     */
    val state: StateFlow<STATE>

    /**
     * The side-effects ([EFFECT]) produced by the view model.
     */
    val effect: SharedFlow<EFFECT>

    /**
     * Handles an [EVENT] and updates the [STATE] of the view model.
     *
     * @param event The [EVENT] to handle.
     */
    fun event(event: EVENT)
}

/**
 * Data class representing a state and a dispatch function, used for destructuring in [observe].
 */
data class StateDispatch<STATE, EVENT>(
    val state: State<STATE>,
    val dispatch: (EVENT) -> Unit,
)

/**
 * Composable function that observes a UnidirectionalViewModel and handles its side effects.
 *
 * Example usage:
 * ```
 * @Composable
 * fun MyScreen(
 *    onNavigateNext: () -> Unit,
 *    onNavigateBack: () -> Unit,
 *    viewModel: MyUnidirectionalViewModel<MyState, MyEvent, MyEffect>,
 * ) {
 *    val (state, dispatch) = viewModel.observe { effect ->
 *      when (effect) {
 *          MyEffect.OnBackPressed -> onNavigateBack()
 *          MyEffect.OnNextPressed -> onNavigateNext()
 *      }
 *    }
 *
 *    MyContent(
 *      onNextClick = {
 *          dispatch(MyEvent.OnNext)
 *      },
 *      onBackClick = {
 *          dispatch(MyEvent.OnBack)
 *      },
 *      state = state.value,
 *    )
 * }
 * ```
 *
 * @param STATE The type that represents the state of the ViewModel.
 * @param EVENT The type that represents user actions that can occur and should be handled by the ViewModel.
 * @param EFFECT The  type that represents side-effects that can occur in response to the state changes.
 *
 * @param handleEffect A function to handle side effects ([EFFECT]).
 *
 * @return A [StateDispatch] containing the state and a dispatch function.
 */
@Composable
inline fun <reified STATE, EVENT, EFFECT> UnidirectionalViewModel<STATE, EVENT, EFFECT>.observe(
    crossinline handleEffect: (EFFECT) -> Unit,
): StateDispatch<STATE, EVENT> {
    val collectedState = state.collectAsStateWithLifecycle()

    val dispatch: (EVENT) -> Unit = { event(it) }

    LaunchedEffect(key1 = effect) {
        effect.collect {
            handleEffect(it)
        }
    }

    return StateDispatch(
        state = collectedState,
        dispatch = dispatch,
    )
}

/**
 * Composable function that observes a UnidirectionalViewModel without handling side effects.
 *
 * Example usage:
 * ```
 * @Composable
 * fun MyScreen(
 *   viewModel: MyUnidirectionalViewModel<MyState, MyEvent, MyEffect>,
 *   onNavigateNext: () -> Unit,
 *   onNavigateBack: () -> Unit,
 * ) {
 *   val (state, dispatch) = viewModel.observeWithoutEffect()
 *
 *   MyContent(
 *     onNextClick = {
 *       dispatch(MyEvent.OnNext)
 *     },
 *     onBackClick = {
 *       dispatch(MyEvent.OnBack)
 *     },
 *     state = state.value,
 *   )
 * }
 * ```
 *
 * @param STATE The type that represents the state of the ViewModel.
 * @param EVENT The type that represents user actions that can occur and should be handled by the ViewModel.
 *
 * @return A [StateDispatch] containing the state and a dispatch function.
 */
@Suppress("MaxLineLength")
@Composable
inline fun <reified STATE, EVENT, EFFECT> UnidirectionalViewModel<STATE, EVENT, EFFECT>.observeWithoutEffect(
    // no effect handler
): StateDispatch<STATE, EVENT> {
    val collectedState = state.collectAsStateWithLifecycle()
    val dispatch: (EVENT) -> Unit = { event(it) }

    return StateDispatch(
        state = collectedState,
        dispatch = dispatch,
    )
}
