package com.fsck.k9.backends

import app.k9mail.legacy.account.AccountManager
import app.k9mail.legacy.account.LegacyAccount
import com.fsck.k9.mail.oauth.AuthStateStorage

class AccountAuthStateStorage(
    private val accountManager: AccountManager,
    private val account: LegacyAccount,
) : AuthStateStorage {
    override fun getAuthorizationState(): String? {
        return account.oAuthState
    }

    override fun updateAuthorizationState(authorizationState: String?) {
        account.oAuthState = authorizationState
        accountManager.saveAccount(account)
    }
}
