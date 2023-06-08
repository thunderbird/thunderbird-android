package app.k9mail.core.common.domain.usecase

/**
 * A use case is a single action the user can trigger.
 *
 * @param INPUT The input type of the use case.
 * @param RESULT The result type of the use case.
 */
interface UseCase<INPUT, RESULT> {

    /**
     * Executes the use case.
     *
     * @param input The input for the use case.
     * @return The result of the use case.
     */
    suspend fun execute(input: INPUT): RESULT
}
