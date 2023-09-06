package app.k9mail.feature.account.edit.domain.usecase

import app.k9mail.feature.account.common.AccountCommonExternalContract
import app.k9mail.feature.account.common.domain.AccountDomainContract
import app.k9mail.feature.account.common.domain.entity.Account
import app.k9mail.feature.account.common.domain.entity.AccountState
import app.k9mail.feature.account.common.domain.entity.AuthorizationState
import app.k9mail.feature.account.edit.domain.AccountEditDomainContract

class LoadAccountState(
    private val accountLoader: AccountCommonExternalContract.AccountLoader,
    private val accountStateRepository: AccountDomainContract.AccountStateRepository,
) : AccountEditDomainContract.UseCase.LoadAccountState {
    override suspend fun execute(accountUuid: String): AccountState {
        val accountState = accountStateRepository.getState()
        return if (accountState.uuid == accountUuid) {
            accountState
        } else {
            loadState(accountUuid)
        }
    }

    private suspend fun loadState(accountUuid: String): AccountState {
        val account = accountLoader.loadAccount(accountUuid)
        return if (account != null) {
            val accountState = account.mapToAccountState()
            accountState
        } else {
            AccountState(uuid = accountUuid)
        }.also { accountStateRepository.setState(it) }
    }

    private fun Account.mapToAccountState() = AccountState(
        uuid = uuid,
        emailAddress = emailAddress,
        incomingServerSettings = incomingServerSettings,
        outgoingServerSettings = outgoingServerSettings,
        authorizationState = AuthorizationState(authorizationState),
        options = options,
    )
}
