package app.k9mail.feature.account.setup.domain.usecase

import app.k9mail.core.common.domain.usecase.validation.ValidationError
import app.k9mail.core.common.domain.usecase.validation.ValidationResult
import app.k9mail.feature.account.setup.domain.DomainContract

internal class ValidateAccountName : DomainContract.UseCase.ValidateAccountName {
    override fun execute(accountName: String): ValidationResult {
        return when {
            accountName.isEmpty() -> ValidationResult.Success
            accountName.isBlank() -> ValidationResult.Failure(ValidateAccountNameError.BlankAccountName)
            else -> ValidationResult.Success
        }
    }

    sealed interface ValidateAccountNameError : ValidationError {
        data object BlankAccountName : ValidateAccountNameError
    }
}
