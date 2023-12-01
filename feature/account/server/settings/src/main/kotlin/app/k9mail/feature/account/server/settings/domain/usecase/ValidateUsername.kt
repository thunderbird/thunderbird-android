package app.k9mail.feature.account.server.settings.domain.usecase

import app.k9mail.core.common.domain.usecase.validation.ValidationError
import app.k9mail.core.common.domain.usecase.validation.ValidationResult
import app.k9mail.feature.account.server.settings.domain.ServerSettingsDomainContract.UseCase

internal class ValidateUsername : UseCase.ValidateUsername {

    override fun execute(username: String): ValidationResult {
        return when {
            username.isBlank() -> ValidationResult.Failure(ValidateUsernameError.EmptyUsername)

            else -> ValidationResult.Success
        }
    }

    sealed interface ValidateUsernameError : ValidationError {
        data object EmptyUsername : ValidateUsernameError
    }
}
