package net.thunderbird.app.common.feature.account

import android.content.Context
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.settings.api.BackgroundAccountRemover

/**
 * Triggers asynchronous removal of an account.
 */
class DefaultBackgroundAccountRemover(
    private val context: Context,
    private val accountManager: LegacyAccountManager,
) : BackgroundAccountRemover {
    override fun removeAccountAsync(accountId: AccountId) {
        val account = accountManager.findById(accountId) ?: return
        if (account.isFinishedSetup) {
            accountManager.updateSync(account.copy(isFinishedSetup = false))
        }

        AccountRemoverWorker.enqueueRemoveAccountWorker(context, accountId)
    }
}
