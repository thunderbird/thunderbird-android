package com.fsck.k9.account

import app.k9mail.feature.account.setup.domain.ExternalContract
import com.fsck.k9.Preferences

class AccountOwnerNameProvider(
    private val preferences: Preferences,
) : ExternalContract.AccountOwnerNameProvider {
    override fun getOwnerName(): String? {
        return preferences.defaultAccount?.senderName
    }
}
