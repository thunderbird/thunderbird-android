package app.k9mail.feature.account.setup.domain.usecase

import app.k9mail.core.common.domain.usecase.validation.ValidationResult
import app.k9mail.core.common.domain.usecase.validation.ValidationUseCase

// TODO check signature for input validity
class ValidateEmailSignature : ValidationUseCase<String> {

    override fun execute(input: String): ValidationResult {
        return when {
            input.isBlank() -> ValidationResult.Failure(EmptyEmailSignature())
            else -> ValidationResult.Success
        }
    }

    class EmptyEmailSignature : Exception()
}
