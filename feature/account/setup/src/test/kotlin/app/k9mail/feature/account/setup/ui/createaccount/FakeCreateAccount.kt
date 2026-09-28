package app.k9mail.feature.account.setup.ui.createaccount

import app.k9mail.feature.account.common.domain.entity.AccountState
import app.k9mail.feature.account.setup.AccountSetupExternalContract.AccountCreator.AccountCreatorResult
import app.k9mail.feature.account.setup.domain.DomainContract.UseCase.CreateAccount
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountIdFactory

class FakeCreateAccount : CreateAccount {
    val recordedInvocations = mutableListOf<AccountState>()

    var result: AccountCreatorResult = AccountCreatorResult.Success(AccountIdFactory.create())

    override suspend fun execute(
        accountState: AccountState,
    ): AccountCreatorResult {
        recordedInvocations.add(accountState)

        return result
    }
}
