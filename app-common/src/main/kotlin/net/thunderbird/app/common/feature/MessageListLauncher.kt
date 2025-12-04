package net.thunderbird.app.common.feature

import android.content.Context
import app.k9mail.feature.launcher.FeatureLauncherExternalContract
import com.fsck.k9.activity.MainActivity

internal class MessageListLauncher(
    private val context: Context,
) : FeatureLauncherExternalContract.MessageListLauncher {
    override fun launch(accountUuid: String?) {
        if (accountUuid != null) {
            MainActivity.launch(context, accountUuid)
        } else {
            MainActivity.launch(context)
        }
    }
}
