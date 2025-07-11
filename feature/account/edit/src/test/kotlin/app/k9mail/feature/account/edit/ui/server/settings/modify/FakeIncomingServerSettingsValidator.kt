package app.k9mail.feature.account.edit.ui.server.settings.modify

import app.k9mail.feature.account.server.settings.ui.incoming.IncomingServerSettingsContract
import net.thunderbird.core.common.domain.usecase.validation.ValidationResult

class FakeIncomingServerSettingsValidator(
    private val serverAnswer: ValidationResult = ValidationResult.Success,
    private val portAnswer: ValidationResult = ValidationResult.Success,
    private val usernameAnswer: ValidationResult = ValidationResult.Success,
    private val passwordAnswer: ValidationResult = ValidationResult.Success,
    private val imapPrefixAnswer: ValidationResult = ValidationResult.Success,
) : IncomingServerSettingsContract.Validator {
    override fun validateServer(server: String): ValidationResult = serverAnswer
    override fun validatePort(port: Long?): ValidationResult = portAnswer
    override fun validateUsername(username: String): ValidationResult = usernameAnswer
    override fun validatePassword(password: String): ValidationResult = passwordAnswer
    override fun validateImapPrefix(imapPrefix: String): ValidationResult = imapPrefixAnswer
}
