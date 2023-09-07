package app.k9mail.feature.account.server.config.ui.incoming

import app.k9mail.core.common.domain.usecase.validation.ValidationResult
import app.k9mail.feature.account.server.config.domain.ServerConfigDomainContract.UseCase
import app.k9mail.feature.account.server.config.domain.usecase.ValidateImapPrefix
import app.k9mail.feature.account.server.config.domain.usecase.ValidatePassword
import app.k9mail.feature.account.server.config.domain.usecase.ValidatePort
import app.k9mail.feature.account.server.config.domain.usecase.ValidateServer
import app.k9mail.feature.account.server.config.domain.usecase.ValidateUsername

internal class AccountIncomingConfigValidator(
    private val serverValidator: UseCase.ValidateServer = ValidateServer(),
    private val portValidator: UseCase.ValidatePort = ValidatePort(),
    private val usernameValidator: UseCase.ValidateUsername = ValidateUsername(),
    private val passwordValidator: UseCase.ValidatePassword = ValidatePassword(),
    private val imapPrefixValidator: UseCase.ValidateImapPrefix = ValidateImapPrefix(),
) : AccountIncomingConfigContract.Validator {
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
