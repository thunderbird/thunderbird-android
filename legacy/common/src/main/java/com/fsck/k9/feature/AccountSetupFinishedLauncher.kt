package com.fsck.k9.feature

import android.content.Context
import app.k9mail.feature.launcher.FeatureLauncherExternalContract
import com.fsck.k9.activity.MessageList

class AccountSetupFinishedLauncher(
    private val context: Context,
) : FeatureLauncherExternalContract.AccountSetupFinishedLauncher {
    override fun launch(accountUuid: String?) {
        if (accountUuid != null) {
            MessageList.launch(context, accountUuid)
        } else {
            MessageList.launch(context)
        }
    }
}
