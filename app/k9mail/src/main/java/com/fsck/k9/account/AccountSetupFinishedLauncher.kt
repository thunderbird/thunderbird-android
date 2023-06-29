package com.fsck.k9.account

import android.content.Context
import android.content.Intent
import app.k9mail.feature.account.setup.AccountSetupExternalContract
import com.fsck.k9.activity.MessageList

class AccountSetupFinishedLauncher(
    private val context: Context,
) : AccountSetupExternalContract.AccountSetupFinishedLauncher {
    override suspend fun launch(accountUuid: String) {
        val intent = Intent(context, MessageList::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(MessageList.EXTRA_ACCOUNT, accountUuid)
        }

        context.startActivity(intent)
    }
}
