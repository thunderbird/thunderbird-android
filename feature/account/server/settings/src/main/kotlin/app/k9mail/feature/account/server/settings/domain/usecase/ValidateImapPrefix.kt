package app.k9mail.feature.account.server.settings.domain.usecase

import app.k9mail.core.common.domain.usecase.validation.ValidationError
import app.k9mail.core.common.domain.usecase.validation.ValidationResult
import app.k9mail.feature.account.server.settings.domain.ServerSettingsDomainContract.UseCase

internal class ValidateImapPrefix : UseCase.ValidateImapPrefix {

    override fun execute(imapPrefix: String): ValidationResult {
        return when {
            imapPrefix.isEmpty() -> ValidationResult.Success
            imapPrefix.isBlank() -> ValidationResult.Failure(ValidateImapPrefixError.BlankImapPrefix)

            else -> ValidationResult.Success
        }
    }

    sealed interface ValidateImapPrefixError : ValidationError {
        data object BlankImapPrefix : ValidateImapPrefixError
    }
}
