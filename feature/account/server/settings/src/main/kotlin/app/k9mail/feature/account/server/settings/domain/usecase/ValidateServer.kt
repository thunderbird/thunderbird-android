package app.k9mail.feature.account.server.settings.domain.usecase

import app.k9mail.feature.account.server.settings.domain.ServerSettingsDomainContract.UseCase
import net.thunderbird.core.common.net.HostNameUtils
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.core.validation.ValidationError
import net.thunderbird.core.validation.ValidationOutcome
import net.thunderbird.core.validation.ValidationSuccess

internal class ValidateServer : UseCase.ValidateServer {

    override fun execute(server: String): ValidationOutcome {
        if (server.isBlank()) {
            return Outcome.Failure(ValidateServerError.EmptyServer)
        }

        return validateHostnameOrIpAddress(server)
    }

    private fun validateHostnameOrIpAddress(server: String): ValidationOutcome {
        val isLegalHostNameOrIP = HostNameUtils.isLegalHostNameOrIP(server) != null

        return if (isLegalHostNameOrIP) {
            ValidationSuccess
        } else {
            Outcome.Failure(ValidateServerError.InvalidHostnameOrIpAddress)
        }
    }

    sealed interface ValidateServerError : ValidationError {
        data object EmptyServer : ValidateServerError
        data object InvalidHostnameOrIpAddress : ValidateServerError
    }
}
