package net.thunderbird.app.common.startup

import android.app.Activity
import app.k9mail.feature.launcher.FeatureLauncherActivity
import app.k9mail.feature.launcher.FeatureLauncherTarget
import com.fsck.k9.account.BackgroundAccountRemover
import com.fsck.k9.activity.MessageListActivity
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager

interface StartupRouter {
    fun routeToNextScreen(activity: Activity)
}

class DefaultStartupRouter(
    private val accountManager: LegacyAccountManager,
    private val accountRemover: BackgroundAccountRemover,
) : StartupRouter {
    override fun routeToNextScreen(activity: Activity) {
        val accounts = accountManager.getAccounts()
        deleteIncompleteAccounts(accounts)

        val hasAccountSetup = accounts.any { it.isFinishedSetup }
        if (!hasAccountSetup) {
            FeatureLauncherActivity.launch(activity, FeatureLauncherTarget.Onboarding)
        } else {
            MessageListActivity.launch(activity)
        }
    }

    private fun deleteIncompleteAccounts(accounts: List<LegacyAccount>) {
        accounts.filter { !it.isFinishedSetup }.forEach {
            accountRemover.removeAccountAsync(it.uuid)
        }
    }
}
