package app.k9mail.core.common.outcome

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Map the value and error of an [Outcome] to a new value.
 *
 * @param transformSuccess The function to transform the value of a [Success] to a new value.
 * @param transformFailure The function to transform the value of a [Failure] to a new value.
 */
fun <IN_SUCCESS, IN_FAILURE, OUT_SUCCESS, OUT_FAILURE> Outcome<IN_SUCCESS, IN_FAILURE>.map(
    transformSuccess: (IN_SUCCESS) -> OUT_SUCCESS,
    transformFailure: (IN_FAILURE) -> OUT_FAILURE,
): Outcome<OUT_SUCCESS, OUT_FAILURE> {
    return when (this) {
        is Success -> Success(transformSuccess(value))
        is Failure -> Failure(transformFailure(error))
    }
}

/**
 * Map the value of a success [Outcome] to a new value.
 *
 * @param transform The function to transform the value of a [Success] to a new value.
 */
@OptIn(ExperimentalContracts::class)
inline infix fun <V, E, O> Outcome<V, E>.mapValue(
    transform: (V) -> O,
): Outcome<O, E> {
    contract {
        callsInPlace(transform, InvocationKind.AT_MOST_ONCE)
    }

    return when (this) {
        is Success -> Success(transform(value))
        is Failure -> this
    }
}

/**
 * Map the value of a failure [Outcome] to a new value.
 *
 * @param transform The function to transform the value of a [Failure] to a new value.
 */
@OptIn(ExperimentalContracts::class)
inline infix fun <V, E, O> Outcome<V, E>.mapError(
    transform: (E) -> O,
): Outcome<V, O> {
    contract {
        callsInPlace(transform, InvocationKind.AT_MOST_ONCE)
    }

    return when (this) {
        is Success -> this
        is Failure -> Failure(transform(error))
    }
}
