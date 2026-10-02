package net.thunderbird.app.common.feature.account

import android.content.Context
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.settings.api.BackgroundAccountRemover

/**
 * Triggers asynchronous removal of an account.
 */
class DefaultBackgroundAccountRemover(private val context: Context) : BackgroundAccountRemover {
    override fun removeAccountAsync(accountId: AccountId) {
        // TODO: Add a mechanism to hide the account from the UI right away

        AccountRemoverWorker.enqueueRemoveAccountWorker(context, accountId)
    }
}
