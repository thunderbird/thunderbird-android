package app.k9mail.feature.account.common

import app.k9mail.feature.account.common.domain.entity.AccountState
import net.thunderbird.feature.account.AccountId

interface AccountCommonExternalContract {

    fun interface AccountStateLoader {
        suspend fun loadAccountState(accountId: AccountId): AccountState?
    }
}
