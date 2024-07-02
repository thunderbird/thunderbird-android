package com.fsck.k9.backends

import com.fsck.k9.Account
import com.fsck.k9.mail.oauth.AuthStateStorage
import com.fsck.k9.preferences.AccountManager

class AccountAuthStateStorage(
    private val accountManager: AccountManager,
    private val account: Account,
) : AuthStateStorage {
    override fun getAuthorizationState(): String? {
        return account.oAuthState
    }

    override fun updateAuthorizationState(authorizationState: String?) {
        account.oAuthState = authorizationState
        accountManager.saveAccount(account)
    }
}
