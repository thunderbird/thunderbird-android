package com.fsck.k9.account

import com.fsck.k9.Core
import com.fsck.k9.LocalKeyStoreManager
import com.fsck.k9.Preferences
import com.fsck.k9.backend.BackendManager
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.mailstore.LocalStoreProvider
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.logging.legacy.Log

/**
 * Removes an account and all associated data.
 */
class AccountRemover(
    private val localStoreProvider: LocalStoreProvider,
    private val messagingController: MessagingController,
    private val backendManager: BackendManager,
    private val localKeyStoreManager: LocalKeyStoreManager,
    private val preferences: Preferences,
) {

    fun removeAccount(accountUuid: String) {
        val account = preferences.getAccount(accountUuid)
        if (account == null) {
            Log.w("Can't remove account with UUID %s because it doesn't exist.", accountUuid)
            return
        }

        val accountName = account.toString()
        Log.v("Removing account '%s'…", accountName)

        removeLocalStore(account)
        messagingController.deleteAccount(account)
        removeBackend(account)

        preferences.deleteAccount(account)

        removeCertificates(account)
        Core.setServicesEnabled()

        Log.v("Finished removing account '%s'.", accountName)
    }

    private fun removeLocalStore(account: LegacyAccount) {
        try {
            val localStore = localStoreProvider.getInstance(account)
            localStore.delete()
        } catch (e: Exception) {
            Log.w(e, "Error removing message database for account '%s'", account)

            // Ignore, this may lead to localStores on sd-cards that are currently not inserted to be left
        }

        localStoreProvider.removeInstance(account)
    }

    private fun removeBackend(account: LegacyAccount) {
        try {
            backendManager.removeBackend(account)
        } catch (e: Exception) {
            Log.e(e, "Failed to reset remote store for account %s", account)
        }
    }

    private fun removeCertificates(account: LegacyAccount) {
        try {
            localKeyStoreManager.deleteCertificates(account)
        } catch (e: Exception) {
            Log.e(e, "Failed to remove certificates for account %s", account)
        }
    }
}
