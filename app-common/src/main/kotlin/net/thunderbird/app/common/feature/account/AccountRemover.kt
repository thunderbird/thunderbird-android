package net.thunderbird.app.common.feature.account

import com.fsck.k9.Core
import com.fsck.k9.LocalKeyStoreManager
import com.fsck.k9.Preferences
import com.fsck.k9.backend.BackendManager
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.mailstore.LocalStoreProvider
import com.fsck.k9.preferences.UnifiedInboxConfigurator
import kotlinx.coroutines.runBlocking
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.logging.Logger
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
    private val logger: Logger,
) {

    fun removeAccount(accountUuid: String) {
        val account = preferences.getAccount(accountUuid)
        if (account == null) {
            logger.warn { "Can't remove account with UUID $accountUuid because it doesn't exist." }
            return
        }

        val accountName = account.toString()
        logger.verbose { "Removing account '$accountName'…" }

        removeAvatar(account.uuid)
        removeLocalStore(account)
        messagingController.deleteAccount(account)
        removeBackend(account)

        preferences.deleteAccount(account)

        removeCertificates(account)
        Core.setServicesEnabled()
        unifiedInboxConfigurator.configureUnifiedInbox()

        logger.verbose { "Finished removing account '$accountName'." }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun removeAvatar(accountUuid: String) {
        runBlocking {
            try {
                avatarImageRepository.delete(AccountIdFactory.of(accountUuid))
            } catch (e: Exception) {
                logger.error(throwable = e) { "Failed to remove avatar for account $accountUuid" }
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun removeLocalStore(account: LegacyAccountDto) {
        try {
            val localStore = localStoreProvider.getInstance(account)
            localStore.delete()
        } catch (e: Exception) {
            logger.error(throwable = e) { "Error removing message database for account $account" }
        }

        localStoreProvider.removeInstance(account.uuid)
    }

    @Suppress("TooGenericExceptionCaught")
    private fun removeBackend(account: LegacyAccountDto) {
        try {
            backendManager.removeBackend(account.id)
        } catch (e: Exception) {
            logger.error(throwable = e) { "Failed to reset remote store for account $account" }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun removeCertificates(account: LegacyAccountDto) {
        try {
            localKeyStoreManager.deleteCertificates(account)
        } catch (e: Exception) {
            logger.error(throwable = e) { "Failed to remove certificates for account $account" }
        }
    }
}
