package app.k9mail.feature.account.server.config.ui.outgoing.fake

import app.k9mail.core.common.domain.usecase.validation.ValidationResult
import app.k9mail.feature.account.server.config.ui.outgoing.AccountOutgoingConfigContract

class FakeAccountOutgoingConfigValidator(
    private val serverAnswer: ValidationResult = ValidationResult.Success,
    private val portAnswer: ValidationResult = ValidationResult.Success,
    private val usernameAnswer: ValidationResult = ValidationResult.Success,
    private val passwordAnswer: ValidationResult = ValidationResult.Success,
) : AccountOutgoingConfigContract.Validator {
    override fun validateServer(server: String): ValidationResult = serverAnswer
    override fun validatePort(port: Long?): ValidationResult = portAnswer
    override fun validateUsername(username: String): ValidationResult = usernameAnswer
    override fun validatePassword(password: String): ValidationResult = passwordAnswer
}
