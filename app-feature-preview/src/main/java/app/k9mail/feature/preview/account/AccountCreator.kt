package app.k9mail.feature.preview.account

import app.k9mail.feature.account.common.domain.entity.Account
import app.k9mail.feature.account.setup.AccountSetupExternalContract
import app.k9mail.feature.account.setup.AccountSetupExternalContract.AccountCreator.AccountCreatorResult
import java.util.UUID

class AccountCreator : AccountSetupExternalContract.AccountCreator {

    override suspend fun createAccount(account: Account): AccountCreatorResult {
        return AccountCreatorResult.Success(UUID.randomUUID().toString())
    }
}
