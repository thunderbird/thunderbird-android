package app.k9mail.feature.account.setup.domain.usecase

import app.k9mail.core.common.domain.usecase.validation.ValidationResult
import app.k9mail.core.common.domain.usecase.validation.ValidationUseCase

class ValidateDisplayName : ValidationUseCase<String> {

    override fun execute(input: String): ValidationResult {
        return when {
            input.isBlank() -> ValidationResult.Failure(EmptyDisplayName())
            else -> ValidationResult.Success
        }
    }

    class EmptyDisplayName : Exception()
}
