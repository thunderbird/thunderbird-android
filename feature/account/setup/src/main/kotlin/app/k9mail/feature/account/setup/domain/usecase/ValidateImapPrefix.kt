package app.k9mail.feature.account.setup.domain.usecase

import app.k9mail.core.common.domain.usecase.validation.ValidationError
import app.k9mail.core.common.domain.usecase.validation.ValidationResult
import app.k9mail.feature.account.setup.domain.DomainContract

internal class ValidateImapPrefix : DomainContract.UseCase.ValidateImapPrefix {

    override fun execute(imapPrefix: String): ValidationResult {
        return when {
            imapPrefix.isEmpty() -> ValidationResult.Success
            imapPrefix.isBlank() -> ValidationResult.Failure(ValidateImapPrefixError.BlankImapPrefix)

            else -> ValidationResult.Success
        }
    }

    sealed interface ValidateImapPrefixError : ValidationError {
        object BlankImapPrefix : ValidateImapPrefixError
    }
}
