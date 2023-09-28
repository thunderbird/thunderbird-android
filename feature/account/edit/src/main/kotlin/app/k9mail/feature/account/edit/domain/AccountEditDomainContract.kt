package app.k9mail.feature.account.edit.domain

import app.k9mail.feature.account.common.domain.entity.AccountState

interface AccountEditDomainContract {

    interface UseCase {

        fun interface LoadAccountState {
            suspend fun execute(accountUuid: String): AccountState
        }
    }
}
