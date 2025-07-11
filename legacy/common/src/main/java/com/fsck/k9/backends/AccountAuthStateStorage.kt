package com.fsck.k9.backends

import com.fsck.k9.mail.oauth.AuthStateStorage
import net.thunderbird.core.android.account.AccountManager
import net.thunderbird.core.android.account.LegacyAccount

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
