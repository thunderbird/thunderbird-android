package app.k9mail.feature.preview.account

import app.k9mail.feature.account.common.AccountCommonExternalContract.AccountStateLoader
import app.k9mail.feature.account.common.domain.entity.Account
import app.k9mail.feature.account.common.domain.entity.AccountDisplayOptions
import app.k9mail.feature.account.common.domain.entity.AccountOptions
import app.k9mail.feature.account.common.domain.entity.AccountState
import app.k9mail.feature.account.common.domain.entity.AccountSyncOptions
import app.k9mail.feature.account.common.domain.entity.AuthorizationState
import app.k9mail.feature.account.edit.AccountEditExternalContract.AccountServerSettingsUpdater
import app.k9mail.feature.account.edit.AccountEditExternalContract.AccountUpdaterFailure
import app.k9mail.feature.account.edit.AccountEditExternalContract.AccountUpdaterResult
import app.k9mail.feature.account.setup.AccountSetupExternalContract.AccountCreator
import app.k9mail.feature.account.setup.AccountSetupExternalContract.AccountCreator.AccountCreatorResult
import com.fsck.k9.mail.ServerSettings

class InMemoryAccountStore(
    private val accountMap: MutableMap<String, Account> = mutableMapOf(),
) : AccountCreator, AccountServerSettingsUpdater, AccountStateLoader {

    override suspend fun loadAccountState(accountUuid: String): AccountState? {
        return accountMap[accountUuid]?.let { mapToAccountState(it) }
    }

    override suspend fun createAccount(account: Account): AccountCreatorResult {
        accountMap[account.uuid] = account

        return AccountCreatorResult.Success(account.uuid)
    }

    override suspend fun updateServerSettings(
        accountUuid: String,
        isIncoming: Boolean,
        serverSettings: ServerSettings,
        authorizationState: AuthorizationState?,
    ): AccountUpdaterResult {
        return if (!accountMap.containsKey(accountUuid)) {
            AccountUpdaterResult.Failure(AccountUpdaterFailure.AccountNotFound(accountUuid))
        } else {
            val account = accountMap[accountUuid]!!

            accountMap[account.uuid] = if (isIncoming) {
                account.copy(
                    incomingServerSettings = serverSettings,
                    authorizationState = authorizationState?.value,
                )
            } else {
                account.copy(
                    outgoingServerSettings = serverSettings,
                    authorizationState = authorizationState?.value,
                )
            }

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
            displayOptions = mapToDisplayOptions(account.options),
            syncOptions = mapToSyncOptions(account.options),
        )
    }

    private fun mapToDisplayOptions(options: AccountOptions): AccountDisplayOptions {
        return AccountDisplayOptions(
            accountName = options.accountName,
            displayName = options.displayName,
            emailSignature = options.emailSignature,
        )
    }

    private fun mapToSyncOptions(options: AccountOptions): AccountSyncOptions {
        return AccountSyncOptions(
            checkFrequencyInMinutes = options.checkFrequencyInMinutes,
            messageDisplayCount = options.messageDisplayCount,
            showNotification = options.showNotification,
        )
    }
}
