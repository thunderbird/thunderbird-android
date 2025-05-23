package app.k9mail.feature.account.server.settings.ui.incoming

import app.k9mail.feature.account.server.settings.domain.ServerSettingsDomainContract.UseCase
import app.k9mail.feature.account.server.settings.domain.usecase.ValidateImapPrefix
import app.k9mail.feature.account.server.settings.domain.usecase.ValidatePassword
import app.k9mail.feature.account.server.settings.domain.usecase.ValidatePort
import app.k9mail.feature.account.server.settings.domain.usecase.ValidateServer
import app.k9mail.feature.account.server.settings.domain.usecase.ValidateUsername
import net.thunderbird.core.common.domain.usecase.validation.ValidationResult

internal class IncomingServerSettingsValidator(
    private val serverValidator: UseCase.ValidateServer = ValidateServer(),
    private val portValidator: UseCase.ValidatePort = ValidatePort(),
    private val usernameValidator: UseCase.ValidateUsername = ValidateUsername(),
    private val passwordValidator: UseCase.ValidatePassword = ValidatePassword(),
    private val imapPrefixValidator: UseCase.ValidateImapPrefix = ValidateImapPrefix(),
) : IncomingServerSettingsContract.Validator {
    override fun validateServer(server: String): ValidationResult {
        return serverValidator.execute(server)
    }

    override fun validatePort(port: Long?): ValidationResult {
        return portValidator.execute(port)
    }

    override fun validateUsername(username: String): ValidationResult {
        return usernameValidator.execute(username)
    }

    override fun validatePassword(password: String): ValidationResult {
        return passwordValidator.execute(password)
    }

    override fun validateImapPrefix(imapPrefix: String): ValidationResult {
        return imapPrefixValidator.execute(imapPrefix)
    }
}
