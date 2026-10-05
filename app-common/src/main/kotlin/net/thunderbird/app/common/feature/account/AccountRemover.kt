package net.thunderbird.app.common.feature.account

import com.fsck.k9.Core
import com.fsck.k9.LocalKeyStoreManager
import com.fsck.k9.Preferences
import com.fsck.k9.backend.BackendManager
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.mailstore.LocalStoreProvider
import com.fsck.k9.preferences.UnifiedInboxConfigurator
import kotlinx.coroutines.runBlocking
import net.thunderbird.components.core.logging.Logger
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.avatar.AvatarImageRepository

private const val TAG = "AccountRemover"

/**
 * Removes an account and all associated data.
 */
class AccountRemover(
    private val localStoreProvider: LocalStoreProvider,
    private val messagingController: MessagingController,
    private val backendManager: BackendManager,
    private val localKeyStoreManager: LocalKeyStoreManager,
    private val accountManager: LegacyAccountManager,
    private val preferences: Preferences,
    private val unifiedInboxConfigurator: UnifiedInboxConfigurator,
    private val avatarImageRepository: AvatarImageRepository,
    private val logger: Logger,
) {

    fun removeAccount(accountId: AccountId) {
        val account = accountManager.getById(accountId)
        val legacyAccount = preferences.getById(accountId)
        if (account == null || legacyAccount == null) {
            logger.warn(TAG) { "Can't remove account with UUID $accountId because it doesn't exist." }
            return
        }

        logger.verbose(TAG) { "Removing account '$accountId'…" }

        removeAvatar(accountId)
        removeLocalStore(account)
        messagingController.deleteAccount(legacyAccount)
        removeBackend(accountId)

        preferences.deleteAccount(legacyAccount)

        removeCertificates(account)
        Core.setServicesEnabled()
        unifiedInboxConfigurator.configureUnifiedInbox()

        logger.verbose(TAG) { "Finished removing account '$accountId'." }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun removeAvatar(accountId: AccountId) {
        runBlocking {
            try {
                avatarImageRepository.delete(accountId)
            } catch (e: Exception) {
                logger.error(tag = TAG, throwable = e) { "Failed to remove avatar for account $accountId" }
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun removeLocalStore(account: LegacyAccount) {
        try {
            val localStore = localStoreProvider.getInstanceByLegacyAccount(account)
            localStore.delete()
        } catch (e: Exception) {
            logger.error(tag = TAG, throwable = e) { "Error removing message database for account $account" }
        }

        localStoreProvider.removeInstance(account.id)
    }

    @Suppress("TooGenericExceptionCaught")
    private fun removeBackend(accountId: AccountId) {
        try {
            backendManager.removeBackend(accountId)
        } catch (e: Exception) {
            logger.error(tag = TAG, throwable = e) { "Failed to reset remote store for account $accountId" }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun removeCertificates(account: LegacyAccount) {
        try {
            localKeyStoreManager.deleteCertificates(account)
        } catch (e: Exception) {
            logger.error(tag = TAG, throwable = e) { "Failed to remove certificates for account $account" }
        }
    }
}
