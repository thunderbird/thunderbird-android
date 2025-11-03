package net.thunderbird.core.outcome

/**
 * A sealed interface representing the outcome of an operation.
 *
 * @param SUCCESS The type of the value when the operation succeeds.
 * @param FAILURE The type of the error when the operation fails.
 */
sealed interface Outcome<out SUCCESS, out FAILURE> {

    /**
     * A successful outcome with a value of type [SUCCESS].
     *
     * @param data The value of the successful outcome.
     */
    data class Success<out SUCCESS>(val data: SUCCESS) : Outcome<SUCCESS, Nothing>

    /**
     * A failed outcome with an error of type [FAILURE].
     *
     * @param error The error of the failed outcome.
     * @param cause The cause of the failed outcome.
     */
    data class Failure<out FAILURE>(
        val error: FAILURE,
        val cause: Any? = null,
    ) : Outcome<Nothing, FAILURE>

    /**
     * Whether the outcome is a success.
     */
    val isSuccess: Boolean
        get() = this is Success

    /**
     * Whether the outcome is a failure.
     */
    val isFailure: Boolean
        get() = this is Failure

    companion object {
        /**
         * Create a [Success] outcome with the given value.
         *
         * @param data The value of the successful outcome.
         */
        fun <SUCCESS> success(data: SUCCESS): Outcome<SUCCESS, Nothing> = Success(data)

        /**
         * Create a [Failure] outcome with the given error.
         *
         * @param error The error of the failed outcome.
         */
        fun <FAILURE> failure(error: FAILURE): Outcome<Nothing, FAILURE> = Failure(error)
    }
}

/**
 * Map the value and error of an [Outcome] to a new value.
 *
 * @param transformSuccess The function to transform the value of a [Success] to a new value.
 * @param transformFailure The function to transform the value of a [Failure] to a new value.
 */
inline fun <IN_SUCCESS, IN_FAILURE, OUT_SUCCESS, OUT_FAILURE> Outcome<IN_SUCCESS, IN_FAILURE>.map(
    transformSuccess: (IN_SUCCESS) -> OUT_SUCCESS,
    transformFailure: (IN_FAILURE, Any?) -> OUT_FAILURE,
): Outcome<OUT_SUCCESS, OUT_FAILURE> {
    return when (this) {
        is Outcome.Success -> Outcome.Success(transformSuccess(data))
        is Outcome.Failure -> Outcome.Failure(transformFailure(error, cause))
    }
}

/**
 * Map the value of a [Outcome] to a new value.
 *
 * @param transformSuccess The function to transform the value of a [Success] to a new value.
 */
inline fun <IN_SUCCESS, OUT_SUCCESS, FAILURE> Outcome<IN_SUCCESS, FAILURE>.mapSuccess(
    transformSuccess: (IN_SUCCESS) -> OUT_SUCCESS,
): Outcome<OUT_SUCCESS, FAILURE> {
    return when (this) {
        is Outcome.Success -> Outcome.Success(transformSuccess(data))
        is Outcome.Failure -> this
    }
}

/**
 * Flat map the value and error of an [Outcome] to a new [Outcome].
 */
inline fun <IN_SUCCESS, FAILURE, OUT_SUCCESS> Outcome<IN_SUCCESS, FAILURE>.flatMapSuccess(
    transformSuccess: (IN_SUCCESS) -> Outcome<OUT_SUCCESS, FAILURE>,
): Outcome<OUT_SUCCESS, FAILURE> {
    return when (this) {
        is Outcome.Success -> transformSuccess(data)
        is Outcome.Failure -> this
    }
}

/**
 * Map the error of a [Outcome] to a new value.
 *
 * @param transformFailure The function to transform the value of a [Failure] to a new value.
 */
inline fun <SUCCESS, IN_FAILURE, OUT_FAILURE> Outcome<SUCCESS, IN_FAILURE>.mapFailure(
    transformFailure: (IN_FAILURE, Any?) -> OUT_FAILURE,
): Outcome<SUCCESS, OUT_FAILURE> {
    return when (this) {
        is Outcome.Success -> this
        is Outcome.Failure -> Outcome.Failure(transformFailure(error, cause))
    }
}

/**
 * Handle the value of an [Outcome] and execute the given function.
 *
 * @param onSuccess The function to execute if the outcome is a [Success].
 * @param onFailure The function to execute if the outcome is a [Failure].
 */
fun <SUCCESS, FAILURE> Outcome<SUCCESS, FAILURE>.handle(
    onSuccess: (SUCCESS) -> Unit,
    onFailure: (FAILURE) -> Unit,
) {
    when (this) {
        is Outcome.Success -> onSuccess(data)
        is Outcome.Failure -> onFailure(error)
    }
}

/**
 * Handle the value of an [Outcome] and execute the given function.
 *
 * @param onSuccess The function to execute if the outcome is a [Success].
 * @param onFailure The function to execute if the outcome is a [Failure].
 */
suspend fun <SUCCESS, FAILURE> Outcome<SUCCESS, FAILURE>.handleAsync(
    onSuccess: suspend (SUCCESS) -> Unit,
    onFailure: suspend (FAILURE) -> Unit,
) {
    when (this) {
        is Outcome.Success -> onSuccess(data)
        is Outcome.Failure -> onFailure(error)
    }
}
