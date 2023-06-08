package app.k9mail.feature.account.setup.ui.autoconfig

import app.k9mail.core.common.domain.usecase.validation.ValidationResult
import app.k9mail.feature.account.setup.domain.usecase.ValidateEmailAddress
import app.k9mail.feature.account.setup.domain.usecase.ValidatePassword

class AccountAutoConfigValidator(
    private val emailAddressValidator: ValidateEmailAddress = ValidateEmailAddress(),
    private val passwordValidator: ValidatePassword = ValidatePassword(),
) : AccountAutoConfigContract.Validator {

    override suspend fun validateEmailAddress(emailAddress: String): ValidationResult {
        return emailAddressValidator.execute(emailAddress)
    }

    override suspend fun validatePassword(password: String): ValidationResult {
        return passwordValidator.execute(password)
    }
}
