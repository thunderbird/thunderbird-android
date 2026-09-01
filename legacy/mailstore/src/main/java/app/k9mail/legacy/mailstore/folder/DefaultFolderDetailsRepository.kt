package app.k9mail.legacy.mailstore.folder

import app.k9mail.legacy.mailstore.MessageStore
import app.k9mail.legacy.mailstore.MessageStoreManager
import app.k9mail.legacy.mailstore.folder.extension.getFolderType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.core.logging.Logger
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.folder.api.Folder
import net.thunderbird.feature.mail.folder.api.FolderDetails
import net.thunderbird.feature.mail.folder.api.OutboxFolderManager
import net.thunderbird.feature.mail.folder.api.data.FolderError
import net.thunderbird.feature.mail.folder.api.data.repository.FolderDetailsRepository
import net.thunderbird.feature.mail.folder.api.data.repository.PartialUpdatableFolderDetails

private const val LOG_ID = "[repository][folder-details]"

class DefaultFolderDetailsRepository(
    private val logger: Logger,
    private val accountManager: LegacyAccountManager,
    private val outboxFolderManager: OutboxFolderManager,
    private val messageStoreManager: MessageStoreManager,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : FolderDetailsRepository {
    override suspend fun findById(accountId: AccountId, folderId: Long): Outcome<FolderDetails?, FolderError> =
        withContext(ioDispatcher) {
            logger.verbose {
                "$LOG_ID finding folder details for account '$accountId' and folder '$folderId'"
            }
            val account = accountManager.getById(accountId).firstOrNull()
                ?: return@withContext Outcome.failure(FolderError.AccountNotFound)

            val messageStore = messageStoreManager.getMessageStore(accountId)
            val outboxFolderId = outboxFolderManager.getOutboxFolderId(accountId)
            logger.verbose {
                "$LOG_ID found outbox folder with id '$outboxFolderId' and account id '$accountId'"
            }
            val folderDetails = messageStore.getFolder(folderId) { folder ->
                FolderDetails(
                    folder = Folder(
                        id = folder.id,
                        name = folder.name,
                        type = folder.getFolderType(account, outboxFolderId),
                        isLocalOnly = folder.isLocalOnly,
                    ),
                    isInTopGroup = folder.isInTopGroup,
                    isIntegrate = folder.isIntegrate,
                    isSyncEnabled = folder.isSyncEnabled,
                    isVisible = folder.isVisible,
                    isNotificationsEnabled = folder.isNotificationsEnabled,
                    isPushEnabled = folder.isPushEnabled,
                )
            }

            logger.verbose {
                "$LOG_ID folder details = $folderDetails"
            }

            Outcome.success(folderDetails)
        }

    override suspend fun update(accountId: AccountId, folderDetails: FolderDetails): Outcome<Unit, FolderError> =
        withContext(ioDispatcher) {
            logger.verbose {
                "$LOG_ID updating folder details with folder id '${
                    folderDetails.folder.id
                }' and account id '$accountId'"
            }
            try {
                val messageStore = messageStoreManager.getMessageStore(accountId)
                messageStore.updateFolderSettings(folderDetails)
                Outcome.success()
            } catch (e: IllegalStateException) {
                logger.error(throwable = e) {
                    "$LOG_ID Failed to update folder with id '${
                        folderDetails.folder.id
                    }' and account id '$accountId'"
                }
                Outcome.failure(FolderError.AccountNotFound)
            } catch (e: IllegalArgumentException) {
                val msg = "Executed a full 'update' without all the required parameters."
                logger.error(throwable = e) {
                    "$LOG_ID Failed to update folder with id '${
                        folderDetails.folder.id
                    }' and account id '$accountId'.\nMessage: $msg"
                }
                Outcome.failure(FolderError.FailedPrecondition(message = msg, throwable = e))
            }
        }

    override suspend fun update(
        accountId: AccountId,
        partialUpdate: PartialUpdatableFolderDetails,
    ): Outcome<Unit, FolderError> =
        withContext(ioDispatcher) {
            logger.verbose {
                "$LOG_ID partially updating folder details with folder id '${
                    partialUpdate.folderId
                }' and account id '$accountId'"
            }
            try {
                val messageStore = messageStoreManager.getMessageStore(accountId)
                partialUpdate(messageStore, partialUpdate)
                Outcome.success()
            } catch (e: IllegalStateException) {
                logger.error(throwable = e) {
                    "$LOG_ID Failed to update folder with id '${
                        partialUpdate.folderId
                    }' and account id '$accountId'"
                }
                Outcome.failure(FolderError.AccountNotFound)
            } catch (e: IllegalArgumentException) {
                val msg = "Executed a full 'update' without all the required parameters."
                logger.error(throwable = e) {
                    "$LOG_ID Failed to update folder with id '${
                        partialUpdate.folderId
                    }' and account id '$accountId'.\nMessage: $msg"
                }
                Outcome.failure(FolderError.FailedPrecondition(message = msg, throwable = e))
            }
        }

    private fun partialUpdate(messageStore: MessageStore, folderDetails: PartialUpdatableFolderDetails) {
        val folderId = folderDetails.folderId
        logger.verbose { "$LOG_ID executing partial update of folder '$folderId'" }
        messageStore.apply {
            val integrate = folderDetails.integrate
            val syncEnabled = folderDetails.syncEnabled
            val visible = folderDetails.visible
            val notificationsEnabled = folderDetails.notificationsEnabled
            val pushEnabled = folderDetails.isPushEnabled

            if (integrate != null) {
                logger.verbose {
                    "$LOG_ID updating 'integrate' to '$integrate' of folder '$folderId'"
                }
                setIncludeInUnifiedInbox(folderId = folderId, includeInUnifiedInbox = integrate)
            }
            if (syncEnabled != null) {
                logger.verbose {
                    "$LOG_ID updating 'sync_enabled' to '$syncEnabled' of folder '$folderId'"
                }
                setSyncEnabled(folderId = folderId, enable = syncEnabled)
            }
            if (visible != null) {
                logger.verbose {
                    "$LOG_ID updating 'visible' to '$visible' of folder '$folderId'"
                }
                setVisible(folderId = folderId, visible = visible)
            }
            if (notificationsEnabled != null) {
                logger.verbose {
                    "$LOG_ID updating 'notification_enabled' to '$notificationsEnabled' of " +
                        "folder '$folderId'"
                }
                setNotificationsEnabled(folderId = folderId, enable = notificationsEnabled)
            }
            if (pushEnabled != null) {
                logger.verbose {
                    "$LOG_ID updating 'push_enabled' to '$pushEnabled' of folder '$folderId'"
                }
                setPushEnabled(folderId = folderId, enable = pushEnabled)
            }
        }
    }
}
