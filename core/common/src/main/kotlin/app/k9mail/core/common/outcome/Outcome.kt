package app.k9mail.core.common.outcome

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * [Outcome] is a type that represents a value that can either be
 * - a [Success] (with a value of type [V])
 * - a [Failure] (with a value of type [E]).
 *
 * @param V The type of the value of a successful [Outcome].
 * @param E The type of the error of a failed [Outcome].
 */
sealed interface Outcome<out V, out E> {

    val isSuccess: Boolean
        get() = this is Success

    val isFailure: Boolean
        get() = this is Failure
}

/**
 * Represents a successful [Outcome] with a value of type [V].
 *
 * @param value The value of the successful [Outcome].
 */
data class Success<out V>(val value: V) : Outcome<V, Nothing>

/**
 * Represents a failed [Outcome] with an error of type [E]..
 *
 * @param error The error of the failed [Outcome].
 */
data class Failure<out E>(val error: E) : Outcome<Nothing, E>

/**
 * Convert a Result of type [T] to an Outcome of type [T] and [Throwable].
 *
 * @param T The type of the value of a successful [Outcome].
 */
fun <T> Result<T>.asOutcome(): Outcome<T, Throwable> {
    return fold(
        onSuccess = { Success(it) },
        onFailure = { Failure(it) },
    )
}

/**
 * Convert a Flow of type [T] to a Flow of type [Outcome] of type [T] and [Throwable].
 *
 * @param T The type of the value of a successful [Outcome].
 */
fun <T> Flow<T>.asOutcome(): Flow<Outcome<T, Throwable>> {
    return map<T, Outcome<T, Throwable>> {
        Success(it)
    }.catch { error ->
        emit(Failure(error))
    }
}
