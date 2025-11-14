package app.k9mail.feature.account.server.settings.ui.outgoing

import net.thunderbird.core.validation.ValidationOutcome
import net.thunderbird.core.validation.ValidationSuccess

class FakeOutgoingServerSettingsValidator(
    private val serverAnswer: ValidationOutcome = ValidationSuccess,
    private val portAnswer: ValidationOutcome = ValidationSuccess,
    private val usernameAnswer: ValidationOutcome = ValidationSuccess,
    private val passwordAnswer: ValidationOutcome = ValidationSuccess,
) : OutgoingServerSettingsContract.Validator {
    override fun validateServer(server: String): ValidationOutcome = serverAnswer
    override fun validatePort(port: Long?): ValidationOutcome = portAnswer
    override fun validateUsername(username: String): ValidationOutcome = usernameAnswer
    override fun validatePassword(password: String): ValidationOutcome = passwordAnswer
}
