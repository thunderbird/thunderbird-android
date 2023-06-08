package app.k9mail.feature.account.setup.domain.usecase

import app.k9mail.core.common.domain.usecase.validation.ValidationError
import app.k9mail.core.common.domain.usecase.validation.ValidationResult
import app.k9mail.core.common.domain.usecase.validation.ValidationUseCase

class ValidateAccountName : ValidationUseCase<String> {
    override suspend fun execute(input: String): ValidationResult {
        return when {
            input.isEmpty() -> ValidationResult.Success
            input.isBlank() -> ValidationResult.Failure(ValidateAccountNameError.BlankAccountName)
            else -> ValidationResult.Success
        }
    }

    sealed interface ValidateAccountNameError : ValidationError {
        object BlankAccountName : ValidateAccountNameError
    }
}
