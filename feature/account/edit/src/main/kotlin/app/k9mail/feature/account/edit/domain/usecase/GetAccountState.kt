package app.k9mail.feature.account.edit.domain.usecase

import app.k9mail.feature.account.common.domain.AccountDomainContract
import app.k9mail.feature.account.common.domain.entity.AccountState
import app.k9mail.feature.account.edit.domain.AccountEditDomainContract.UseCase
import net.thunderbird.feature.account.AccountId

class GetAccountState(
    private val accountStateRepository: AccountDomainContract.AccountStateRepository,
) : UseCase.GetAccountState {
    override suspend fun execute(accountId: AccountId): AccountState {
        val accountState = accountStateRepository.getState()
        return if (accountState.id == accountId) {
            accountState
        } else {
            error("Account state for $accountId not found")
        }
    }
}
