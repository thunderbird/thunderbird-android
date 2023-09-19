package app.k9mail.feature.account.edit.domain.usecase

import app.k9mail.feature.account.common.domain.AccountDomainContract
import app.k9mail.feature.account.common.domain.entity.AccountState
import app.k9mail.feature.account.edit.domain.AccountEditDomainContract.UseCase

class GetAccountState(
    private val accountStateRepository: AccountDomainContract.AccountStateRepository,
) : UseCase.GetAccountState {
    override suspend fun execute(accountUuid: String): AccountState {
        val accountState = accountStateRepository.getState()
        return if (accountState.uuid == accountUuid) {
            accountState
        } else {
            error("Account state for $accountUuid not found")
        }
    }
}
