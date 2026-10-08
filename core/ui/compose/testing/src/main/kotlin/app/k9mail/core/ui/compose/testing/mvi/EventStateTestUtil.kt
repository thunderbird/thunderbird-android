package app.k9mail.core.ui.compose.testing.mvi

import assertk.assertThat
import assertk.assertions.isEqualTo
import net.thunderbird.core.ui.contract.mvi.UnidirectionalViewModel

/**
 * Tests that the state of the [viewModel] changes as expected when the [event] is sent.
 */
suspend inline fun <reified STATE, EVENT, EFFECT> MviContext.eventStateTest(
    viewModel: UnidirectionalViewModel<STATE, EVENT, EFFECT>,
    initialState: STATE,
    event: EVENT,
    expectedState: STATE,
) {
    val turbines = turbinesWithInitialStateCheck(viewModel, initialState)

    viewModel.event(event)

    assertThat(turbines.stateTurbine.awaitItem()).isEqualTo(expectedState)
}
