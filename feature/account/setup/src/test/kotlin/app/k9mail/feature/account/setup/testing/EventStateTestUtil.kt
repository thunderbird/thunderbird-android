package app.k9mail.feature.account.setup.testing

import app.cash.turbine.testIn
import app.k9mail.core.ui.compose.common.mvi.UnidirectionalViewModel
import assertk.assertions.assertThatAndTurbinesConsumed
import assertk.assertions.isEqualTo
import kotlinx.coroutines.CoroutineScope

internal suspend fun <STATE, EVENT, EFFECT> eventStateTest(
    viewModel: UnidirectionalViewModel<STATE, EVENT, EFFECT>,
    initialState: STATE,
    event: EVENT,
    expectedState: STATE,
    coroutineScope: CoroutineScope,
) {
    val stateTurbine = viewModel.state.testIn(coroutineScope)
    val effectTurbine = viewModel.effect.testIn(coroutineScope)
    val turbines = listOf(stateTurbine, effectTurbine)

    assertThatAndTurbinesConsumed(
        actual = stateTurbine.awaitItem(),
        turbines = turbines,
    ) {
        isEqualTo(initialState)
    }

    viewModel.event(event)

    assertThatAndTurbinesConsumed(
        actual = stateTurbine.awaitItem(),
        turbines = turbines,
    ) {
        isEqualTo(expectedState)
    }
}
