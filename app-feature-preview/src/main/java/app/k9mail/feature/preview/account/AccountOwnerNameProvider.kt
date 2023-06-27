package app.k9mail.feature.preview.account

import app.k9mail.feature.account.setup.domain.ExternalContract

class AccountOwnerNameProvider: ExternalContract.AccountOwnerNameProvider {
    override fun getOwnerName(): String? {
        return null
    }
}
