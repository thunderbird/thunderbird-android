package app.k9mail.feature.account.server.settings.domain.usecase

import app.k9mail.feature.account.server.settings.domain.ServerSettingsDomainContract.UseCase
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.core.validation.ValidationError
import net.thunderbird.core.validation.ValidationOutcome
import net.thunderbird.core.validation.ValidationSuccess

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
