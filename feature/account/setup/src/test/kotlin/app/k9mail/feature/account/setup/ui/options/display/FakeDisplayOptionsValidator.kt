package app.k9mail.feature.account.setup.ui.options.display

import app.k9mail.feature.account.setup.ui.options.display.DisplayOptionsContract.Validator
import net.thunderbird.core.validation.ValidationOutcome
import net.thunderbird.core.validation.ValidationSuccess

internal class FakeDisplayOptionsValidator(
    private val accountNameAnswer: ValidationOutcome = ValidationSuccess,
    private val displayNameAnswer: ValidationOutcome = ValidationSuccess,
    private val emailSignatureAnswer: ValidationOutcome = ValidationSuccess,
) : Validator {
    override fun validateAccountName(accountName: String): ValidationOutcome = accountNameAnswer
    override fun validateDisplayName(displayName: String): ValidationOutcome = displayNameAnswer
    override fun validateEmailSignature(emailSignature: String): ValidationOutcome = emailSignatureAnswer
}
