package app.k9mail.feature.account.setup.domain.usecase

import app.k9mail.feature.account.setup.domain.DomainContract
import net.thunderbird.core.common.domain.usecase.validation.ValidationError
import net.thunderbird.core.common.domain.usecase.validation.ValidationResult

internal class ValidateDisplayName : DomainContract.UseCase.ValidateDisplayName {

    override fun execute(displayName: String): ValidationResult {
        return when {
            displayName.isBlank() -> ValidationResult.Failure(ValidateDisplayNameError.EmptyDisplayName)
            else -> ValidationResult.Success
        }
    }

    sealed interface ValidateDisplayNameError : ValidationError {
        data object EmptyDisplayName : ValidateDisplayNameError
    }
}
