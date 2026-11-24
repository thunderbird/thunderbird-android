package app.k9mail.feature.account.edit.domain

import app.k9mail.feature.account.common.domain.entity.AccountState
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
    }
}
