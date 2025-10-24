package app.k9mail.feature.account.setup.domain.usecase

import app.k9mail.feature.account.setup.domain.DomainContract
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.core.validation.ValidationError
import net.thunderbird.core.validation.ValidationOutcome
import net.thunderbird.core.validation.ValidationSuccess

internal class ValidateAccountName : DomainContract.UseCase.ValidateAccountName {
    override fun execute(accountName: String): ValidationOutcome {
        return when {
            accountName.isEmpty() -> ValidationSuccess
            accountName.isBlank() -> Outcome.Failure(ValidateAccountNameError.BlankAccountName)
            else -> ValidationSuccess
        }
    }

    sealed interface ValidateAccountNameError : ValidationError {
        data object BlankAccountName : ValidateAccountNameError
    }
}
