package app.k9mail.feature.account.setup.ui.autodiscovery

import app.k9mail.feature.account.server.settings.domain.usecase.ValidatePassword
import app.k9mail.feature.account.setup.domain.DomainContract.UseCase
import app.k9mail.feature.account.setup.domain.usecase.ValidateConfigurationApproval
import app.k9mail.feature.account.setup.domain.usecase.ValidateEmailAddress
import net.thunderbird.core.common.net.HostNameUtils
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.core.validation.ValidationError
import net.thunderbird.core.validation.ValidationOutcome
import net.thunderbird.core.validation.ValidationSuccess
import app.k9mail.feature.account.server.settings.domain.ServerSettingsDomainContract.UseCase as ServerSettingsUseCase

internal class AccountAutoDiscoveryValidator(
    private val emailAddressValidator: UseCase.ValidateEmailAddress = ValidateEmailAddress(),
    private val passwordValidator: ServerSettingsUseCase.ValidatePassword = ValidatePassword(),
    private val configurationApprovalValidator: UseCase.ValidateConfigurationApproval = ValidateConfigurationApproval(),
) : AccountAutoDiscoveryContract.Validator {

    override fun validateEmailAddress(emailAddress: String): ValidationOutcome {
        return emailAddressValidator.execute(emailAddress)
    }

    override fun validatePassword(password: String): ValidationOutcome {
        return passwordValidator.execute(password)
    }

    override fun validateProxyServer(server: String): ValidationOutcome {
        if (server.isBlank()) return Outcome.Failure(ProxyValidationError.EmptyProxyServer)

        val isLegalHostNameOrIP = HostNameUtils.isLegalHostNameOrIP(server) != null
        return if (isLegalHostNameOrIP) {
            ValidationSuccess
        } else {
            Outcome.Failure(ProxyValidationError.InvalidProxyServer)
        }
    }

    override fun validateProxyPort(port: Long?): ValidationOutcome {
        return when (port) {
            null -> Outcome.Failure(ProxyValidationError.EmptyProxyPort)
            in MIN_PORT_NUMBER..MAX_PORT_NUMBER -> ValidationSuccess
            else -> Outcome.Failure(ProxyValidationError.InvalidProxyPort)
        }
    }

    override fun validateConfigurationApproval(
        isApproved: Boolean?,
        isAutoDiscoveryTrusted: Boolean?,
    ): ValidationOutcome {
        return configurationApprovalValidator.execute(isApproved, isAutoDiscoveryTrusted)
    }

    sealed interface ProxyValidationError : ValidationError {
        data object EmptyProxyServer : ProxyValidationError
        data object InvalidProxyServer : ProxyValidationError
        data object EmptyProxyPort : ProxyValidationError
        data object InvalidProxyPort : ProxyValidationError
    }

    private companion object {
        const val MAX_PORT_NUMBER = 65535
        const val MIN_PORT_NUMBER = 1
    }
}
