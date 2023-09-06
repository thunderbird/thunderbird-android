package app.k9mail.feature.account.setup.ui.autodiscovery

import app.k9mail.core.common.domain.usecase.validation.ValidationResult
import app.k9mail.feature.account.server.config.domain.ServerConfigDomainContract
import app.k9mail.feature.account.server.config.domain.usecase.ValidateEmailAddress
import app.k9mail.feature.account.server.config.domain.usecase.ValidatePassword
import app.k9mail.feature.account.setup.domain.DomainContract.UseCase
import app.k9mail.feature.account.setup.domain.usecase.ValidateConfigurationApproval

internal class AccountAutoDiscoveryValidator(
    private val emailAddressValidator: ServerConfigDomainContract.UseCase.ValidateEmailAddress = ValidateEmailAddress(),
    private val passwordValidator: ServerConfigDomainContract.UseCase.ValidatePassword = ValidatePassword(),
    private val configurationApprovalValidator: UseCase.ValidateConfigurationApproval = ValidateConfigurationApproval(),
) : AccountAutoDiscoveryContract.Validator {

    override fun validateEmailAddress(emailAddress: String): ValidationResult {
        return emailAddressValidator.execute(emailAddress)
    }

    override fun validatePassword(password: String): ValidationResult {
        return passwordValidator.execute(password)
    }

    override fun validateConfigurationApproval(
        isApproved: Boolean?,
        isAutoDiscoveryTrusted: Boolean?,
    ): ValidationResult {
        return configurationApprovalValidator.execute(isApproved, isAutoDiscoveryTrusted)
    }
}
