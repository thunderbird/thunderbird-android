package app.k9mail.feature.account.setup.ui.options.display

import app.k9mail.feature.account.setup.AccountSetupExternalContract

class FakeAccountOwnerNameProvider : AccountSetupExternalContract.AccountOwnerNameProvider {
    var ownerName: String? = null

    override suspend fun getOwnerName(): String? {
        return ownerName
    }
}
