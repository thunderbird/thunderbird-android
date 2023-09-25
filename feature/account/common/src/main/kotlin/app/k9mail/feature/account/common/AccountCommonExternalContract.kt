package app.k9mail.feature.account.common

import app.k9mail.feature.account.common.domain.entity.AccountState

interface AccountCommonExternalContract {

    fun interface AccountStateLoader {
        suspend fun loadAccountState(accountUuid: String): AccountState?
    }
}
