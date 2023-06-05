package app.k9mail.feature.account.setup.ui.incoming

import app.k9mail.core.common.domain.usecase.validation.ValidationResult

class FakeAccountIncomingConfigValidator(
    private val serverAnswer: ValidationResult = ValidationResult.Success,
    private val portAnswer: ValidationResult = ValidationResult.Success,
    private val usernameAnswer: ValidationResult = ValidationResult.Success,
    private val passwordAnswer: ValidationResult = ValidationResult.Success,
    private val imapPrefixAnswer: ValidationResult = ValidationResult.Success,
) : AccountIncomingConfigContract.Validator {
    override fun validateServer(server: String): ValidationResult = serverAnswer
    override fun validatePort(port: Long?): ValidationResult = portAnswer
    override fun validateUsername(username: String): ValidationResult = usernameAnswer
    override fun validatePassword(password: String): ValidationResult = passwordAnswer
    override fun validateImapPrefix(imapPrefix: String): ValidationResult = imapPrefixAnswer
}
