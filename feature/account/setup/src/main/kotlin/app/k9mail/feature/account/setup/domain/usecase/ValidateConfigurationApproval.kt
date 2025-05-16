package app.k9mail.feature.account.setup.domain.usecase

import app.k9mail.feature.account.setup.domain.DomainContract.UseCase
import net.thunderbird.core.common.domain.usecase.validation.ValidationError
import net.thunderbird.core.common.domain.usecase.validation.ValidationResult

class ValidateConfigurationApproval : UseCase.ValidateConfigurationApproval {
    override fun execute(isApproved: Boolean?, isAutoDiscoveryTrusted: Boolean?): ValidationResult {
        return if (isApproved == null && isAutoDiscoveryTrusted == null) {
            ValidationResult.Success
        } else if (isAutoDiscoveryTrusted == true) {
            ValidationResult.Success
        } else if (isApproved == true) {
            ValidationResult.Success
        } else {
            ValidationResult.Failure(ValidateConfigurationApprovalError.ApprovalRequired)
        }
    }

    sealed interface ValidateConfigurationApprovalError : ValidationError {
        data object ApprovalRequired : ValidateConfigurationApprovalError
    }
}
