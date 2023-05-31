package app.k9mail.core.common.domain.usecase

interface UseCase<T, R> {
    fun execute(input: T): R
}
