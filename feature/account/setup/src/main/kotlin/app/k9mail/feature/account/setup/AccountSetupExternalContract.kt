package app.k9mail.feature.account.setup

import app.k9mail.feature.account.common.domain.entity.Account
import net.thunderbird.feature.account.AccountId

interface AccountSetupExternalContract {

    fun interface AccountCreator {
        suspend fun createAccount(account: Account): AccountCreatorResult

        sealed interface AccountCreatorResult {
            data class Success(val accountId: AccountId) : AccountCreatorResult
            data class Error(val message: String) : AccountCreatorResult
        }
    }

    fun interface AccountOwnerNameProvider {
        suspend fun getOwnerName(): String?
    }
}
