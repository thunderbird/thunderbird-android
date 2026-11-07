package net.thunderbird.app.common.feature.account.usecase

import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountRepository
import net.thunderbird.feature.account.usecase.DeleteAccount

class DefaultDeleteAccount(
    private val accountRepository: AccountRepository,
) : DeleteAccount {
    override suspend fun invoke(accountId: AccountId) {
        accountRepository.delete(accountId)
    }
}
