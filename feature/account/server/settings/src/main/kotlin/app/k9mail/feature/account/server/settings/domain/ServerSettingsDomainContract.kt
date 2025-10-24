package app.k9mail.feature.account.server.settings.domain

import net.thunderbird.core.validation.ValidationOutcome

interface ServerSettingsDomainContract {

    interface UseCase {

        fun interface ValidatePassword {
            fun execute(password: String): ValidationOutcome
        }

        fun interface ValidateServer {
            fun execute(server: String): ValidationOutcome
        }

        fun interface ValidatePort {
            fun execute(port: Long?): ValidationOutcome
        }

        fun interface ValidateUsername {
            fun execute(username: String): ValidationOutcome
        }

        fun interface ValidateImapPrefix {
            fun execute(imapPrefix: String): ValidationOutcome
        }
    }
}
