package net.thunderbird.app.common.feature.account

import app.k9mail.legacy.mailstore.MessageStoreManager
import com.fsck.k9.Core
import com.fsck.k9.LocalKeyStoreManager
import com.fsck.k9.backend.BackendManager
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.mailstore.LocalStoreProvider
import com.fsck.k9.preferences.UnifiedInboxConfigurator
import kotlinx.coroutines.runBlocking
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.account.AccountRepository
import net.thunderbird.feature.account.avatar.AvatarImageRepository

/**
 * Removes an account and all associated data.
 */
class AccountRemover(
    private val localStoreProvider: LocalStoreProvider,
    private val messagingController: MessagingController,
    private val backendManager: BackendManager,
    private val localKeyStoreManager: LocalKeyStoreManager,
    private val accountManager: LegacyAccountManager,
    private val accountRepository: AccountRepository,
    private val unifiedInboxConfigurator: UnifiedInboxConfigurator,
    private val avatarImageRepository: AvatarImageRepository,
    private val messageStoreManager: MessageStoreManager,
    private val logger: Logger,
) {

    fun removeAccount(accountId: AccountId) {
        val account = accountManager.findById(accountId)
        if (account == null) {
            logger.warn { "Can't remove account with UUID $accountId because it doesn't exist." }
            return
        }

        logger.verbose { "Removing account '$accountId'…" }

        removeAvatar(accountId)
        removeLocalStore(accountId)
        messageStoreManager.removeMessageStore(accountId)
        messagingController.deleteAccount(accountId)
        removeBackend(accountId)

        accountRepository.delete(accountId)

        removeCertificates(account)
        Core.setServicesEnabled()
        unifiedInboxConfigurator.configureUnifiedInbox()

        logger.verbose { "Finished removing account '$accountId'." }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun removeAvatar(accountId: AccountId) {
        runBlocking {
            try {
                avatarImageRepository.delete(accountId)
            } catch (e: Exception) {
                logger.error(throwable = e) { "Failed to remove avatar for account $accountId" }
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun removeLocalStore(accountId: AccountId) {
        try {
            val localStore = localStoreProvider.getInstance(accountId)
            localStore?.delete()
        } catch (e: Exception) {
            logger.error(throwable = e) { "Error removing message database for account $accountId" }
        }

        localStoreProvider.removeInstance(accountId)
    }

    @Suppress("TooGenericExceptionCaught")
    private fun removeBackend(accountId: AccountId) {
        try {
            backendManager.removeBackend(accountId)
        } catch (e: Exception) {
            logger.error(throwable = e) { "Failed to reset remote store for account $accountId" }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun removeCertificates(account: LegacyAccount) {
        try {
            localKeyStoreManager.deleteCertificates(account)
        } catch (e: Exception) {
            logger.error(throwable = e) { "Failed to remove certificates for account $account" }
        }
    }
}
