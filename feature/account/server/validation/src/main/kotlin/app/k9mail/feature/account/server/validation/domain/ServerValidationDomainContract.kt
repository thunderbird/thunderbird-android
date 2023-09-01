package app.k9mail.feature.account.server.validation.domain

import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.server.ServerSettingsValidationResult

interface ServerValidationDomainContract {

    interface UseCase {

        fun interface ValidateServerSettings {
            suspend fun execute(settings: ServerSettings): ServerSettingsValidationResult
        }
    }
}
