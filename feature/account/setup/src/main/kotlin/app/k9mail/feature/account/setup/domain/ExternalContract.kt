package app.k9mail.feature.account.setup.domain

import app.k9mail.feature.account.setup.domain.entity.Account

interface ExternalContract {

    fun interface AccountCreator {
        suspend fun createAccount(account: Account): String

        sealed interface AccountCreatorResult {
            object Success : AccountCreatorResult
            data class Error(val message: String) : AccountCreatorResult
        }
    }

    fun interface AccountOwnerNameProvider {
        fun getOwnerName(): String?
    }

    fun interface AccountSetupFinishedLauncher {
        fun launch(accountUuid: String)
    }
}
