package com.fsck.k9.account

import app.k9mail.feature.account.common.AccountCommonExternalContract
import app.k9mail.feature.account.common.domain.entity.AccountState
import app.k9mail.feature.account.common.domain.entity.AuthorizationState
import com.fsck.k9.backends.toImapServerSettings
import com.fsck.k9.mail.ServerSettings
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.android.account.LegacyAccountDtoManager
import net.thunderbird.core.common.mail.Protocols
import net.thunderbird.feature.account.AccountId
import net.thunderbird.legacy.logging.Log

private const val TAG = "AccountStateLoader"

class AccountStateLoader(
    private val accountManager: LegacyAccountDtoManager,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AccountCommonExternalContract.AccountStateLoader {

    @Suppress("TooGenericExceptionCaught")
    override suspend fun loadAccountState(accountId: AccountId): AccountState? {
        return try {
            withContext(coroutineDispatcher) {
                load(accountId)
            }
        } catch (e: Exception) {
            Log.e(TAG, e, "Error while loading account")

            null
        }
    }

    private fun load(accountId: AccountId): AccountState? {
        return accountManager.getById(accountId)?.let { mapToAccountState(it) }
    }

    private fun mapToAccountState(account: LegacyAccountDto): AccountState {
        return AccountState(
            id = account.id,
            emailAddress = account.email,
            incomingServerSettings = account.incomingServerSettingsExtra,
            outgoingServerSettings = account.outgoingServerSettings,
            authorizationState = AuthorizationState(account.oAuthState),
        )
    }
}

private val LegacyAccountDto.incomingServerSettingsExtra: ServerSettings
    get() = when (incomingServerSettings.type) {
        Protocols.IMAP -> toImapServerSettings()
        else -> incomingServerSettings
    }
