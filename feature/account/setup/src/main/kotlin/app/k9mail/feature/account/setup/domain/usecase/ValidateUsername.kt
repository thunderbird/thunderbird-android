package app.k9mail.feature.account.setup.domain.usecase

import app.k9mail.core.common.domain.usecase.validation.ValidationError
import app.k9mail.core.common.domain.usecase.validation.ValidationResult
import app.k9mail.core.common.domain.usecase.validation.ValidationUseCase

class ValidateUsername : ValidationUseCase<String> {

    override fun execute(input: String): ValidationResult {
        return when {
            input.isBlank() -> ValidationResult.Failure(ValidateUsernameError.EmptyUsername)

            else -> ValidationResult.Success
        }
    }

    sealed interface ValidateUsernameError : ValidationError {
        object EmptyUsername : ValidateUsernameError
    }
}
