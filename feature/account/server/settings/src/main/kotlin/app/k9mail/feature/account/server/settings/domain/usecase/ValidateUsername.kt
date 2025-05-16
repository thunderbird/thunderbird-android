package app.k9mail.feature.account.server.settings.domain.usecase

import app.k9mail.feature.account.server.settings.domain.ServerSettingsDomainContract.UseCase
import net.thunderbird.core.common.domain.usecase.validation.ValidationError
import net.thunderbird.core.common.domain.usecase.validation.ValidationResult

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
