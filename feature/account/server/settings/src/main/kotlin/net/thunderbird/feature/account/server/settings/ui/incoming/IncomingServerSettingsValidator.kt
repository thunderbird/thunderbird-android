package net.thunderbird.feature.account.server.settings.ui.incoming

import net.thunderbird.core.validation.ValidationOutcome
import net.thunderbird.feature.account.server.settings.domain.ServerSettingsDomainContract.UseCase
import net.thunderbird.feature.account.server.settings.domain.usecase.ValidateImapPrefix
import net.thunderbird.feature.account.server.settings.domain.usecase.ValidatePassword
import net.thunderbird.feature.account.server.settings.domain.usecase.ValidatePort
import net.thunderbird.feature.account.server.settings.domain.usecase.ValidateServer
import net.thunderbird.feature.account.server.settings.domain.usecase.ValidateUsername

internal class IncomingServerSettingsValidator(
    private val serverValidator: UseCase.ValidateServer = ValidateServer(),
    private val portValidator: UseCase.ValidatePort = ValidatePort(),
    private val usernameValidator: UseCase.ValidateUsername = ValidateUsername(),
    private val passwordValidator: UseCase.ValidatePassword = ValidatePassword(),
    private val imapPrefixValidator: UseCase.ValidateImapPrefix = ValidateImapPrefix(),
) : IncomingServerSettingsContract.Validator {
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

    override fun validateImapPrefix(imapPrefix: String): ValidationOutcome {
        return imapPrefixValidator.execute(imapPrefix)
    }
}
