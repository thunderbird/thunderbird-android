package app.k9mail.core.ui.compose.testing.mvi

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.TurbineContext
import app.cash.turbine.turbineScope
import app.k9mail.core.ui.compose.common.mvi.UnidirectionalViewModel
import assertk.Assert
import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

/**
 * The `runMviTest` function is a wrapper around `runTest` and `turbineScope`
 * that provides a MviContext to the test body.
 */
fun runMviTest(
    testBody: suspend MviContext.() -> Unit,
) {
    runTest {
        val testScope = this
        turbineScope {
            val turbineContext = this
            testBody(
                DefaultMviContext(
                    testScope = testScope,
                    turbineContext = turbineContext,
                ),
            )
        }
    }
}

interface MviContext {
    val testScope: TestScope
    val turbineContext: TurbineContext
}

class DefaultMviContext(
    override val testScope: TestScope,
    override val turbineContext: TurbineContext,
) : MviContext

@OptIn(ExperimentalCoroutinesApi::class)
fun MviContext.advanceUntilIdle() {
    testScope.advanceUntilIdle()
}

/**
 * The `turbines` extension function creates a MviTurbines instance for the given MVI ViewModel.
 */
inline fun <reified STATE, EVENT, EFFECT> MviContext.turbines(
    viewModel: UnidirectionalViewModel<STATE, EVENT, EFFECT>,
): MviTurbines<STATE, EFFECT> {
    with(turbineContext) {
        return MviTurbines(
            stateTurbine = viewModel.state.testIn(testScope.backgroundScope),
            effectTurbine = viewModel.effect.testIn(testScope.backgroundScope),
        )
    }
}

/**
 * The `turbinesWithInitialStateCheck` extension function creates a MviTurbines instance for the given MVI ViewModel
 * and ensures that the initial state is emitted.
 */
suspend inline fun <reified STATE, EVENT, EFFECT> MviContext.turbinesWithInitialStateCheck(
    viewModel: UnidirectionalViewModel<STATE, EVENT, EFFECT>,
    initialState: STATE,
): MviTurbines<STATE, EFFECT> {
    val turbines = turbines(viewModel)

    assertThatAndMviTurbinesConsumed(
        actual = turbines.stateTurbine.awaitItem(),
        turbines = turbines,
    ) {
        isEqualTo(initialState)
    }

    return turbines
}

/**
 * The `assertThatAndMviTurbinesConsumed` function ensures that the assertion passed and
 * all events in the given MviTurbines have been consumed.
 *
 * Usage:
 *  val actualValue: T = getActualValue()
 *  val turbines = viewModel.turbines(coroutineScope)
 *  assertThatAndMviTurbinesConsumed(actualValue, turbines) {
 *     // your assertion here
 *  }
 *
 *  @param T The type of the actual value.
 *  @param STATE The type of the state.
 *  @param EFFECT The type of the effect.
 *  @param actual The actual value being asserted.
 *  @param turbines The MviTurbines instance to check if all events are consumed.
 *  @param assertion An extension function on `Assert<T>`, which is used to define assertions on the actual value.
 */
fun <T, STATE, EFFECT> assertThatAndMviTurbinesConsumed(
    actual: T,
    turbines: MviTurbines<STATE, EFFECT>,
    assertion: Assert<T>.() -> Unit,
) {
    assertThat(actual).all {
        assertion()
    }

    turbines.stateTurbine.ensureAllEventsConsumed()
    turbines.effectTurbine.ensureAllEventsConsumed()
}

/**
 * The `assertThatAndStateTurbineConsumed` function ensures that the assertion passed and
 * all events in the state turbine have been consumed.
 */
suspend fun <STATE, EFFECT> MviTurbines<STATE, EFFECT>.assertThatAndStateTurbineConsumed(
    assertion: Assert<STATE>.() -> Unit,
) {
    assertThat(stateTurbine.awaitItem()).all {
        assertion()
    }

    stateTurbine.ensureAllEventsConsumed()
    effectTurbine.ensureAllEventsConsumed()
}

/**
 * The `assertThatAndEffectTurbineConsumed` function ensures that the assertion passed and
 * all events in the effect turbine have been consumed.
 */
suspend fun <STATE, EFFECT> MviTurbines<STATE, EFFECT>.assertThatAndEffectTurbineConsumed(
    assertion: Assert<EFFECT>.() -> Unit,
) {
    assertThat(effectTurbine.awaitItem()).all {
        assertion()
    }

    stateTurbine.ensureAllEventsConsumed()
    effectTurbine.ensureAllEventsConsumed()
}

/**
 * A container class for the state and effect turbines of an MVI ViewModel.
 */
data class MviTurbines<STATE, EFFECT>(
    val stateTurbine: ReceiveTurbine<STATE>,
    val effectTurbine: ReceiveTurbine<EFFECT>,
) {
    suspend fun awaitStateItem() = stateTurbine.awaitItem()

    suspend fun awaitEffectItem() = effectTurbine.awaitItem()
}
