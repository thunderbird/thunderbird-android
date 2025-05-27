package app.k9mail.feature.account.server.settings.domain.usecase

import app.k9mail.feature.account.server.settings.domain.ServerSettingsDomainContract.UseCase
import net.thunderbird.core.common.domain.usecase.validation.ValidationError
import net.thunderbird.core.common.domain.usecase.validation.ValidationResult

class ValidatePassword : UseCase.ValidatePassword {

    // TODO change behavior to allow empty password when no password is required based on auth type
    override fun execute(password: String): ValidationResult {
        return when {
            password.isBlank() -> ValidationResult.Failure(ValidatePasswordError.EmptyPassword)

            else -> ValidationResult.Success
        }
    }

    sealed interface ValidatePasswordError : ValidationError {
        data object EmptyPassword : ValidatePasswordError
    }
}
