package app.k9mail.feature.account.setup.ui.autodiscovery

import app.k9mail.feature.account.server.settings.domain.usecase.ValidatePassword
import app.k9mail.feature.account.setup.domain.DomainContract.UseCase
import app.k9mail.feature.account.setup.domain.usecase.ValidateConfigurationApproval
import app.k9mail.feature.account.setup.domain.usecase.ValidateEmailAddress
import net.thunderbird.core.common.domain.usecase.validation.ValidationResult
import app.k9mail.feature.account.server.settings.domain.ServerSettingsDomainContract.UseCase as ServerSettingsUseCase

internal class AccountAutoDiscoveryValidator(
    private val emailAddressValidator: UseCase.ValidateEmailAddress = ValidateEmailAddress(),
    private val passwordValidator: ServerSettingsUseCase.ValidatePassword = ValidatePassword(),
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
