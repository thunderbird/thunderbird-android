package com.fsck.k9.backends

import com.fsck.k9.mail.oauth.AuthStateStorage
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.core.common.mail.Protocols
import net.thunderbird.feature.account.AccountId

interface OAuthClientSecretProvider {
    fun getClientSecret(): String?
}

class AccountAuthStateStorage(
    private val accountManager: LegacyAccountManager,
    private var accountId: AccountId,
) : AuthStateStorage, OAuthClientSecretProvider {
    override fun getAuthorizationState(): String? {
        return getAccountById(accountId).oAuthState
    }

    override fun updateAuthorizationState(authorizationState: String?) {
        val account = getAccountById(accountId)

        val updatedAccount = account.copy(
            oAuthState = authorizationState,
        )
        accountManager.updateSync(updatedAccount)
    }

    override fun getClientSecret(): String? {
        val incomingSettings = getAccountById(accountId).incomingServerSettings
        if (incomingSettings.type != Protocols.ZOHO_MAIL_API) return null
        return incomingSettings.password?.takeIf(String::isNotBlank)
    }

    private fun getAccountById(accountId: AccountId): LegacyAccount {
        return accountManager.getById(accountId)
            ?: error("Account not found: $accountId")
    }
}
