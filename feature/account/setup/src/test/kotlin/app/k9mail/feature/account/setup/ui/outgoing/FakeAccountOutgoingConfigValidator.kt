package app.k9mail.feature.account.setup.ui.outgoing

import app.k9mail.core.common.domain.usecase.validation.ValidationResult

class FakeAccountOutgoingConfigValidator(
    private val serverAnswer: ValidationResult = ValidationResult.Success,
    private val portAnswer: ValidationResult = ValidationResult.Success,
    private val usernameAnswer: ValidationResult = ValidationResult.Success,
    private val passwordAnswer: ValidationResult = ValidationResult.Success,
) : AccountOutgoingConfigContract.Validator {
    override suspend fun validateServer(server: String): ValidationResult = serverAnswer
    override suspend fun validatePort(port: Long?): ValidationResult = portAnswer
    override suspend fun validateUsername(username: String): ValidationResult = usernameAnswer
    override suspend fun validatePassword(password: String): ValidationResult = passwordAnswer
}
