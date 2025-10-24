package app.k9mail.feature.account.setup.domain.usecase

import app.k9mail.feature.account.setup.domain.DomainContract
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.core.validation.ValidationError
import net.thunderbird.core.validation.ValidationOutcome
import net.thunderbird.core.validation.ValidationSuccess

internal class ValidateDisplayName : DomainContract.UseCase.ValidateDisplayName {

    override fun execute(displayName: String): ValidationOutcome {
        return when {
            displayName.isBlank() -> Outcome.Failure(ValidateDisplayNameError.EmptyDisplayName)
            else -> ValidationSuccess
        }
    }

    sealed interface ValidateDisplayNameError : ValidationError {
        data object EmptyDisplayName : ValidateDisplayNameError
    }
}
