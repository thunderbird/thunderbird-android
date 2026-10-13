package app.k9mail.feature.account.edit.domain

import app.k9mail.feature.account.common.domain.entity.AccountState
import app.k9mail.feature.account.common.domain.entity.AuthorizationState
import app.k9mail.feature.account.edit.domain.GraphAccountReauthorizationResult
import app.k9mail.feature.account.oauth.domain.entity.OAuthResult
import net.thunderbird.feature.account.AccountId

interface AccountEditDomainContract {

    interface UseCase {

        fun interface LoadAccountState {
            suspend fun execute(accountId: AccountId): AccountState
        }

        fun interface GetAccountState {
            suspend fun execute(accountId: AccountId): AccountState
        }

        fun interface SaveServerSettings {
            suspend fun execute(accountId: AccountId, isIncoming: Boolean)
        }

        fun interface ReauthorizeGraphAccount {
            suspend fun execute(accountId: AccountId, result: OAuthResult): GraphAccountReauthorizationResult
        }

        fun interface ReauthorizeNativeApiAccount {
            suspend fun execute(accountId: AccountId, result: OAuthResult): ApiAccountReauthorizationResult
        }
    }

    fun interface MicrosoftAccountIdentityVerifier {
        fun matches(
            accountEmail: String?,
            currentAuthorizationState: AuthorizationState?,
            newAuthorizationState: AuthorizationState,
            clientId: String,
        ): Boolean
    }
}
