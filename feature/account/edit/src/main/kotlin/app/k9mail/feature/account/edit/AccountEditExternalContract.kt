package app.k9mail.feature.account.edit

import app.k9mail.feature.account.common.domain.entity.Account

interface AccountEditExternalContract {

    fun interface AccountUpdater {
        suspend fun updateAccount(account: Account): AccountUpdaterResult

        sealed interface AccountUpdaterResult {
            data class Success(val accountId: String) : AccountUpdaterResult
            data class Error(val message: String) : AccountUpdaterResult
        }
    }
}
