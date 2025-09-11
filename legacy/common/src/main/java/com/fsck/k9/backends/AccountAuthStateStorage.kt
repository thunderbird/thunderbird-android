package com.fsck.k9.backends

import com.fsck.k9.mail.oauth.AuthStateStorage
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.android.account.LegacyAccountDtoManager

class AccountAuthStateStorage(
    private val accountManager: LegacyAccountDtoManager,
    private val account: LegacyAccountDto,
) : AuthStateStorage {
    override fun getAuthorizationState(): String? {
        return account.oAuthState
    }

    override fun updateAuthorizationState(authorizationState: String?) {
        account.oAuthState = authorizationState
        accountManager.saveAccount(account)
    }
}
