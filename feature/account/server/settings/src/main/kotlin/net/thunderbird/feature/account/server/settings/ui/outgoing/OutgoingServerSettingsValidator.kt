package net.thunderbird.feature.account.server.settings.ui.outgoing

import net.thunderbird.core.validation.ValidationOutcome
import net.thunderbird.feature.account.server.settings.domain.usecase.ValidatePassword
import net.thunderbird.feature.account.server.settings.domain.usecase.ValidatePort
import net.thunderbird.feature.account.server.settings.domain.usecase.ValidateServer
import net.thunderbird.feature.account.server.settings.domain.usecase.ValidateUsername

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
