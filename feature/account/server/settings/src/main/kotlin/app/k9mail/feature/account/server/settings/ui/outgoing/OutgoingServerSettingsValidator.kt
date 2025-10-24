package app.k9mail.feature.account.server.settings.ui.outgoing

import app.k9mail.feature.account.server.settings.domain.usecase.ValidatePassword
import app.k9mail.feature.account.server.settings.domain.usecase.ValidatePort
import app.k9mail.feature.account.server.settings.domain.usecase.ValidateServer
import app.k9mail.feature.account.server.settings.domain.usecase.ValidateUsername
import net.thunderbird.core.validation.ValidationOutcome

internal class OutgoingServerSettingsValidator(
    private val serverValidator: ValidateServer = ValidateServer(),
    private val portValidator: ValidatePort = ValidatePort(),
    private val usernameValidator: ValidateUsername = ValidateUsername(),
    private val passwordValidator: ValidatePassword = ValidatePassword(),
) : OutgoingServerSettingsContract.Validator {
    override fun validateServer(server: String): ValidationOutcome {
        return serverValidator.execute(server)
    }

    override fun validatePort(port: Long?): ValidationOutcome {
        return portValidator.execute(port)
    }

    override fun validateUsername(username: String): ValidationOutcome {
        return usernameValidator.execute(username)
    }

    override fun validatePassword(password: String): ValidationOutcome {
        return passwordValidator.execute(password)
    }
}
