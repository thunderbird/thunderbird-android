package app.k9mail.feature.account.setup.domain

import app.k9mail.autodiscovery.api.AutoDiscoveryResult
import app.k9mail.feature.account.common.domain.entity.AccountState
import app.k9mail.feature.account.common.domain.entity.SpecialFolderOptions
import app.k9mail.feature.account.setup.AccountSetupExternalContract.AccountCreator.AccountCreatorResult
import net.thunderbird.core.validation.ValidationError
import net.thunderbird.core.validation.ValidationOutcome

interface DomainContract {

    interface UseCase {
        fun interface GetAutoDiscovery {
            suspend fun execute(emailAddress: String): AutoDiscoveryResult
        }

        fun interface CreateAccount {
            suspend fun execute(accountState: AccountState): AccountCreatorResult
        }

        fun interface ValidateEmailAddress {
            fun execute(emailAddress: String): ValidationOutcome
        }

        fun interface ValidateConfigurationApproval {
            fun execute(isApproved: Boolean?, isAutoDiscoveryTrusted: Boolean?): ValidationOutcome
        }

        fun interface ValidateAccountName {
            fun execute(accountName: String): ValidationOutcome
        }

        fun interface ValidateDisplayName {
            fun execute(displayName: String): ValidationOutcome
        }

        fun interface ValidateEmailSignature {
            fun execute(emailSignature: String): ValidationOutcome
        }

        fun interface GetSpecialFolderOptions {
            suspend operator fun invoke(): SpecialFolderOptions
        }

        fun interface ValidateSpecialFolderOptions {
            operator fun invoke(specialFolderOptions: SpecialFolderOptions): ValidationOutcome

            sealed interface Failure : ValidationError {
                data object MissingDefaultSpecialFolderOption : Failure
            }
        }
    }
}
