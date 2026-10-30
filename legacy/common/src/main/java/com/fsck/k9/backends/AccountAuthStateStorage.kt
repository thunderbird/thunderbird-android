package com.fsck.k9.backends

import com.fsck.k9.mail.oauth.AuthStateStorage
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.feature.account.AccountId

class AccountAuthStateStorage(
    private val accountManager: LegacyAccountManager,
    private var accountId: AccountId,
) : AuthStateStorage {
    override fun getAuthorizationState(): String? {
        return getAccountById(accountId).oAuthState
    }

    override fun updateAuthorizationState(authorizationState: String?) {
        accountManager.updateSync(accountId) { account -> account.copy(oAuthState = authorizationState) }
            ?: error("Account not found: $accountId")
    }

    private fun getAccountById(accountId: AccountId): LegacyAccount {
        return accountManager.findById(accountId)
            ?: error("Account not found: $accountId")
    }
}
