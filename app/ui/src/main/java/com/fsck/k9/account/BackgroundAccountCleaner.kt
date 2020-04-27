package com.fsck.k9.account

import android.content.Context

/**
 * Triggers asynchronous cleanup of an account.
 */
class BackgroundAccountCleaner(private val context: Context) {
    fun clearAccountAsync(accountUuid: String) {
        AccountCleanerService.enqueueClearAccountJob(context, accountUuid)
    }
}
