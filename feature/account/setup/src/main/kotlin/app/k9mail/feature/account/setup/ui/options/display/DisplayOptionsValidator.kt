package app.k9mail.feature.account.setup.ui.options.display

import app.k9mail.feature.account.setup.domain.usecase.ValidateAccountName
import app.k9mail.feature.account.setup.domain.usecase.ValidateDisplayName
import app.k9mail.feature.account.setup.domain.usecase.ValidateEmailSignature
import app.k9mail.feature.account.setup.ui.options.display.DisplayOptionsContract.Validator
import net.thunderbird.core.common.domain.usecase.validation.ValidationResult

internal class DisplayOptionsValidator(
    private val accountNameValidator: ValidateAccountName = ValidateAccountName(),
    private val displayNameValidator: ValidateDisplayName = ValidateDisplayName(),
    private val emailSignatureValidator: ValidateEmailSignature = ValidateEmailSignature(),
) : Validator {
    override fun validateAccountName(accountName: String): ValidationResult {
        return accountNameValidator.execute(accountName)
    }

    override fun validateDisplayName(displayName: String): ValidationResult {
        return displayNameValidator.execute(displayName)
    }

    override fun validateEmailSignature(emailSignature: String): ValidationResult {
        return emailSignatureValidator.execute(emailSignature)
    }
}
