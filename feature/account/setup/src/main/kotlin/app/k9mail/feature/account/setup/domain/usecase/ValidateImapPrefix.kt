package app.k9mail.feature.account.setup.domain.usecase

import app.k9mail.core.common.domain.usecase.validation.ValidationError
import app.k9mail.core.common.domain.usecase.validation.ValidationResult
import app.k9mail.core.common.domain.usecase.validation.ValidationUseCase

internal class ValidateImapPrefix : ValidationUseCase<String> {

    override fun execute(input: String): ValidationResult {
        return when {
            input.isEmpty() -> ValidationResult.Success
            input.isBlank() -> ValidationResult.Failure(ValidateImapPrefixError.BlankImapPrefix)

            else -> ValidationResult.Success
        }
    }

    sealed interface ValidateImapPrefixError : ValidationError {
        object BlankImapPrefix : ValidateImapPrefixError
    }
}
