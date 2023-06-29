package com.fsck.k9.feature

import android.content.Context
import android.content.Intent
import app.k9mail.feature.launcher.FeatureLauncherExternalContract
import com.fsck.k9.activity.MessageList

class AccountSetupFinishedLauncher(
    private val context: Context,
) : FeatureLauncherExternalContract.AccountSetupFinishedLauncher {
    override fun launch(accountUuid: String) {
        val intent = Intent(context, MessageList::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(MessageList.EXTRA_ACCOUNT, accountUuid)
        }

        context.startActivity(intent)
    }
}
