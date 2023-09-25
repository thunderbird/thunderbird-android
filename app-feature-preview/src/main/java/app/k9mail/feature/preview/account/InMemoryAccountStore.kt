package app.k9mail.feature.preview.account

import app.k9mail.feature.account.common.AccountCommonExternalContract.AccountStateLoader
import app.k9mail.feature.account.common.domain.entity.Account
import app.k9mail.feature.account.common.domain.entity.AccountState
import app.k9mail.feature.account.common.domain.entity.AuthorizationState
import app.k9mail.feature.account.edit.AccountEditExternalContract.AccountUpdater
import app.k9mail.feature.account.edit.AccountEditExternalContract.AccountUpdater.AccountUpdaterResult
import app.k9mail.feature.account.setup.AccountSetupExternalContract.AccountCreator
import app.k9mail.feature.account.setup.AccountSetupExternalContract.AccountCreator.AccountCreatorResult

class InMemoryAccountStore(
    private val accountMap: MutableMap<String, Account> = mutableMapOf(),
) : AccountCreator, AccountUpdater, AccountStateLoader {

    suspend fun load(accountUuid: String): Account? {
        return accountMap[accountUuid]
    }

    override suspend fun loadAccountState(accountUuid: String): AccountState? {
        return accountMap[accountUuid]?.let { mapToAccountState(it) }
    }

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

    private fun mapToAccountState(account: Account): AccountState {
        return AccountState(
            uuid = account.uuid,
            emailAddress = account.emailAddress,
            incomingServerSettings = account.incomingServerSettings,
            outgoingServerSettings = account.outgoingServerSettings,
            authorizationState = account.authorizationState?.let { AuthorizationState(it) },
            options = account.options,
        )
    }
}
