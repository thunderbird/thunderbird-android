package com.fsck.k9.account

import app.k9mail.feature.account.setup.AccountSetupExternalContract
import com.fsck.k9.Preferences
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AccountOwnerNameProvider(
    private val preferences: Preferences,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AccountSetupExternalContract.AccountOwnerNameProvider {
    override suspend fun getOwnerName(): String? {
        return withContext(coroutineDispatcher) {
            preferences.defaultAccount?.senderName
        }
    }
}
