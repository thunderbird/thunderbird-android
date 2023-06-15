package app.k9mail.feature.account.setup.domain.usecase

import app.k9mail.core.common.domain.usecase.validation.ValidationError
import app.k9mail.core.common.domain.usecase.validation.ValidationResult
import app.k9mail.feature.account.setup.domain.DomainContract

internal class ValidatePassword : DomainContract.UseCase.ValidatePassword {

    // TODO change behavior to allow empty password when no password is required based on auth type
    override fun execute(password: String): ValidationResult {
        return when {
            password.isBlank() -> ValidationResult.Failure(ValidatePasswordError.EmptyPassword)

            else -> ValidationResult.Success
        }
    }

    sealed interface ValidatePasswordError : ValidationError {
        object EmptyPassword : ValidatePasswordError
    }
}
