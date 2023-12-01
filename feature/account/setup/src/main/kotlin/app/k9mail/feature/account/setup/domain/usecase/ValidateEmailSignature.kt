package app.k9mail.feature.account.setup.domain.usecase

import app.k9mail.core.common.domain.usecase.validation.ValidationError
import app.k9mail.core.common.domain.usecase.validation.ValidationResult
import app.k9mail.feature.account.setup.domain.DomainContract
import app.k9mail.feature.account.setup.domain.usecase.ValidateEmailSignature.ValidateEmailSignatureError.BlankEmailSignature

// TODO check signature for input validity
internal class ValidateEmailSignature : DomainContract.UseCase.ValidateEmailSignature {

    override fun execute(emailSignature: String): ValidationResult {
        return when {
            emailSignature.isEmpty() -> ValidationResult.Success
            emailSignature.isBlank() -> ValidationResult.Failure(error = BlankEmailSignature)
            else -> ValidationResult.Success
        }
    }

    sealed interface ValidateEmailSignatureError : ValidationError {
        data object BlankEmailSignature : ValidateEmailSignatureError
    }
}
