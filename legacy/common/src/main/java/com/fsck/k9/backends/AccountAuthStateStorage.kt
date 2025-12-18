package com.fsck.k9.backends

import com.fsck.k9.mail.oauth.AuthStateStorage
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.feature.account.AccountId

class AccountAuthStateStorage(
    private val accountManager: LegacyAccountManager,
    private val accountId: AccountId,
) : AuthStateStorage {
    override fun getAuthorizationState(): String? = runBlocking {
        getAccountById(accountId).oAuthState
    }

    override fun updateAuthorizationState(authorizationState: String?) = runBlocking {
        val account = getAccountById(accountId)

        accountManager.update(
            account.copy(
                oAuthState = authorizationState,
            ),
        )
    }

    private suspend fun getAccountById(accountId: AccountId): LegacyAccount {
        return accountManager.getById(accountId).firstOrNull()
            ?: error("Account not found: $accountId")
    }
}
