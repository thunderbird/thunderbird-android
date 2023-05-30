package assertk.assertions

import app.cash.turbine.ReceiveTurbine
import assertk.Assert
import assertk.all
import assertk.assertThat

/**
 * The `assertThatAndTurbinesConsumed` function ensures that the assertion passed and
 * all events in the given turbines have been consumed.
 *
 * Usage:
 *  val actualValue: T = getActualValue()
 *  val turbines: List<ReceiveTurbine<*>> = getTurbines()
 *  assertThatAndEnsureAllEventsConsumed(actualValue, turbines) {
 *      // your assertion here
 *  }
 *
 * @param T The type of the actual value.
 * @param actual The actual value being asserted.
 * @param turbines The list of ReceiveTurbine instances to check if all events are consumed.
 * @param assertion An extension function on `Assert<T>`, which is used to define assertions on the actual value.
 */
fun <T> assertThatAndTurbinesConsumed(
    actual: T,
    turbines: List<ReceiveTurbine<*>>,
    assertion: Assert<T>.() -> Unit,
) {
    assertThat(actual).all {
        assertion()
    }

    turbines.forEach { it.ensureAllEventsConsumed() }
}
