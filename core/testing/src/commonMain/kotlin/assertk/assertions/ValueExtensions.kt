package assertk.assertions

import assertk.Assert
import assertk.assertions.support.expected
import assertk.assertions.support.show

/**
 * Asserts that the value is one of the expected values.
 */
fun <T> Assert<T>.isOneOf(vararg expectedValues: T) = given { actual ->
    if (expectedValues.none { it == actual }) {
        expected("to be one of ${show(expectedValues.toList())} but was ${show(actual)}")
    }
}

/**
 * Asserts that the value is one of the expected values.
 */
inline fun <reified T> Assert<T>.isOneOf(expectedValues: Collection<T>) {
    isOneOf(*expectedValues.toTypedArray())
}
