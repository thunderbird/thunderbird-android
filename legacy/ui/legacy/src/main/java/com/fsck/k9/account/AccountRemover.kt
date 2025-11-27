package com.fsck.k9.account

import com.fsck.k9.Core
import com.fsck.k9.LocalKeyStoreManager
import com.fsck.k9.Preferences
import com.fsck.k9.backend.BackendManager
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.mailstore.LocalStoreProvider
import com.fsck.k9.preferences.UnifiedInboxConfigurator
import kotlinx.coroutines.runBlocking
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.logging.legacy.Log
import net.thunderbird.feature.account.AccountIdFactory
import net.thunderbird.feature.account.avatar.AvatarImageRepository

/**
 * Removes an account and all associated data.
 */
class AccountRemover(
    private val localStoreProvider: LocalStoreProvider,
    private val messagingController: MessagingController,
    private val backendManager: BackendManager,
    private val localKeyStoreManager: LocalKeyStoreManager,
    private val preferences: Preferences,
    private val unifiedInboxConfigurator: UnifiedInboxConfigurator,
    private val avatarImageRepository: AvatarImageRepository,
) {

    fun removeAccount(accountUuid: String) {
        val account = preferences.getAccount(accountUuid)
        if (account == null) {
            Log.w("Can't remove account with UUID %s because it doesn't exist.", accountUuid)
            return
        }

        val accountName = account.toString()
        Log.v("Removing account '%s'…", accountName)

        removeAvatar(account.uuid)
        removeLocalStore(account)
        messagingController.deleteAccount(account)
        removeBackend(account)

        preferences.deleteAccount(account)

        removeCertificates(account)
        Core.setServicesEnabled()
        unifiedInboxConfigurator.configureUnifiedInbox()

        Log.v("Finished removing account '%s'.", accountName)
    }

    private fun removeAvatar(accountUuid: String) {
        runBlocking {
            try {
                avatarImageRepository.delete(AccountIdFactory.of(accountUuid))
            } catch (e: Exception) {
                Log.e(e, "Failed to remove avatar for account %s", accountUuid)
            }
        }
    }

    private fun removeLocalStore(account: LegacyAccountDto) {
        try {
            val localStore = localStoreProvider.getInstance(account)
            localStore.delete()
        } catch (e: Exception) {
            Log.w(e, "Error removing message database for account '%s'", account)

            // Ignore, this may lead to localStores on sd-cards that are currently not inserted to be left
        }

        localStoreProvider.removeInstance(account.uuid)
    }

    private fun removeBackend(account: LegacyAccountDto) {
        try {
            backendManager.removeBackend(account)
        } catch (e: Exception) {
            Log.e(e, "Failed to reset remote store for account %s", account)
        }
    }

    private fun removeCertificates(account: LegacyAccountDto) {
        try {
            localKeyStoreManager.deleteCertificates(account)
        } catch (e: Exception) {
            Log.e(e, "Failed to remove certificates for account %s", account)
        }
    }
}
