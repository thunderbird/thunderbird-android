package app.k9mail.feature.account.setup.domain

import app.k9mail.autodiscovery.api.AutoDiscoveryResult
import app.k9mail.core.common.domain.usecase.validation.ValidationResult
import app.k9mail.feature.account.common.domain.entity.AccountOptions
import com.fsck.k9.mail.ServerSettings

interface DomainContract {

    interface UseCase {
        fun interface GetAutoDiscovery {
            suspend fun execute(emailAddress: String): AutoDiscoveryResult
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

        fun interface ValidateConfigurationApproval {
            fun execute(isApproved: Boolean?, isAutoDiscoveryTrusted: Boolean?): ValidationResult
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
