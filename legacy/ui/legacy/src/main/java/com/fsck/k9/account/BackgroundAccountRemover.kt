package com.fsck.k9.account

import android.content.Context

/**
 * Triggers asynchronous removal of an account.
 */
class BackgroundAccountRemover(private val context: Context) {
    fun removeAccountAsync(accountUuid: String) {
        // TODO: Add a mechanism to hide the account from the UI right away

        AccountRemoverWorker.enqueueRemoveAccountWorker(context, accountUuid)
    }
}
