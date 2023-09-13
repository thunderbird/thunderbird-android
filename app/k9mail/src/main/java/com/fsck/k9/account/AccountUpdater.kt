package com.fsck.k9.account

import app.k9mail.feature.account.common.domain.entity.Account
import app.k9mail.feature.account.edit.AccountEditExternalContract
import app.k9mail.feature.account.edit.AccountEditExternalContract.AccountUpdater.AccountUpdaterResult
import com.fsck.k9.Preferences
import com.fsck.k9.logging.Timber
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AccountUpdater(
    private val preferences: Preferences,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AccountEditExternalContract.AccountUpdater {

    @Suppress("TooGenericExceptionCaught")
    override suspend fun updateAccount(account: Account): AccountUpdaterResult {
        return try {
            withContext(coroutineDispatcher) {
                AccountUpdaterResult.Success(update(account))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error while updating account")

            AccountUpdaterResult.Error(e.message ?: "Unknown update account error")
        }
    }

    private fun update(account: Account): String {
        val uuid = account.uuid
        require(uuid.isNotEmpty()) { "Can't update account without uuid" }

        val existingAccount = preferences.getAccount(uuid)
        require(existingAccount != null) { "Can't update non-existing account" }

        existingAccount.incomingServerSettings = account.incomingServerSettings
        existingAccount.outgoingServerSettings = account.outgoingServerSettings

        preferences.saveAccount(existingAccount)

        return uuid
    }
}
