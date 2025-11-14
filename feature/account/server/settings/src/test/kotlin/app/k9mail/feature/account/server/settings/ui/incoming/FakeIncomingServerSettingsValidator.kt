package app.k9mail.feature.account.server.settings.ui.incoming

import net.thunderbird.core.validation.ValidationOutcome
import net.thunderbird.core.validation.ValidationSuccess

class FakeIncomingServerSettingsValidator(
    private val serverAnswer: ValidationOutcome = ValidationSuccess,
    private val portAnswer: ValidationOutcome = ValidationSuccess,
    private val usernameAnswer: ValidationOutcome = ValidationSuccess,
    private val passwordAnswer: ValidationOutcome = ValidationSuccess,
    private val imapPrefixAnswer: ValidationOutcome = ValidationSuccess,
) : IncomingServerSettingsContract.Validator {
    override fun validateServer(server: String): ValidationOutcome = serverAnswer
    override fun validatePort(port: Long?): ValidationOutcome = portAnswer
    override fun validateUsername(username: String): ValidationOutcome = usernameAnswer
    override fun validatePassword(password: String): ValidationOutcome = passwordAnswer
    override fun validateImapPrefix(imapPrefix: String): ValidationOutcome = imapPrefixAnswer
}
