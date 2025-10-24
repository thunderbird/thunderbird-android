package app.k9mail.feature.account.setup.domain.usecase

import app.k9mail.feature.account.setup.domain.DomainContract.UseCase
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.core.validation.ValidationError
import net.thunderbird.core.validation.ValidationOutcome
import net.thunderbird.core.validation.ValidationSuccess

class ValidateConfigurationApproval : UseCase.ValidateConfigurationApproval {
    override fun execute(isApproved: Boolean?, isAutoDiscoveryTrusted: Boolean?): ValidationOutcome {
        return if (isApproved == null && isAutoDiscoveryTrusted == null) {
            ValidationSuccess
        } else if (isAutoDiscoveryTrusted == true) {
            ValidationSuccess
        } else if (isApproved == true) {
            ValidationSuccess
        } else {
            Outcome.Failure(ValidateConfigurationApprovalError.ApprovalRequired)
        }
    }

    sealed interface ValidateConfigurationApprovalError : ValidationError {
        data object ApprovalRequired : ValidateConfigurationApprovalError
    }
}
