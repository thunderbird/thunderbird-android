package com.fsck.k9.account

import com.fsck.k9.Core
import com.fsck.k9.Preferences
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.mailstore.LocalStoreProvider
import timber.log.Timber

/**
 * Removes an account and all associated data.
 */
class AccountRemover(
    private val localStoreProvider: LocalStoreProvider,
    private val messagingController: MessagingController,
    private val preferences: Preferences
) {

    fun removeAccount(accountUuid: String) {
        val account = preferences.getAccount(accountUuid)
        if (account == null) {
            Timber.w("Can't remove account with UUID %s because it doesn't exist.", accountUuid)
            return
        }

        val accountName = account.description
        Timber.v("Removing account '%s'…", accountName)

        try {
            val localStore = localStoreProvider.getInstance(account)
            localStore.delete()
        } catch (e: Exception) {
            Timber.w(e, "Error removing message database for account '%s'", accountName)

            // Ignore, this may lead to localStores on sd-cards that are currently not inserted to be left
        }

        messagingController.deleteAccount(account)
        preferences.deleteAccount(account)
        Core.setServicesEnabled()

        Timber.v("Finished removing account '%s'.", accountName)
    }
}
