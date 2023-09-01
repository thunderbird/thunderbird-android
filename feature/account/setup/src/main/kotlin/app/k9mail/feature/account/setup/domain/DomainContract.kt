package app.k9mail.feature.account.setup.domain

import app.k9mail.autodiscovery.api.AutoDiscoveryResult
import app.k9mail.core.common.domain.usecase.validation.ValidationResult
import app.k9mail.feature.account.common.domain.entity.AccountOptions
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.server.ServerSettingsValidationResult
import java.security.cert.X509Certificate

interface DomainContract {

    interface UseCase {
        fun interface GetAutoDiscovery {
            suspend fun execute(emailAddress: String): AutoDiscoveryResult
        }

        fun interface ValidateServerSettings {
            suspend fun execute(settings: ServerSettings): ServerSettingsValidationResult
        }

        fun interface AddServerCertificateException {
            suspend fun addCertificate(hostname: String, port: Int, certificate: X509Certificate?)
        }

        fun interface CreateAccount {
            suspend fun execute(
                emailAddress: String,
                incomingServerSettings: ServerSettings,
                outgoingServerSettings: ServerSettings,
                authorizationState: String?,
                options: AccountOptions,
            ): String
        }

        fun interface ValidateEmailAddress {
            fun execute(emailAddress: String): ValidationResult
        }

        fun interface ValidatePassword {
            fun execute(password: String): ValidationResult
        }

        fun interface ValidateConfigurationApproval {
            fun execute(isApproved: Boolean?, isAutoDiscoveryTrusted: Boolean?): ValidationResult
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

        fun interface ValidateAccountName {
            fun execute(accountName: String): ValidationResult
        }

        fun interface ValidateDisplayName {
            fun execute(displayName: String): ValidationResult
        }

        fun interface ValidateEmailSignature {
            fun execute(emailSignature: String): ValidationResult
        }
    }
}
