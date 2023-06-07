package app.k9mail.feature.account.setup.domain.usecase

import app.k9mail.core.common.domain.usecase.validation.ValidationError
import app.k9mail.core.common.domain.usecase.validation.ValidationResult
import app.k9mail.core.common.domain.usecase.validation.ValidationUseCase

class ValidateServer : ValidationUseCase<String> {

    // TODO validate domain, ip4 or ip6
    override fun execute(input: String): ValidationResult {
        return when {
            input.isBlank() -> ValidationResult.Failure(ValidateServerError.EmptyServer)
            else -> ValidationResult.Success
        }
    }

    sealed interface ValidateServerError : ValidationError {
        object EmptyServer : ValidateServerError
    }
}
