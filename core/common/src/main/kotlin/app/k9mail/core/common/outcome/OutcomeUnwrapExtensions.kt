package app.k9mail.core.common.outcome

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * Unwrap the value of a successful [Outcome].
 */
@OptIn(ExperimentalContracts::class)
fun <V, E> Outcome<V, E>.unwrapValue(): V {
    contract {
        returns() implies (this@unwrapValue is Success<V>)
    }

    return when (this) {
        is Success -> value
        is Failure -> error("Calling Outcome.unwrapValue on a Failure is not possible")
    }
}

/**
 * Unwrap the error of a failed [Outcome].
 */
@OptIn(ExperimentalContracts::class)
fun <V, E> Outcome<V, E>.unwrapError(): E {
    contract {
        returns() implies (this@unwrapError is Failure<E>)
    }

    return when (this) {
        is Success -> error("Calling Outcome.unwrapError on a Success is not possible")
        is Failure -> error
    }
}

/**
 * Unwrap the cause of a failed [Outcome].
 */
@OptIn(ExperimentalContracts::class)
fun <V, E> Outcome<V, E>.unwrapCause(): Any? {
    contract {
        returns() implies (this@unwrapCause is Failure<E>)
    }

    return when (this) {
        is Success -> error("Calling Outcome.unwrapCause on a Success is not possible")
        is Failure -> cause
    }
}
