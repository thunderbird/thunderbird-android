package app.k9mail.feature.account.setup.ui.autoconfig

import app.k9mail.core.common.domain.usecase.validation.ValidationResult

class FakeAccountAutoConfigValidator(
    private val emailAddressAnswer: ValidationResult = ValidationResult.Success,
    private val passwordAnswer: ValidationResult = ValidationResult.Success,
) : AccountAutoConfigContract.Validator {
    override suspend fun validateEmailAddress(emailAddress: String): ValidationResult = emailAddressAnswer
    override suspend fun validatePassword(password: String): ValidationResult = passwordAnswer
}
