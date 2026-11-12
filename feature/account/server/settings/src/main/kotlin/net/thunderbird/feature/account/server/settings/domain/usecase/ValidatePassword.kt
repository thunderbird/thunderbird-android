package net.thunderbird.feature.account.server.settings.domain.usecase

import net.thunderbird.components.core.outcome.Outcome
import net.thunderbird.core.validation.ValidationError
import net.thunderbird.core.validation.ValidationOutcome
import net.thunderbird.core.validation.ValidationSuccess
import net.thunderbird.feature.account.server.settings.domain.ServerSettingsDomainContract.UseCase

class ValidatePassword : UseCase.ValidatePassword {

    // TODO change behavior to allow empty password when no password is required based on auth type
    override fun execute(password: String): ValidationOutcome {
        return when {
            password.isBlank() -> Outcome.Failure(ValidatePasswordError.EmptyPassword)
            else -> ValidationSuccess
        }
    }

    sealed interface ValidatePasswordError : ValidationError {
        data object EmptyPassword : ValidatePasswordError
    }
}
