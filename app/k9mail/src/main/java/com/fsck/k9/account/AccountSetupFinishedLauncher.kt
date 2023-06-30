package com.fsck.k9.account

import android.content.Context
import app.k9mail.feature.account.setup.AccountSetupExternalContract
import com.fsck.k9.Preferences
import com.fsck.k9.activity.MessageList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AccountSetupFinishedLauncher(
    private val context: Context,
    private val preferences: Preferences,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AccountSetupExternalContract.AccountSetupFinishedLauncher {
    override suspend fun launch(accountUuid: String) {
        val account = withContext(coroutineDispatcher) {
            preferences.getAccount(accountUuid)
        }

        if (account == null) {
            MessageList.launch(context)
        } else {
            MessageList.launch(context, account)
        }
    }
}
