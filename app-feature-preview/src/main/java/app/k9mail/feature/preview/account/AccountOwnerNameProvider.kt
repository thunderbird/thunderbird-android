package app.k9mail.feature.preview.account

import app.k9mail.feature.account.setup.AccountSetupExternalContract

class AccountOwnerNameProvider : AccountSetupExternalContract.AccountOwnerNameProvider {
    override suspend fun getOwnerName(): String? {
        return null
    }
}
