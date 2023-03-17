package assertk.assertions

import assertk.Assert
import assertk.assertions.support.expected
import assertk.assertions.support.show

fun <T> Assert<List<T>>.containsNoDuplicates() = given { actual ->
    val seen: MutableSet<T> = mutableSetOf()
    val duplicates = actual.filter { !seen.add(it) }
    if (duplicates.isNotEmpty()) {
        expected("to contain no duplicates but found: ${show(duplicates)}")
    }
}
