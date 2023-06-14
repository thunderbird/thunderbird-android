package app.k9mail.feature.account.setup.domain.usecase

import app.k9mail.core.common.domain.usecase.validation.ValidationError
import app.k9mail.core.common.domain.usecase.validation.ValidationResult
import app.k9mail.core.common.domain.usecase.validation.ValidationUseCase
import app.k9mail.feature.account.setup.domain.usecase.ValidateEmailSignature.ValidateEmailSignatureError.BlankEmailSignature

// TODO check signature for input validity
internal class ValidateEmailSignature : ValidationUseCase<String> {

    override fun execute(input: String): ValidationResult {
        return when {
            input.isEmpty() -> ValidationResult.Success
            input.isBlank() -> ValidationResult.Failure(error = BlankEmailSignature)
            else -> ValidationResult.Success
        }
    }

    sealed interface ValidateEmailSignatureError : ValidationError {
        object BlankEmailSignature : ValidateEmailSignatureError
    }
}
