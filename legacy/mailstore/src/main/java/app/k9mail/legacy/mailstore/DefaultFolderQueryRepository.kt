package app.k9mail.legacy.mailstore

import app.k9mail.legacy.mailstore.folder.extension.getFolderType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.core.logging.Logger
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.feature.account.AccountId
import net.thunderbird.feature.mail.folder.api.Folder
import net.thunderbird.feature.mail.folder.api.FolderServerId
import net.thunderbird.feature.mail.folder.api.OutboxFolderManager
import net.thunderbird.feature.mail.folder.api.data.FolderError
import net.thunderbird.feature.mail.folder.api.data.repository.FolderQueryRepository

private const val LOG_ID = "[repository][folder-query]"

class DefaultFolderQueryRepository(
    private val logger: Logger,
    private val accountManager: LegacyAccountManager,
    private val messageStoreManager: MessageStoreManager,
    private val outboxFolderManager: OutboxFolderManager,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : FolderQueryRepository {
    private suspend fun getAccountById(accountId: AccountId): LegacyAccount? =
        accountManager.getById(accountId).firstOrNull()

    override suspend fun findById(
        accountId: AccountId,
        folderId: Long,
    ): Outcome<Folder?, FolderError> = withContext(ioDispatcher) {
        logger.verbose { "$LOG_ID getting folder by id '$folderId' from account '$accountId'" }
        val account = getAccountById(accountId)
            ?: return@withContext Outcome.failure(FolderError.AccountNotFound)
        val messageStore = createMessageStore(accountId)
            ?: return@withContext Outcome.failure(FolderError.AccountNotFound)
        val outboxFolderId = outboxFolderManager.getOutboxFolderId(accountId)
        logger.verbose { "$LOG_ID found outbox folder id '$outboxFolderId' for account '$accountId'" }
        val folder = messageStore.getFolder(folderId) { folder ->
            Folder(
                id = folder.id,
                name = folder.name,
                type = folder.getFolderType(account, outboxFolderId),
                isLocalOnly = folder.isLocalOnly,
            )
        }
        logger.verbose { "$LOG_ID found folder: $folder" }

        Outcome.success(folder)
    }

    override suspend fun findFolderServerIdById(
        accountId: AccountId,
        folderId: Long,
    ): Outcome<FolderServerId?, FolderError> = withContext(ioDispatcher) {
        logger.verbose { "$LOG_ID getting folder server_id by id '$folderId' from account '$accountId'" }
        val messageStore = createMessageStore(accountId)
            ?: return@withContext Outcome.failure(FolderError.AccountNotFound)
        val serverId = messageStore.getFolder(folderId) { folder ->
            folder.serverId?.let(::FolderServerId)
        }
        logger.verbose { "$LOG_ID found folder server_id: '${serverId?.serverId}'" }

        Outcome.success(serverId)
    }

    override suspend fun findIdByServerId(
        accountId: AccountId,
        folderServerId: FolderServerId,
    ): Outcome<Long?, FolderError> = withContext(ioDispatcher) {
        logger.verbose { "$LOG_ID getting folder id by server_id '$folderServerId' from account '$accountId'" }
        val messageStore = createMessageStore(accountId)
            ?: return@withContext Outcome.failure(FolderError.AccountNotFound)
        val folderId = messageStore.getFolderId(folderServerId.serverId)
        logger.verbose { "$LOG_ID found folder id: '${folderId}'" }

        Outcome.success(folderId)
    }

    override suspend fun isPresent(
        accountId: AccountId,
        folderId: Long,
    ): Boolean = withContext(ioDispatcher) {
        try {
            val messageStore = messageStoreManager.getMessageStore(accountId)
            messageStore.getFolder(folderId) { true } ?: false
        } catch (e: IllegalStateException) {
            logger.error(throwable = e) {
                "$LOG_ID failed to verify if folder is present. Account '$accountId' was not found."
            }
            false
        }
    }

    private fun createMessageStore(accountId: AccountId): MessageStore? = try {
        messageStoreManager.getMessageStore(accountId)
    } catch (e: IllegalStateException) {
        logger.error(throwable = e) {
            "$LOG_ID failed to create MessageStore for account '$accountId'. Account was not found."
        }
        null
    }
}
