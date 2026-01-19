package net.thunderbird.app.common

import android.os.Bundle
import app.k9mail.feature.launcher.FeatureLauncherActivity
import app.k9mail.feature.launcher.FeatureLauncherTarget
import com.fsck.k9.account.BackgroundAccountRemover
import com.fsck.k9.activity.MessageListActivity
import com.fsck.k9.ui.base.BaseActivity
import kotlin.getValue
import net.thunderbird.core.android.common.startup.DatabaseUpgradeInterceptor
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import org.koin.android.ext.android.inject

class MainActivity : BaseActivity() {

    private val accountManager: LegacyAccountManager by inject()
    private val accountRemover: BackgroundAccountRemover by inject()
    private val databaseUpgradeInterceptor: DatabaseUpgradeInterceptor by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (databaseUpgradeInterceptor.checkAndHandleUpgrade(this, intent)) {
            finish()
            return
        }

        val accounts = accountManager.getAccounts()
        deleteIncompleteAccounts(accounts)
        val hasAccountSetup = accounts.any { it.isFinishedSetup }
        if (!hasAccountSetup) {
            FeatureLauncherActivity.launch(this, FeatureLauncherTarget.Onboarding)
        } else {
            MessageListActivity.launch(this)
        }
        finish()
    }

    // TODO remove this as the new account setup doesn't create incomplete accounts anymore
    private fun deleteIncompleteAccounts(accounts: List<LegacyAccount>) {
        accounts.filter { !it.isFinishedSetup }.forEach {
            accountRemover.removeAccountAsync(it.uuid)
        }
    }
}
