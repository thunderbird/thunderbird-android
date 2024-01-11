package app.k9mail.feature.account.setup.ui.options.display

import app.k9mail.core.common.domain.usecase.validation.ValidationResult
import app.k9mail.feature.account.setup.ui.options.display.DisplayOptionsContract.Validator

internal class FakeDisplayOptionsValidator(
    private val accountNameAnswer: ValidationResult = ValidationResult.Success,
    private val displayNameAnswer: ValidationResult = ValidationResult.Success,
    private val emailSignatureAnswer: ValidationResult = ValidationResult.Success,
) : Validator {
    override fun validateAccountName(accountName: String): ValidationResult = accountNameAnswer
    override fun validateDisplayName(displayName: String): ValidationResult = displayNameAnswer
    override fun validateEmailSignature(emailSignature: String): ValidationResult = emailSignatureAnswer
}
