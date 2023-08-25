package app.k9mail.feature.account.setup.domain

import app.k9mail.autodiscovery.api.AutoDiscoveryResult
import app.k9mail.core.common.domain.usecase.validation.ValidationResult
import app.k9mail.feature.account.oauth.domain.entity.AuthorizationState
import app.k9mail.feature.account.setup.domain.entity.AccountOptions
import app.k9mail.feature.account.setup.domain.entity.AccountSetupState
import app.k9mail.feature.account.setup.domain.entity.CertificateError
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.mail.server.ServerSettingsValidationResult
import java.security.cert.X509Certificate

interface DomainContract {

    interface AccountSetupStateRepository {
        fun getState(): AccountSetupState

        fun save(accountSetupState: AccountSetupState)

        fun saveEmailAddress(emailAddress: String)

        fun saveIncomingServerSettings(serverSettings: ServerSettings)

        fun saveOutgoingServerSettings(serverSettings: ServerSettings)

        fun saveAuthorizationState(authorizationState: AuthorizationState)

        fun saveOptions(options: AccountOptions)

        fun clear()
    }

    interface CertificateErrorRepository {
        fun getCertificateError(): CertificateError?

        fun setCertificateError(certificateError: CertificateError)

        fun clearCertificateError()
    }

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
