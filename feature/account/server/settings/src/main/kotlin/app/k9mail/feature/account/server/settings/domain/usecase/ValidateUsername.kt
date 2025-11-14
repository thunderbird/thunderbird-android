package app.k9mail.feature.account.server.settings.domain.usecase

import app.k9mail.feature.account.server.settings.domain.ServerSettingsDomainContract.UseCase
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.core.validation.ValidationError
import net.thunderbird.core.validation.ValidationOutcome
import net.thunderbird.core.validation.ValidationSuccess

internal class ValidateUsername : UseCase.ValidateUsername {

    override fun execute(username: String): ValidationOutcome {
        return when {
            username.isBlank() -> Outcome.Failure(ValidateUsernameError.EmptyUsername)

            else -> ValidationSuccess
        }
    }

    sealed interface ValidateUsernameError : ValidationError {
        data object EmptyUsername : ValidateUsernameError
    }
}
