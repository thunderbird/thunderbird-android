package com.fsck.k9.account

import android.content.Context
import app.k9mail.feature.account.setup.domain.ExternalContract
import com.fsck.k9.Preferences
import com.fsck.k9.activity.MessageList

class AccountSetupFinishedLauncher(
    private val context: Context,
    private val preferences: Preferences,
) : ExternalContract.AccountSetupFinishedLauncher {
    override fun launch(accountUuid: String) {
        val account = preferences.getAccount(accountUuid)
        if (account == null) {
            MessageList.launch(context)
        } else {
            MessageList.launch(context, account)
        }
    }
}
