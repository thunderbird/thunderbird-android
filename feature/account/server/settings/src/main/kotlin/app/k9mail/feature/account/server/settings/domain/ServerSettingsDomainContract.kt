package app.k9mail.feature.account.server.settings.domain

import app.k9mail.core.common.domain.usecase.validation.ValidationResult

interface ServerSettingsDomainContract {

    interface UseCase {

        fun interface ValidatePassword {
            fun execute(password: String): ValidationResult
        }

        fun interface ValidateServer {
            fun execute(server: String): ValidationResult
        }

        fun interface ValidatePort {
            fun execute(port: Long?): ValidationResult
        }

        fun interface ValidateUsername {
            fun execute(username: String): ValidationResult
        }

        fun interface ValidateImapPrefix {
            fun execute(imapPrefix: String): ValidationResult
        }
    }
}
