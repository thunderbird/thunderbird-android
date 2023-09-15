package app.k9mail.feature.preview.account

import app.k9mail.feature.account.common.domain.entity.Account
import app.k9mail.feature.account.edit.AccountEditExternalContract.AccountUpdater
import app.k9mail.feature.account.edit.AccountEditExternalContract.AccountUpdater.AccountUpdaterResult
import app.k9mail.feature.account.setup.AccountSetupExternalContract.AccountCreator
import app.k9mail.feature.account.setup.AccountSetupExternalContract.AccountCreator.AccountCreatorResult

class InMemoryAccountStore(
    private val accountMap: MutableMap<String, Account> = mutableMapOf(),
) : AccountCreator, AccountUpdater {

    override suspend fun createAccount(account: Account): AccountCreatorResult {
        accountMap[account.uuid] = account

        return AccountCreatorResult.Success(account.uuid)
    }

    override suspend fun updateAccount(account: Account): AccountUpdaterResult {
        return if (!accountMap.containsKey(account.uuid)) {
            AccountUpdaterResult.Error("Account not found")
        } else {
            accountMap[account.uuid] = account

            AccountUpdaterResult.Success(account.uuid)
        }
    }
}
