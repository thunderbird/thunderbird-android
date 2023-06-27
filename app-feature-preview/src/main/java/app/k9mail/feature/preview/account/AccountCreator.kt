package app.k9mail.feature.preview.account

import app.k9mail.feature.account.setup.domain.ExternalContract
import app.k9mail.feature.account.setup.domain.entity.Account
import java.util.UUID

class AccountCreator : ExternalContract.AccountCreator {

    override suspend fun createAccount(account: Account): String {
        return UUID.randomUUID().toString()
    }
}
