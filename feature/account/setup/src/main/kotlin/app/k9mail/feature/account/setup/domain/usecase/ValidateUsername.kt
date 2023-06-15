package app.k9mail.feature.account.setup.domain.usecase

import app.k9mail.core.common.domain.usecase.validation.ValidationError
import app.k9mail.core.common.domain.usecase.validation.ValidationResult
import app.k9mail.feature.account.setup.domain.DomainContract

internal class ValidateUsername : DomainContract.UseCase.ValidateUsername {

    override fun execute(username: String): ValidationResult {
        return when {
            username.isBlank() -> ValidationResult.Failure(ValidateUsernameError.EmptyUsername)

            else -> ValidationResult.Success
        }
    }

    sealed interface ValidateUsernameError : ValidationError {
        object EmptyUsername : ValidateUsernameError
    }
}
