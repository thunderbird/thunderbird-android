package com.fsck.k9.account

import app.k9mail.feature.account.common.AccountCommonExternalContract
import app.k9mail.feature.account.common.domain.entity.AccountState
import app.k9mail.feature.account.common.domain.entity.AuthorizationState
import com.fsck.k9.backends.toImapServerSettings
import com.fsck.k9.mail.ServerSettings
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.core.common.mail.Protocols
import net.thunderbird.feature.account.AccountId
import net.thunderbird.legacy.logging.Log

class AccountStateLoader(
    private val accountManager: LegacyAccountManager,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AccountCommonExternalContract.AccountStateLoader {

    @Suppress("TooGenericExceptionCaught")
    override suspend fun loadAccountState(accountId: AccountId): AccountState? {
        return try {
            withContext(coroutineDispatcher) {
                load(accountId)
            }
        } catch (e: Exception) {
            Log.e(e, "Error while loading account")

            null
        }
    }

    private fun load(accountId: AccountId): AccountState? {
        return accountManager.findById(accountId)?.let { mapToAccountState(it) }
    }

    private fun mapToAccountState(account: LegacyAccount): AccountState {
        return AccountState(
            id = account.id,
            emailAddress = account.email,
            incomingServerSettings = account.incomingServerSettingsExtra,
            outgoingServerSettings = account.outgoingServerSettings,
            authorizationState = AuthorizationState(account.oAuthState),
        )
    }
}

private val LegacyAccount.incomingServerSettingsExtra: ServerSettings
    get() = when (incomingServerSettings.type) {
        Protocols.IMAP -> toImapServerSettings()
        else -> incomingServerSettings
    }
