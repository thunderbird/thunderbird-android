package com.fsck.k9.account

import app.k9mail.feature.account.edit.AccountEditExternalContract
import app.k9mail.feature.account.edit.AccountEditExternalContract.AccountUpdaterFailure
import app.k9mail.feature.account.edit.AccountEditExternalContract.AccountUpdaterResult
import com.fsck.k9.logging.Timber
import com.fsck.k9.mail.ServerSettings
import com.fsck.k9.preferences.AccountManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AccountServerSettingsUpdater(
    private val accountManager: AccountManager,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AccountEditExternalContract.AccountServerSettingsUpdater {

    @Suppress("TooGenericExceptionCaught")
    override suspend fun updateServerSettings(
        accountUuid: String,
        isIncoming: Boolean,
        serverSettings: ServerSettings,
    ): AccountUpdaterResult {
        return try {
            withContext(coroutineDispatcher) {
                updateSettings(accountUuid, isIncoming, serverSettings)
            }
        } catch (error: Exception) {
            Timber.e(error, "Error while updating account server settings with UUID %s", accountUuid)

            AccountUpdaterResult.Failure(AccountUpdaterFailure.UnknownError(error))
        }
    }

    private fun updateSettings(
        accountUuid: String,
        isIncoming: Boolean,
        serverSettings: ServerSettings,
    ): AccountUpdaterResult {
        val account = accountManager.getAccount(accountUuid = accountUuid) ?: return AccountUpdaterResult.Failure(
            AccountUpdaterFailure.AccountNotFound(accountUuid),
        )

        if (isIncoming) {
            account.incomingServerSettings = serverSettings
        } else {
            account.outgoingServerSettings = serverSettings
        }

        accountManager.saveAccount(account)

        return AccountUpdaterResult.Success(accountUuid)
    }
}
