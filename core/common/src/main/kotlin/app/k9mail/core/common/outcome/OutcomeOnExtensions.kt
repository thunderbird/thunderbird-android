package app.k9mail.core.common.outcome

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Invokes an action if the [Outcome] is a [Success].
 */
@OptIn(ExperimentalContracts::class)
inline infix fun <V, E> Outcome<V, E>.onSuccess(action: (V) -> Unit): Outcome<V, E> {
    contract {
        callsInPlace(action, InvocationKind.AT_MOST_ONCE)
    }

    if (this is Success) {
        action(value)
    }

    return this
}

/**
 * Invokes an action if the [Outcome] is a [Failure].
 */
@OptIn(ExperimentalContracts::class)
inline infix fun <V, E> Outcome<V, E>.onFailure(
    action: (E) -> Unit,
): Outcome<V, E> {
    contract {
        callsInPlace(action, InvocationKind.AT_MOST_ONCE)
    }

    if (this is Failure) {
        action(error)
    }

    return this
}
