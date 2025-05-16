package app.k9mail.feature.account.server.settings.domain.usecase

import app.k9mail.feature.account.server.settings.domain.ServerSettingsDomainContract.UseCase
import net.thunderbird.core.common.domain.usecase.validation.ValidationError
import net.thunderbird.core.common.domain.usecase.validation.ValidationResult
import net.thunderbird.core.common.net.HostNameUtils

internal class ValidateServer : UseCase.ValidateServer {

    override fun execute(server: String): ValidationResult {
        if (server.isBlank()) {
            return ValidationResult.Failure(ValidateServerError.EmptyServer)
        }

        return validateHostnameOrIpAddress(server)
    }

    private fun validateHostnameOrIpAddress(server: String): ValidationResult {
        val isLegalHostNameOrIP = HostNameUtils.isLegalHostNameOrIP(server) != null

        return if (isLegalHostNameOrIP) {
            ValidationResult.Success
        } else {
            ValidationResult.Failure(ValidateServerError.InvalidHostnameOrIpAddress)
        }
    }

    sealed interface ValidateServerError : ValidationError {
        data object EmptyServer : ValidateServerError
        data object InvalidHostnameOrIpAddress : ValidateServerError
    }
}
