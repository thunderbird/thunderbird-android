package com.fsck.k9.account

import com.fsck.k9.Core
import com.fsck.k9.Preferences
import com.fsck.k9.mailstore.LocalStoreProvider
import com.fsck.k9.controller.MessagingController
import timber.log.Timber

/**
 * Cleans an account and all associated data.
 */
class AccountCleaner(
    private val messagingController: MessagingController,
    private val preferences: Preferences
) {

    fun clearAccount(accountUuid: String) {
        val account = preferences.getAccount(accountUuid)
        if (account == null) {
            Timber.w("Can't clean account with UUID %s because it doesn't exist.", accountUuid)
            return
        }

        val accountName = account.description
        Timber.v("Cleaning account '%s'…", accountName)

        try {
            messagingController.clear(account, null)
        } catch (e: Exception) {
            Timber.w(e, "Error cleaning message database for account '%s'", accountName)

            // Ignore, this may lead to localStores on sd-cards that are currently not inserted to be left
        }

        Core.setServicesEnabled()

        Timber.v("Finished cleaning account '%s'.", accountName)
    }
}
