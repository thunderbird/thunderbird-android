package app.k9mail.feature.account.server.settings.domain.usecase

import app.k9mail.core.common.domain.usecase.validation.ValidationError
import app.k9mail.core.common.domain.usecase.validation.ValidationResult
import app.k9mail.feature.account.server.settings.domain.ServerSettingsDomainContract.UseCase

internal class ValidateServer : UseCase.ValidateServer {

    // TODO validate domain, ip4 or ip6
    override fun execute(server: String): ValidationResult {
        return when {
            server.isBlank() -> ValidationResult.Failure(ValidateServerError.EmptyServer)
            else -> ValidationResult.Success
        }
    }

    sealed interface ValidateServerError : ValidationError {
        object EmptyServer : ValidateServerError
    }
}
