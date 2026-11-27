package app.k9mail.feature.account.edit.ui.server.settings.modify

import net.thunderbird.core.validation.ValidationOutcome
import net.thunderbird.core.validation.ValidationSuccess
import net.thunderbird.feature.account.server.settings.ui.outgoing.OutgoingServerSettingsContract

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
