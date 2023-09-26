package app.k9mail.feature.account.edit.domain.usecase

import app.k9mail.feature.account.common.AccountCommonExternalContract
import app.k9mail.feature.account.common.domain.AccountDomainContract
import app.k9mail.feature.account.common.domain.entity.AccountState
import app.k9mail.feature.account.edit.domain.AccountEditDomainContract

class LoadAccountState(
    private val accountStateLoader: AccountCommonExternalContract.AccountStateLoader,
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
        val accountState = accountStateLoader.loadAccountState(accountUuid)
        return if (accountState != null) {
            accountState
        } else {
            AccountState(uuid = accountUuid)
        }.also { accountStateRepository.setState(it) }
    }
}
