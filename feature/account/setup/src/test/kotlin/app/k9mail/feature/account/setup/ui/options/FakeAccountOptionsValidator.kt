package app.k9mail.feature.account.setup.ui.options

import app.k9mail.core.common.domain.usecase.validation.ValidationResult
import app.k9mail.feature.account.setup.ui.options.AccountOptionsContract.Validator

internal class FakeAccountOptionsValidator(
    private val accountNameAnswer: ValidationResult = ValidationResult.Success,
    private val displayNameAnswer: ValidationResult = ValidationResult.Success,
    private val emailSignatureAnswer: ValidationResult = ValidationResult.Success,
) : Validator {
    override suspend fun validateAccountName(accountName: String): ValidationResult = accountNameAnswer
    override suspend fun validateDisplayName(displayName: String): ValidationResult = displayNameAnswer
    override suspend fun validateEmailSignature(emailSignature: String): ValidationResult = emailSignatureAnswer
}
